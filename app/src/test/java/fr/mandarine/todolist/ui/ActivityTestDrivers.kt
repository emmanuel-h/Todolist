package fr.mandarine.todolist.ui

import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import fr.mandarine.todolist.R
import java.time.LocalDate

internal fun TodoListsActivity.tapFab() {
    findViewById<FloatingActionButton>(R.id.fabAddList).performClick()
}

internal fun TodoListsActivity.typeInInlineRowForTest(text: String) {
    inlineAddRowInternal.findViewById<TextInputEditText>(R.id.editListInlineAdd).setText(text)
}

internal fun TodoListsActivity.submitInlineRowForTest() {
    inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineSubmit).performClick()
}

internal fun TodoListsActivity.cancelInlineRowForTest() {
    inlineAddRowInternal.findViewById<MaterialButton>(R.id.btnListInlineCancel).performClick()
}

internal fun TodoListsActivity.triggerImeActionForTest(actionId: Int) {
    inlineAddRowInternal.findViewById<TextInputEditText>(R.id.editListInlineAdd).onEditorAction(actionId)
}

internal fun TodoListsActivity.typeInRenameDialogForTest(text: String) {
    currentDialogView?.findViewById<TextInputEditText>(R.id.editDialogRenameList)?.setText(text)
}

internal fun TodoListsActivity.confirmDialogForTest() {
    currentDialogView?.findViewById<MaterialButton>(R.id.btnDialogConfirm)?.performClick()
}

internal fun TodoListsActivity.cancelCurrentDialogForTest() {
    currentDialogView?.findViewById<MaterialButton>(R.id.btnDialogCancel)?.performClick()
}

internal fun TodoListsActivity.commitReorderForTest(fromIndex: Int, toIndex: Int) {
    viewModel.reorderLists(fromIndex, toIndex)
}

internal fun TodoListsActivity.createListWithDateForTest(name: String, date: LocalDate?) {
    viewModel.createList(name, date)
}

internal fun TodoListsActivity.createListWithDueDateForTest(name: String, dueDate: LocalDate?) {
    viewModel.createList(name, null, dueDate)
}

internal fun TodoListsActivity.setInlineDateForTest(date: LocalDate) {
    onInlineTargetDatePicked(date)
}

internal fun TodoListsActivity.setInlineDueDateForTest(date: LocalDate) {
    onInlineDueDatePicked(date)
}

internal fun TodoListsActivity.setRenameTargetDateForTest(date: LocalDate) {
    currentDialogView?.findViewById<MaterialButtonToggleGroup>(R.id.toggleDateKind)
        ?.check(R.id.btnToggleTargetDate)
    onRenameTargetDatePicked(date)
}

internal fun TodoListsActivity.setRenameDueDateForTest(date: LocalDate) {
    currentDialogView?.findViewById<MaterialButtonToggleGroup>(R.id.toggleDateKind)
        ?.check(R.id.btnToggleDueDate)
    onRenameDueDatePicked(date)
}

internal fun TodoListsActivity.clearRenameDateForTest() {
    currentDialogView?.findViewById<MaterialButton>(R.id.btnDialogClearDate)?.performClick()
}

internal fun TodoListsActivity.clickToggleTargetDateForTest() {
    currentDialogView?.findViewById<MaterialButton>(R.id.btnToggleTargetDate)?.performClick()
}

internal fun TodoListsActivity.clickToggleDueDateForTest() {
    currentDialogView?.findViewById<MaterialButton>(R.id.btnToggleDueDate)?.performClick()
}

internal fun TodoListActivity.refreshListForTest() {
    viewModel.refresh()
}

internal val TodoListActivity.inlineAddEditTextInternal: TextInputEditText
    get() {
        for (i in 0 until recyclerViewInternal.childCount) {
            val holder = recyclerViewInternal.getChildViewHolder(recyclerViewInternal.getChildAt(i))
            if (holder is TodoListAdapter.InlineAddViewHolder) return holder.editText
        }
        error("InlineAdd ViewHolder not found - call layoutRecyclerView() before accessing this")
    }
