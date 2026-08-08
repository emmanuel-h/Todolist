package fr.mandarine.todolist.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.data.RoomTodoListRepository
import fr.mandarine.todolist.data.RoomTodoRepository
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import fr.mandarine.todolist.domain.SystemClock
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.presentation.TodoListsState
import fr.mandarine.todolist.presentation.TodoListsViewModel
import java.time.LocalDate
import java.util.Locale

class TodoListsActivity : AppCompatActivity() {

    private lateinit var viewModel: TodoListsViewModel
    private lateinit var adapter: TodoListsAdapter
    private lateinit var emptyLayout: View
    private lateinit var fab: FloatingActionButton
    internal lateinit var recyclerViewInternal: RecyclerView
    internal lateinit var inlineAddRowInternal: View
    internal var itemTouchHelperInternal: ItemTouchHelper? = null

    private var dragFromIndex: Int = -1
    private var selectedInlineDate: LocalDate? = null
    internal var selectedRenameDate: LocalDate? = null

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
            GetTodoListsWithStatusUseCase(todoListRepository, todoRepository, SystemClock()),
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

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (viewHolder.itemViewType != TodoListsAdapter.VIEW_TYPE_ITEM) return 0
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_ID.toInt()) return 0
                return if (position < adapter.activeItemCount()) {
                    makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                } else {
                    0
                }
            }

            override fun onMove(
                rv: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                val activeCount = adapter.activeItemCount()
                if (to < 0 || to >= activeCount) return false
                if (target.itemViewType != TodoListsAdapter.VIEW_TYPE_ITEM) return false
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
        val dateButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineDate)

        fun trySubmit() {
            val name = editText.text?.toString().orEmpty()
            if (name.isBlank()) return
            viewModel.createList(name, selectedInlineDate)
            selectedInlineDate = null
            dateButton.setIconResource(R.drawable.ic_event_add)
            updateInlineDateButtonTint(dateButton, hasDate = false)
            editText.text?.clear()
            hideInlineAddRow()
            refreshLists()
        }

        editText.setOnEditorActionListener { _, actionId, event ->
            val isDone = actionId == EditorInfo.IME_ACTION_DONE
            val isUnspecified = actionId == EditorInfo.IME_ACTION_UNSPECIFIED
            val isEnterKey = event != null &&
                event.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_DOWN
            if (isDone || isUnspecified || isEnterKey) {
                trySubmit()
                true
            } else {
                false
            }
        }

        submitButton.setOnClickListener { trySubmit() }

        cancelButton.setOnClickListener {
            selectedInlineDate = null
            dateButton.setIconResource(R.drawable.ic_event_add)
            updateInlineDateButtonTint(dateButton, hasDate = false)
            editText.text?.clear()
            hideInlineAddRow()
        }

        dateButton.setOnClickListener {
            val current = selectedInlineDate ?: LocalDate.now()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedInlineDate = LocalDate.of(year, month + 1, dayOfMonth)
                    dateButton.setIconResource(R.drawable.ic_event)
                    updateInlineDateButtonTint(dateButton, hasDate = true)
                },
                current.year,
                current.monthValue - 1,
                current.dayOfMonth
            ).show()
        }
    }

    private fun updateInlineDateButtonTint(button: MaterialButton, hasDate: Boolean) {
        val attr = if (hasDate) {
            com.google.android.material.R.attr.colorPrimary
        } else {
            com.google.android.material.R.attr.colorOnSurfaceVariant
        }
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        button.iconTint = ColorStateList.valueOf(typedValue.data)
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
        selectedRenameDate = list.targetDate
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename_list, null)
        currentDialogView = dialogView
        val input = dialogView.findViewById<TextInputEditText>(R.id.editDialogRenameList)
        input.setText(list.name)

        updateRenameDateDisplay()

        dialogView.findViewById<View>(R.id.layoutDialogDate).setOnClickListener {
            val current = selectedRenameDate ?: LocalDate.now()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedRenameDate = LocalDate.of(year, month + 1, dayOfMonth)
                    updateRenameDateDisplay()
                },
                current.year,
                current.monthValue - 1,
                current.dayOfMonth
            ).show()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnDialogClearDate).setOnClickListener {
            selectedRenameDate = null
            updateRenameDateDisplay()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        dialogView.findViewById<MaterialButton>(R.id.btnDialogConfirm).setOnClickListener {
            val newName = input.text.toString()
            if (newName.isNotBlank()) {
                viewModel.editList(list.id, newName, selectedRenameDate)
                refreshLists()
                dialog.dismiss()
            }
        }
        dialogView.findViewById<MaterialButton>(R.id.btnDialogCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    internal fun updateRenameDateDisplay() {
        val dialogView = currentDialogView ?: return
        val addIcon = dialogView.findViewById<ImageView>(R.id.iconDateAddAffordance) ?: return
        val dateText = dialogView.findViewById<MaterialTextView>(R.id.textDialogTargetDate) ?: return
        val clearButton = dialogView.findViewById<MaterialButton>(R.id.btnDialogClearDate) ?: return
        val date = selectedRenameDate
        if (date != null) {
            addIcon.visibility = View.GONE
            val locale = Locale.getDefault(Locale.Category.FORMAT)
            dateText.text = TodoListsAdapter.formatTargetDate(date, showYear = true, locale)
            dateText.visibility = View.VISIBLE
            clearButton.visibility = View.VISIBLE
        } else {
            addIcon.visibility = View.VISIBLE
            dateText.visibility = View.GONE
            clearButton.visibility = View.GONE
        }
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

    internal fun triggerImeActionForTest(actionId: Int) {
        inlineAddRowInternal
            .findViewById<TextInputEditText>(R.id.editListInlineAdd)
            .onEditorAction(actionId)
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

    internal fun setInlineDateForTest(date: LocalDate?) {
        selectedInlineDate = date
        val dateButton = inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineDate)
        if (date != null) {
            dateButton.setIconResource(R.drawable.ic_event)
            updateInlineDateButtonTint(dateButton, hasDate = true)
        } else {
            dateButton.setIconResource(R.drawable.ic_event_add)
            updateInlineDateButtonTint(dateButton, hasDate = false)
        }
    }

    internal fun setRenameTargetDateForTest(date: LocalDate?) {
        selectedRenameDate = date
        updateRenameDateDisplay()
    }

    internal fun clearRenameDateForTest() {
        currentDialogView?.findViewById<MaterialButton>(R.id.btnDialogClearDate)?.performClick()
    }

    internal fun createListWithDateForTest(name: String, date: LocalDate?) {
        viewModel.createList(name, date)
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
                adapter.submitList(emptyList(), emptyList())
                if (inlineAddRowInternal.visibility != View.VISIBLE) {
                    emptyLayout.visibility = View.VISIBLE
                }
            }
            is TodoListsState.Content -> {
                adapter.submitList(s.activeSummaries, s.doneSummaries)
                emptyLayout.visibility = View.GONE
            }
        }
    }
}
