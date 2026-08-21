package fr.mandarine.todolist.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        adapter.onBindViewHolder(holder, 2)

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
        adapter.onBindViewHolder(holder, 2)

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
        adapter.onBindViewHolder(holder, 2)

        val label = holder.itemView.findViewById<MaterialTextView>(R.id.textDividerLabel)
        val labelText = label.text.toString().lowercase()
        assert(!labelText.contains("completed")) {
            "Expected divider label to not contain 'completed' but was: '${label.text}'"
        }
    }

    @Test
    fun `should not contain any static text in the empty state of todo list`() {
        val parent = FrameLayout(themedContext)
        val contentView = LayoutInflater.from(themedContext)
            .inflate(R.layout.activity_todo_list, parent, false)

        assertEquals(emptyList<String>(), staticTextIn(contentView))
    }

    @Test
    fun `should not contain any static text in the empty state of lists screen`() {
        val parent = FrameLayout(themedContext)
        val contentView = LayoutInflater.from(themedContext)
            .inflate(R.layout.activity_todo_lists, parent, false)

        assertEquals(emptyList<String>(), staticTextIn(contentView))
    }

    @Test
    fun `should not contain a background illustration in either screen`() {
        val parent = FrameLayout(themedContext)
        val listView = LayoutInflater.from(themedContext)
            .inflate(R.layout.activity_todo_list, parent, false)
        val listsView = LayoutInflater.from(themedContext)
            .inflate(R.layout.activity_todo_lists, parent, false)

        assertEquals(emptyList<String>(), decorativeImagesIn(listView))
        assertEquals(emptyList<String>(), decorativeImagesIn(listsView))
    }

    private fun staticTextIn(view: android.view.View): List<String> = when {
        view is android.widget.TextView && view.text.isNotBlank() -> listOf(view.text.toString())
        view is android.view.ViewGroup ->
            (0 until view.childCount).flatMap { staticTextIn(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun decorativeImagesIn(view: android.view.View): List<String> = when {
        view is android.widget.ImageView && view.contentDescription.isNullOrBlank() ->
            listOf(view.javaClass.simpleName)
        view is android.view.ViewGroup ->
            (0 until view.childCount).flatMap { decorativeImagesIn(view.getChildAt(it)) }
        else -> emptyList()
    }

    @Test
    fun `should not have a toolbar in the lists screen`() {
        val parent = FrameLayout(themedContext)
        val listsView = LayoutInflater.from(themedContext)
            .inflate(R.layout.activity_todo_lists, parent, false)

        val toolbar = listsView.findViewById<android.view.View>(R.id.toolbar)
        assertNull("Expected no toolbar in the lists screen", toolbar)
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
    fun `should have icon-only confirm button in list row delete confirm strip`() {
        val parent = FrameLayout(themedContext)
        val rowView = LayoutInflater.from(themedContext)
            .inflate(R.layout.item_todo_list, parent, false)

        val confirmBtn = rowView.findViewById<android.widget.ImageButton>(R.id.btnDeleteConfirm)
        assertNotNull("Expected btnDeleteConfirm to exist in item_todo_list layout", confirmBtn)
    }

    @Test
    fun `should have icon-only cancel button in list row delete confirm strip`() {
        val parent = FrameLayout(themedContext)
        val rowView = LayoutInflater.from(themedContext)
            .inflate(R.layout.item_todo_list, parent, false)

        val cancelBtn = rowView.findViewById<android.widget.ImageButton>(R.id.btnDeleteCancel)
        assertNotNull("Expected btnDeleteCancel to exist in item_todo_list layout", cancelBtn)
    }
}
