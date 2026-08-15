package fr.mandarine.todolist.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
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
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListViewModel
import fr.mandarine.todolist.presentation.TutorialUiState
import fr.mandarine.todolist.presentation.TutorialViewModel
import kotlinx.coroutines.launch

class TodoListActivity : AppCompatActivity() {

    internal lateinit var viewModel: TodoListViewModel
    private lateinit var adapter: TodoListAdapter
    private lateinit var watermark: ImageView
    internal lateinit var recyclerViewInternal: RecyclerView
    internal lateinit var itemTouchHelperInternal: ItemTouchHelper

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

        watermark = findViewById(R.id.imageWatermark)

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
        recyclerViewInternal.addItemDecoration(InsetItemDivider())

        itemTouchHelperInternal = ItemTouchHelper(buildDragCallback())
        itemTouchHelperInternal.attachToRecyclerView(recyclerViewInternal)

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
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                val toIndex = viewHolder.bindingAdapterPosition
                if (dragFromIndex != RecyclerView.NO_POSITION &&
                    toIndex != RecyclerView.NO_POSITION &&
                    dragFromIndex != toIndex
                ) {
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

    private inner class InsetItemDivider : RecyclerView.ItemDecoration() {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val dividerHeightPx: Int
        private val insetStartPx: Int
        private val insetEndPx: Int

        init {
            val density = resources.displayMetrics.density
            dividerHeightPx = (1f * density).toInt().coerceAtLeast(1)
            insetStartPx = (52f * density).toInt()
            insetEndPx = (16f * density).toInt()
            val typedArray = obtainStyledAttributes(
                intArrayOf(com.google.android.material.R.attr.colorOutlineVariant)
            )
            paint.color = typedArray.getColor(0, android.graphics.Color.LTGRAY)
            typedArray.recycle()
        }

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (needsDividerBelow(position)) {
                outRect.bottom = dividerHeightPx
            }
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)
                if (needsDividerBelow(position)) {
                    val top = child.bottom.toFloat()
                    val bottom = top + dividerHeightPx
                    c.drawRect(
                        insetStartPx.toFloat(),
                        top,
                        (parent.width - parent.paddingEnd - insetEndPx).toFloat(),
                        bottom,
                        paint
                    )
                }
            }
        }

        private fun needsDividerBelow(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (adapter.getItemViewType(position) != TodoListAdapter.VIEW_TYPE_ITEM) return false
            val nextPosition = position + 1
            if (nextPosition >= adapter.itemCount) return false
            return adapter.getItemViewType(nextPosition) == TodoListAdapter.VIEW_TYPE_ITEM
        }
    }

    private fun renderState(state: TodoListState) {
        when (state) {
            is TodoListState.NotFound -> finish()
            is TodoListState.Empty -> {
                adapter.submitList(emptyList(), emptyList())
                watermark.alpha = 0.15f
            }
            is TodoListState.Content -> {
                adapter.submitList(state.activeItems, state.completedItems)
                watermark.alpha = 0.08f
            }
        }
    }
}
