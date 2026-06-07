package fr.mandarine.todolist.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.RoomTodoListRepository
import fr.mandarine.todolist.data.RoomTodoRepository
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.presentation.TodoListsViewModel

class TodoListsActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoListsViewModel
    private lateinit var adapter: TodoListsAdapter
    private lateinit var emptyLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_lists)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        val db = TodoDatabase.getInstance(this)
        val todoListRepository = RoomTodoListRepository(db.todoListDao())
        val todoRepository = RoomTodoRepository(db.todoItemDao())
        viewModel = TodoListsViewModel(
            CreateTodoListUseCase(todoListRepository),
            DeleteTodoListUseCase(todoListRepository, todoRepository),
            GetTodoListsUseCase(todoListRepository)
        )

        emptyLayout = findViewById(R.id.layoutEmptyLists)

        adapter = TodoListsAdapter(
            onListClick = { list -> openList(list) },
            onDeleteClick = { list -> showDeleteConfirmation(list) }
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewLists)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        refreshLists()

        findViewById<FloatingActionButton>(R.id.fabAddList).setOnClickListener {
            showCreateListDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLists()
    }

    private fun showCreateListDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_list, null)
        val input = dialogView.findViewById<TextInputEditText>(R.id.editDialogCreateList)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.create_list)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = input.text.toString()
                if (name.isNotBlank()) {
                    viewModel.createList(name)
                    refreshLists()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirmation(list: TodoList) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_list_confirmation_title)
            .setMessage(R.string.delete_list_confirmation_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteList(list.id)
                refreshLists()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openList(list: TodoList) {
        val intent = Intent(this, TodoListActivity::class.java)
        intent.putExtra("LIST_ID", list.id)
        intent.putExtra("LIST_NAME", list.name)
        startActivity(intent)
    }

    private fun refreshLists() {
        when (val s = viewModel.state) {
            is TodoListsState.Empty -> {
                adapter.submitList(emptyList())
                emptyLayout.visibility = View.VISIBLE
            }
            is TodoListsState.Content -> {
                adapter.submitList(s.lists)
                emptyLayout.visibility = View.GONE
            }
        }
    }
}
