package fr.mandarine.todolist.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.InMemoryTodoListRepository
import fr.mandarine.todolist.data.InMemoryTodoRepository
import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListViewModel

class TodoListActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoListViewModel
    private lateinit var adapter: TodoListAdapter
    private lateinit var emptyView: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_list)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val listId = intent.getStringExtra("LIST_ID") ?: run {
            val todoListRepository = InMemoryTodoListRepository()
            CreateTodoListUseCase(todoListRepository)("My List").id
        }

        val todoRepository = InMemoryTodoRepository()
        viewModel = TodoListViewModel(
            AddTodoUseCase(todoRepository),
            GetTodosUseCase(todoRepository),
            listId = listId
        )

        emptyView = findViewById(R.id.textEmptyTodos)

        adapter = TodoListAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        refreshList()

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener { showAddDialog() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun showAddDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.add_item)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val title = input.text.toString()
                if (title.isNotBlank()) {
                    viewModel.addTodo(title)
                    refreshList()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun refreshList() {
        when (val s = viewModel.state) {
            is TodoListState.Empty -> {
                adapter.submitList(emptyList())
                emptyView.visibility = View.VISIBLE
            }
            is TodoListState.Content -> {
                adapter.submitList(s.items)
                emptyView.visibility = View.GONE
            }
        }
    }
}
