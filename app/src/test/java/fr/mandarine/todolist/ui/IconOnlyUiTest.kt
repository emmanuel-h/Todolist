package fr.mandarine.todolist.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconOnlyUiTest {

    private lateinit var adapter: TodoListAdapter
    private lateinit var themedContext: Context

    @Before
    fun setUp() {
        val onToggle: (String) -> Unit = mockk(relaxed = true)
        val onDelete: (String) -> Unit = mockk(relaxed = true)
        val onEdit: (String, String) -> Unit = mockk(relaxed = true)
        adapter = TodoListAdapter(
            onToggle = onToggle,
            onDelete = onDelete,
            onEdit = onEdit
        )
        themedContext = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_ToDoList
        )
    }

    @Test
    fun `should show only numeric count in divider label without the word Completed`() {
        val activeItems = listOf(TodoItem("1", "Buy milk", "list-1"))
        val completedItems = listOf(
            TodoItem("2", "Call dentist", "list-1", isCompleted = true, completedAt = 1000L),
            TodoItem("3", "Walk the dog", "list-1", isCompleted = true, completedAt = 2000L)
        )
        adapter.submitList(activeItems, completedItems)

        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_DIVIDER)
            as TodoListAdapter.DividerViewHolder
        adapter.onBindViewHolder(holder, 1)

        val label = holder.itemView.findViewById<MaterialTextView>(R.id.textDividerLabel)
        val labelText = label.text.toString()
        assertEquals("2", labelText)
    }

    @Test
    fun `should show count of one in divider label when exactly one completed item exists`() {
        val activeItems = listOf(TodoItem("1", "Buy milk", "list-1"))
        val completedItems = listOf(
            TodoItem("2", "Call dentist", "list-1", isCompleted = true, completedAt = 1000L)
        )
        adapter.submitList(activeItems, completedItems)

        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_DIVIDER)
            as TodoListAdapter.DividerViewHolder
        adapter.onBindViewHolder(holder, 1)

        val label = holder.itemView.findViewById<MaterialTextView>(R.id.textDividerLabel)
        assertEquals("1", label.text.toString())
    }

    @Test
    fun `should not contain the word Completed in divider label text`() {
        val activeItems = listOf(TodoItem("1", "Buy milk", "list-1"))
        val completedItems = listOf(
            TodoItem("2", "Call dentist", "list-1", isCompleted = true, completedAt = 1000L)
        )
        adapter.submitList(activeItems, completedItems)

        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_DIVIDER)
            as TodoListAdapter.DividerViewHolder
        adapter.onBindViewHolder(holder, 1)

        val label = holder.itemView.findViewById<MaterialTextView>(R.id.textDividerLabel)
        val labelText = label.text.toString().lowercase()
        assert(!labelText.contains("completed")) {
            "Expected divider label to not contain 'completed' but was: '${label.text}'"
        }
    }

    @Test
    fun `should not contain any text views in the empty state of todo list`() {
        val parent = FrameLayout(themedContext)
        val contentView = LayoutInflater.from(themedContext)
            .inflate(R.layout.activity_todo_list, parent, false)
        val emptyLayout = contentView.findViewById<ViewGroup>(R.id.layoutEmptyTodos)

        val textViewCount = countTextViews(emptyLayout)
        assertEquals("Empty state in todo list should have no visible text views", 0, textViewCount)
    }

    @Test
    fun `should not contain any text views in the empty state of lists screen`() {
        val parent = FrameLayout(themedContext)
        val contentView = LayoutInflater.from(themedContext)
            .inflate(R.layout.activity_todo_lists, parent, false)
        val emptyLayout = contentView.findViewById<ViewGroup>(R.id.layoutEmptyLists)

        val textViewCount = countTextViews(emptyLayout)
        assertEquals("Empty state in lists screen should have no visible text views", 0, textViewCount)
    }

    @Test
    fun `should not have a toolbar in the lists screen`() {
        val parent = FrameLayout(themedContext)
        val listsView = LayoutInflater.from(themedContext)
            .inflate(R.layout.activity_todo_lists, parent, false)

        val toolbar = listsView.findViewById<android.view.View>(R.id.toolbar)
        assert(toolbar == null) {
            "Expected no toolbar in the lists screen but found one"
        }
    }

    @Test
    fun `should have confirm icon button in create list dialog layout`() {
        val parent = FrameLayout(themedContext)
        val dialogView = LayoutInflater.from(themedContext)
            .inflate(R.layout.dialog_create_list, parent, false)

        val confirmBtn = dialogView.findViewById<android.view.View>(R.id.btnDialogConfirm)
        assertNotNull("Expected btnDialogConfirm to exist in dialog_create_list layout", confirmBtn)
    }

    @Test
    fun `should have cancel icon button in create list dialog layout`() {
        val parent = FrameLayout(themedContext)
        val dialogView = LayoutInflater.from(themedContext)
            .inflate(R.layout.dialog_create_list, parent, false)

        val cancelBtn = dialogView.findViewById<android.view.View>(R.id.btnDialogCancel)
        assertNotNull("Expected btnDialogCancel to exist in dialog_create_list layout", cancelBtn)
    }

    @Test
    fun `should have confirm icon button in delete list dialog layout`() {
        val parent = FrameLayout(themedContext)
        val dialogView = LayoutInflater.from(themedContext)
            .inflate(R.layout.dialog_delete_list, parent, false)

        val confirmBtn = dialogView.findViewById<android.view.View>(R.id.btnDialogConfirm)
        assertNotNull("Expected btnDialogConfirm to exist in dialog_delete_list layout", confirmBtn)
    }

    @Test
    fun `should have cancel icon button in delete list dialog layout`() {
        val parent = FrameLayout(themedContext)
        val dialogView = LayoutInflater.from(themedContext)
            .inflate(R.layout.dialog_delete_list, parent, false)

        val cancelBtn = dialogView.findViewById<android.view.View>(R.id.btnDialogCancel)
        assertNotNull("Expected btnDialogCancel to exist in dialog_delete_list layout", cancelBtn)
    }

    private fun countTextViews(viewGroup: ViewGroup?): Int {
        if (viewGroup == null) return 0
        var count = 0
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is MaterialTextView) count++
            if (child is ViewGroup) count += countTextViews(child)
        }
        return count
    }
}
