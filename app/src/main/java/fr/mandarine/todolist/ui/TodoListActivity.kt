package fr.mandarine.todolist.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.RoomTodoRepository
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.DeleteTodoUseCase
import fr.mandarine.todolist.domain.EditTodoUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ReorderTodosUseCase
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListViewModel

class TodoListActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoListViewModel
    private lateinit var adapter: TodoListAdapter
    private lateinit var emptyLayout: View
    private lateinit var watermark: ImageView
    internal lateinit var recyclerViewInternal: RecyclerView
    internal lateinit var itemTouchHelperInternal: ItemTouchHelper
    internal lateinit var inlineAddEditTextInternal: TextInputEditText

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

        val db = TodoDatabase.getInstance(this)
        val todoRepository = RoomTodoRepository(db.todoItemDao())
        viewModel = TodoListViewModel(
            AddTodoUseCase(todoRepository),
            GetTodosUseCase(todoRepository),
            ToggleTodoUseCase(todoRepository),
            DeleteTodoUseCase(todoRepository),
            EditTodoUseCase(todoRepository),
            ReorderTodosUseCase(todoRepository),
            listId = listId
        )

        emptyLayout = findViewById(R.id.layoutEmptyTodos)
        watermark = findViewById(R.id.imageWatermark)

        wireInlineAddRow()

        adapter = TodoListAdapter(
            onToggle = { todoId ->
                viewModel.toggleTodo(todoId)
                renderState(viewModel.state.value)
            },
            onDelete = { todoId ->
                viewModel.deleteTodo(todoId)
                renderState(viewModel.state.value)
            },
            onEdit = { todoId, newTitle ->
                viewModel.editTodo(todoId, newTitle)
                renderState(viewModel.state.value)
            },
            onStartDrag = { holder ->
                itemTouchHelperInternal.startDrag(holder)
            }
        )

        recyclerViewInternal = findViewById(R.id.recyclerView)
        recyclerViewInternal.layoutManager = LinearLayoutManager(this)
        recyclerViewInternal.adapter = adapter

        itemTouchHelperInternal = ItemTouchHelper(buildDragCallback())
        itemTouchHelperInternal.attachToRecyclerView(recyclerViewInternal)

        renderState(viewModel.state.value)
    }

    private fun wireInlineAddRow() {
        val inlineAddRow = findViewById<View>(R.id.inlineAddRow)
        val ghostRow = inlineAddRow.findViewById<View>(R.id.ghostRow)
        val expandedRow = inlineAddRow.findViewById<View>(R.id.expandedRow)
        val editText = inlineAddRow.findViewById<TextInputEditText>(R.id.editInlineAdd)
        val submitButton = inlineAddRow.findViewById<MaterialButton>(R.id.btnInlineSubmit)
        inlineAddEditTextInternal = editText

        fun showExpanded(expanded: Boolean) {
            ghostRow.visibility = if (expanded) View.GONE else View.VISIBLE
            expandedRow.visibility = if (expanded) View.VISIBLE else View.GONE
        }

        fun trySubmit() {
            val title = editText.text?.toString().orEmpty()
            if (title.isNotBlank()) {
                viewModel.submitInlineInput(title)
                editText.text?.clear()
                renderState(viewModel.state.value)
            }
        }

        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                trySubmit()
                true
            } else {
                false
            }
        }

        submitButton.setOnClickListener { trySubmit() }

        editText.setOnFocusChangeListener { _, hasFocus ->
            showExpanded(hasFocus)
            if (hasFocus) {
                val imm = getSystemService(InputMethodManager::class.java)
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        ghostRow.setOnClickListener {
            showExpanded(true)
            editText.requestFocus()
        }
    }

    private fun buildDragCallback(): ItemTouchHelper.Callback =
        object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            private var dragFromIndex: Int = RecyclerView.NO_ID.toInt()

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (viewHolder.itemViewType != TodoListAdapter.VIEW_TYPE_ITEM) return 0
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_ID.toInt()) return 0
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
                if (dragFromIndex == RecyclerView.NO_ID.toInt()) {
                    dragFromIndex = fromPos
                }
                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    dragFromIndex = viewHolder?.bindingAdapterPosition ?: RecyclerView.NO_ID.toInt()
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                val toIndex = viewHolder.bindingAdapterPosition
                if (dragFromIndex != RecyclerView.NO_ID.toInt() && toIndex != RecyclerView.NO_ID.toInt() && dragFromIndex != toIndex) {
                    viewModel.reorderTodos(dragFromIndex, toIndex)
                    renderState(viewModel.state.value)
                }
                dragFromIndex = RecyclerView.NO_ID.toInt()
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

    internal fun refreshListForTest() {
        renderState(viewModel.state.value)
    }

    private fun renderState(state: TodoListState) {
        when (state) {
            is TodoListState.Empty -> {
                adapter.submitList(emptyList(), emptyList())
                emptyLayout.visibility = View.VISIBLE
                watermark.alpha = 0.15f
            }
            is TodoListState.Content -> {
                adapter.submitList(state.activeItems, state.completedItems)
                emptyLayout.visibility = View.GONE
                watermark.alpha = 0.08f
            }
        }
    }
}
