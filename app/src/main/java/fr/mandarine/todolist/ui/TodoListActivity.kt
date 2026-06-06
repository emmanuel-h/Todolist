package fr.mandarine.todolist.ui

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.InMemoryTodoRepository
import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.presentation.TodoListViewModel

class TodoListActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoListViewModel
    private lateinit var adapter: TodoListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_list)

        val repository = InMemoryTodoRepository()
        viewModel = TodoListViewModel(AddTodoUseCase(repository), GetTodosUseCase(repository))

        adapter = TodoListAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        refreshList()

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener { showAddDialog() }
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
            is TodoListState.Empty -> adapter.submitList(emptyList())
            is TodoListState.Content -> adapter.submitList(s.items)
        }
    }
}
