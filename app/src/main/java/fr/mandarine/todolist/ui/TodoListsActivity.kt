package fr.mandarine.todolist.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.RoomTodoListRepository
import fr.mandarine.todolist.data.RoomTodoRepository
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.presentation.TodoListsViewModel

class TodoListsActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoListsViewModel
    private lateinit var adapter: TodoListsAdapter
    private lateinit var emptyLayout: View
    private lateinit var fab: FloatingActionButton
    internal lateinit var recyclerViewInternal: RecyclerView
    internal lateinit var inlineAddRowInternal: View
    internal var itemTouchHelperInternal: ItemTouchHelper? = null

    private var dragFromIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_lists)

        val db = TodoDatabase.getInstance(this)
        val todoListRepository = RoomTodoListRepository(db.todoListDao())
        val todoRepository = RoomTodoRepository(db.todoItemDao())
        viewModel = TodoListsViewModel(
            CreateTodoListUseCase(todoListRepository),
            DeleteTodoListUseCase(todoListRepository, todoRepository),
            EditTodoListUseCase(todoListRepository),
            GetTodoListsUseCase(todoListRepository),
            ReorderTodoListsUseCase(todoListRepository)
        )

        emptyLayout = findViewById(R.id.layoutEmptyLists)
        fab = findViewById(R.id.fabAddList)
        inlineAddRowInternal = findViewById(R.id.inlineAddListRow)

        adapter = TodoListsAdapter(
            onListClick = { list -> openList(list) },
            onDeleteClick = { list -> showDeleteConfirmation(list) },
            onRenameClick = { list -> showRenameDialog(list) },
            onDragStart = { holder -> itemTouchHelperInternal?.startDrag(holder) }
        )

        recyclerViewInternal = findViewById(R.id.recyclerViewLists)
        recyclerViewInternal.layoutManager = LinearLayoutManager(this)
        recyclerViewInternal.adapter = adapter

        val touchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun isLongPressDragEnabled(): Boolean = false

            override fun onMove(
                rv: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (dragFromIndex < 0) dragFromIndex = from
                adapter.moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun clearView(rv: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(rv, viewHolder)
                val to = viewHolder.bindingAdapterPosition
                if (dragFromIndex >= 0 && dragFromIndex != to) {
                    viewModel.reorderLists(dragFromIndex, to)
                    refreshLists()
                }
                dragFromIndex = -1
            }
        }

        itemTouchHelperInternal = ItemTouchHelper(touchCallback)
        itemTouchHelperInternal!!.attachToRecyclerView(recyclerViewInternal)

        wireInlineAddRow()

        refreshLists()

        fab.setOnClickListener {
            showInlineAddRow()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLists()
    }

    private fun wireInlineAddRow() {
        val editText = inlineAddRowInternal.findViewById<TextInputEditText>(R.id.editListInlineAdd)
        val submitButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineSubmit)
        val cancelButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineCancel)

        fun trySubmit() {
            val name = editText.text?.toString().orEmpty()
            if (viewModel.submitInlineInput(name)) {
                editText.text?.clear()
                hideInlineAddRow()
                refreshLists()
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

        cancelButton.setOnClickListener {
            editText.text?.clear()
            hideInlineAddRow()
        }
    }

    private fun showInlineAddRow() {
        inlineAddRowInternal.visibility = View.VISIBLE
        val divider = findViewById<View>(R.id.inlineAddListDivider)
        divider.visibility = View.VISIBLE
        fab.visibility = View.GONE
        emptyLayout.visibility = View.GONE
        val editText = inlineAddRowInternal.findViewById<TextInputEditText>(R.id.editListInlineAdd)
        editText.requestFocus()
        val imm = getSystemService(InputMethodManager::class.java)
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideInlineAddRow() {
        inlineAddRowInternal.visibility = View.GONE
        val divider = findViewById<View>(R.id.inlineAddListDivider)
        divider.visibility = View.GONE
        fab.visibility = View.VISIBLE
        val editText = inlineAddRowInternal.findViewById<TextInputEditText>(R.id.editListInlineAdd)
        editText.clearFocus()
        val imm = getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
        refreshLists()
    }

    private fun showRenameDialog(list: TodoList) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename_list, null)
        currentDialogView = dialogView
        val input = dialogView.findViewById<TextInputEditText>(R.id.editDialogRenameList)
        input.setText(list.name)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        dialogView.findViewById<MaterialButton>(R.id.btnDialogConfirm).setOnClickListener {
            val newName = input.text.toString()
            if (newName.isNotBlank()) {
                viewModel.editList(list.id, newName)
                refreshLists()
                dialog.dismiss()
            }
        }
        dialogView.findViewById<MaterialButton>(R.id.btnDialogCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDeleteConfirmation(list: TodoList) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_list, null)
        currentDialogView = dialogView
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        dialogView.findViewById<MaterialButton>(R.id.btnDialogConfirm).setOnClickListener {
            viewModel.deleteList(list.id)
            refreshLists()
            dialog.dismiss()
        }
        dialogView.findViewById<MaterialButton>(R.id.btnDialogCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    internal var currentDialogView: android.view.View? = null

    internal fun tapFab() {
        fab.performClick()
    }

    internal fun typeInInlineRowForTest(text: String) {
        inlineAddRowInternal.findViewById<TextInputEditText>(R.id.editListInlineAdd).setText(text)
    }

    internal fun submitInlineRowForTest() {
        inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineSubmit).performClick()
    }

    internal fun cancelInlineRowForTest() {
        inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineCancel).performClick()
    }

    internal fun typeInRenameDialogForTest(text: String) {
        currentDialogView?.findViewById<TextInputEditText>(R.id.editDialogRenameList)?.apply {
            setText(text)
        }
    }

    internal fun confirmDialogForTest() {
        currentDialogView?.findViewById<MaterialButton>(R.id.btnDialogConfirm)?.performClick()
    }

    internal fun cancelCurrentDialogForTest() {
        currentDialogView?.findViewById<MaterialButton>(R.id.btnDialogCancel)?.performClick()
    }

    internal fun commitReorderForTest(fromIndex: Int, toIndex: Int) {
        viewModel.reorderLists(fromIndex, toIndex)
        refreshLists()
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
                if (inlineAddRowInternal.visibility != View.VISIBLE) {
                    emptyLayout.visibility = View.VISIBLE
                }
            }
            is TodoListsState.Content -> {
                adapter.submitList(s.lists)
                emptyLayout.visibility = View.GONE
            }
        }
    }
}
