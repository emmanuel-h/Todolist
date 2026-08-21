package fr.mandarine.todolist.ui

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import fr.mandarine.todolist.R
import fr.mandarine.todolist.TodoListApplication
import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ReorderTodosUseCase
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import fr.mandarine.todolist.domain.TutorialAction
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.domain.TutorialScreen
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListViewModel
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialStage
import fr.mandarine.todolist.presentation.TutorialUiState
import fr.mandarine.todolist.presentation.TutorialViewModel
import kotlinx.coroutines.launch

class TodoListActivity : AppCompatActivity(), TutorialStage {

    internal lateinit var viewModel: TodoListViewModel
    private lateinit var adapter: TodoListAdapter
    internal lateinit var recyclerViewInternal: RecyclerView
    internal lateinit var itemTouchHelperInternal: ItemTouchHelper
    private lateinit var itemAnimator: TodoItemAnimator

    private lateinit var tutorialViewModel: TutorialViewModel
    private lateinit var tutorialController: TutorialOverlayController
    internal val tutorialBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            tutorialController.onSkipRequested()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_list)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val listId = requireNotNull(intent.getStringExtra("LIST_ID")) {
            "TodoListActivity requires LIST_ID intent extra"
        }
        val listName = intent.getStringExtra("LIST_NAME") ?: getString(R.string.app_name)
        supportActionBar?.title = listName

        val container = (application as TodoListApplication).container
        val todoRepository = container.todoRepository
        viewModel = ViewModelProvider(
            this,
            viewModelFactory {
                TodoListViewModel(
                    AddTodoUseCase(todoRepository),
                    GetTodosUseCase(todoRepository),
                    ToggleTodoUseCase(todoRepository),
                    DeleteTodoUseCase(todoRepository),
                    EditTodoUseCase(todoRepository),
                    ReorderTodosUseCase(todoRepository),
                    GetTodoListsUseCase(container.todoListRepository),
                    listId = listId,
                    dispatcher = container.databaseDispatcher
                )
            }
        )[TodoListViewModel::class.java]

        tutorialViewModel = container.tutorialViewModel


        adapter = TodoListAdapter(
            onToggle = { todoId -> viewModel.toggleTodo(todoId) },
            onDelete = { todoId -> viewModel.deleteTodo(todoId) },
            onEdit = { todoId, newTitle -> viewModel.editTodo(todoId, newTitle) },
            onStartDrag = { holder -> itemTouchHelperInternal.startDrag(holder) }
        )
        adapter.onSubmitInlineAdd = { title ->
            viewModel.submitInlineInput(title)
        }

        recyclerViewInternal = findViewById(R.id.recyclerView)
        recyclerViewInternal.layoutManager = LinearLayoutManager(this)
        recyclerViewInternal.adapter = adapter

        itemAnimator = TodoItemAnimator(shouldAnimate = {
            !tutorialViewModel.animationsSuppressed && !isReducedMotion()
        })
        recyclerViewInternal.itemAnimator = itemAnimator

        itemTouchHelperInternal = ItemTouchHelper(buildDragCallback())
        itemTouchHelperInternal.attachToRecyclerView(recyclerViewInternal)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.animationEvents.collect { event ->
                    if (!tutorialViewModel.animationsSuppressed && !isReducedMotion()) {
                        itemAnimator.pendingEvent = event
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { renderState(it) }
            }
        }

        onBackPressedDispatcher.addCallback(this, tutorialBackCallback)

        tutorialController = TutorialOverlayController(tutorialViewModel, lifecycleScope)
        tutorialController.attachToActivity(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tutorialViewModel.uiState.collect { state ->
                    tutorialBackCallback.isEnabled = state is TutorialUiState.Active
                    tutorialController.handleState(state, this@TodoListActivity)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        tutorialController.detachFromActivity()
    }

    private fun buildDragCallback(): ItemTouchHelper.Callback =
        object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            private var dragFromIndex: Int = RecyclerView.NO_POSITION

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (viewHolder.itemViewType != TodoListAdapter.VIEW_TYPE_ITEM) return 0
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return 0
                val isActive = position < adapter.activeItemCount()
                return if (isActive) {
                    makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                } else {
                    0
                }
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                val activeCount = adapter.activeItemCount()
                if (toPos < 0 || toPos >= activeCount) return false
                if (target.itemViewType != TodoListAdapter.VIEW_TYPE_ITEM) return false
                adapter.moveItem(fromPos, toPos)
                if (dragFromIndex == RecyclerView.NO_POSITION) {
                    dragFromIndex = fromPos
                }
                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    dragFromIndex = viewHolder?.bindingAdapterPosition ?: RecyclerView.NO_POSITION
                    viewHolder?.itemView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                val toIndex = viewHolder.bindingAdapterPosition
                if (dragFromIndex != RecyclerView.NO_POSITION &&
                    toIndex != RecyclerView.NO_POSITION &&
                    dragFromIndex != toIndex
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else {
                        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    viewModel.reorderTodos(dragFromIndex, toIndex)
                }
                dragFromIndex = RecyclerView.NO_POSITION
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun isLongPressDragEnabled(): Boolean = false
        }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val focused = currentFocus ?: window.decorView.findFocus()
            if (focused is TextInputEditText && isTouchOutsideView(ev, focused)) {
                val result = super.dispatchTouchEvent(ev)
                focused.isFocusable = false
                focused.clearFocus()
                focused.isFocusableInTouchMode = true
                focused.isFocusable = true
                getSystemService(InputMethodManager::class.java)
                    .hideSoftInputFromWindow(focused.windowToken, 0)
                return result
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isTouchOutsideView(ev: MotionEvent, view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0].toFloat()
        val top = location[1].toFloat()
        val right = left + view.width
        val bottom = top + view.height
        return ev.rawX < left || ev.rawX > right || ev.rawY < top || ev.rawY > bottom
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun isReducedMotion(): Boolean {
        val scale = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        return scale == 0f
    }

    // ── TutorialStage ──

    override val screen: TutorialScreen = TutorialScreen.ITEMS

    override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? = when (anchor) {
        TutorialAnchor.ItemGhostRow -> visibleGhostRow()?.tutorialBounds()
        TutorialAnchor.SubmitItemButton -> inlineAddView(R.id.btnInlineSubmit)?.tutorialBounds()
        is TutorialAnchor.ActiveItemToggle ->
            toggleButtonAt(activePosition(anchor.index))?.tutorialBounds()
        is TutorialAnchor.CompletedItemToggle ->
            toggleButtonAt(completedPosition(anchor.index))?.tutorialBounds()
        is TutorialAnchor.ActiveItemDragHandle ->
            itemHolderAt(activePosition(anchor.index))?.dragHandle?.tutorialBounds()
        is TutorialAnchor.ActiveItemRow ->
            itemHolderAt(activePosition(anchor.index))?.itemView?.tutorialBounds()
        else -> null
    }

    override suspend fun perform(action: TutorialAction): Boolean = when (action) {
        TutorialAction.OpenItemAddRow -> visibleGhostRow()?.performClick() ?: false
        is TutorialAction.TypeItemTitle -> {
            val field = inlineAddHolder()?.editText
            field?.typeTutorialText(action.text)
            field != null
        }
        TutorialAction.SubmitItem -> inlineAddView(R.id.btnInlineSubmit)?.performClick() ?: false
        is TutorialAction.ToggleActiveItem ->
            toggleButtonAt(activePosition(action.index))?.performClick() ?: false
        is TutorialAction.ToggleCompletedItem ->
            toggleButtonAt(completedPosition(action.index))?.performClick() ?: false
        is TutorialAction.MoveActiveItem -> {
            adapter.moveItem(activePosition(action.from), activePosition(action.to))
            true
        }
        is TutorialAction.CommitReorder -> {
            viewModel.reorderTodos(action.from, action.to)
            true
        }
        TutorialAction.NavigateBack -> {
            tutorialBackCallback.isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            true
        }
        else -> false
    }

    override suspend fun awaitDemoListId(): String? = null

    override fun bannerContent(): TutorialBannerContent? = null

    private fun activePosition(index: Int): Int = index

    /** Completed rows sit after the active rows, the inline-add row and the divider. */
    private fun completedPosition(index: Int): Int = adapter.activeItemCount() + 2 + index

    private fun itemHolderAt(position: Int): TodoListAdapter.ItemViewHolder? =
        recyclerViewInternal.findViewHolderForAdapterPosition(position)
            as? TodoListAdapter.ItemViewHolder

    private fun toggleButtonAt(position: Int): View? =
        itemHolderAt(position)?.itemView?.findViewById(R.id.btnToggleComplete)

    private fun inlineAddHolder(): TodoListAdapter.InlineAddViewHolder? {
        val position = (0 until adapter.itemCount).firstOrNull { position ->
            adapter.getItemViewType(position) == TodoListAdapter.VIEW_TYPE_INLINE_ADD
        } ?: return null
        return recyclerViewInternal.findViewHolderForAdapterPosition(position)
            as? TodoListAdapter.InlineAddViewHolder
    }

    private fun inlineAddView(viewId: Int): View? =
        inlineAddHolder()?.itemView?.findViewById(viewId)

    private fun visibleGhostRow(): View? =
        inlineAddView(R.id.ghostRow)?.takeIf { it.visibility == View.VISIBLE }

    private fun renderState(state: TodoListState) {
        when (state) {
            is TodoListState.NotFound -> finish()
            is TodoListState.Empty -> {
                adapter.submitList(emptyList(), emptyList())
            }
            is TodoListState.Content -> {
                adapter.submitList(state.activeItems, state.completedItems)
            }
        }
    }
}
