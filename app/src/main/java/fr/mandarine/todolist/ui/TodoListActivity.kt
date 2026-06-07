package fr.mandarine.todolist.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textview.MaterialTextView
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
    private lateinit var emptyView: MaterialTextView
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

        emptyView = findViewById(R.id.textEmptyTodos)

        adapter = TodoListAdapter(
            onCommit = { title ->
                val added = viewModel.submitInlineInput(title)
                if (added) {
                    renderState(viewModel.state.value)
                    recyclerViewInternal.scrollToPosition(adapter.itemCount - 1)
                    adapter.requestAddRowFocus()
                }
            },
            onToggle = { todoId ->
                viewModel.toggleTodo(todoId)
                renderState(viewModel.state.value)
            }
        )
        recyclerViewInternal = findViewById(R.id.recyclerView)
        recyclerViewInternal.layoutManager = LinearLayoutManager(this)
        recyclerViewInternal.adapter = adapter

        renderState(viewModel.state.value)
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
                emptyView.visibility = View.GONE
            }
            is TodoListState.Content -> {
                adapter.submitList(state.items)
                emptyView.visibility = View.GONE
            }
        }
    }
}
