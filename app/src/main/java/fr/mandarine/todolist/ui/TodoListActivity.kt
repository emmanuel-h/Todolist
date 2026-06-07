package fr.mandarine.todolist.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.RoomTodoRepository
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.domain.ToggleTodoUseCase
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListViewModel

class TodoListActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoListViewModel
    private lateinit var adapter: TodoListAdapter
    private lateinit var emptyLayout: View
    internal lateinit var recyclerViewInternal: RecyclerView

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
            listId = listId
        )

        emptyLayout = findViewById(R.id.layoutEmptyTodos)

        adapter = TodoListAdapter(
            onToggle = { todoId ->
                viewModel.toggleTodo(todoId)
                renderState(viewModel.state.value)
            },
            onSubmit = { title ->
                viewModel.submitInlineInput(title)
                renderState(viewModel.state.value)
            }
        )

        recyclerViewInternal = findViewById(R.id.recyclerView)
        recyclerViewInternal.layoutManager = LinearLayoutManager(this)
        recyclerViewInternal.adapter = adapter
        recyclerViewInternal.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        renderState(viewModel.state.value)
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
                adapter.submitList(emptyList())
                emptyLayout.visibility = View.GONE
            }
            is TodoListState.Content -> {
                adapter.submitList(state.items)
                emptyLayout.visibility = View.GONE
            }
        }
    }
}
