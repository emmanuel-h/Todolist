package fr.mandarine.todolist.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListAdapterTest {

    private lateinit var onToggle: (String) -> Unit
    private lateinit var onDelete: (String) -> Unit
    private lateinit var onEdit: (String, String) -> Unit
    private lateinit var adapter: TodoListAdapter
    private lateinit var themedContext: Context

    @Before
    fun setUp() {
        onToggle = mockk(relaxed = true)
        onDelete = mockk(relaxed = true)
        onEdit = mockk(relaxed = true)
        adapter = TodoListAdapter(onToggle = onToggle, onDelete = onDelete, onEdit = onEdit)
        themedContext = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_ToDoList
        )
    }

    @Test
    fun `should return items size when getItemCount is called with items`() {
        val items = listOf(
            TodoItem("1", "Buy milk", "list-1"),
            TodoItem("2", "Call dentist", "list-1")
        )
        adapter.submitList(items, emptyList())

        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `should return zero when getItemCount is called with empty list`() {
        adapter.submitList(emptyList(), emptyList())

        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `should return VIEW_TYPE_ITEM for item positions`() {
        val items = listOf(TodoItem("1", "Buy milk", "list-1"))
        adapter.submitList(items, emptyList())

        assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
    }

    @Test
    fun `should create item view holder for VIEW_TYPE_ITEM`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM)
        assertEquals(TodoListAdapter.ItemViewHolder::class.java, holder.javaClass)
    }

    @Test
    fun `should not include divider row when only active items exist`() {
        val activeItems = listOf(TodoItem("1", "Buy milk", "list-1"))
        adapter.submitList(activeItems, emptyList())

        assertEquals(1, adapter.itemCount)
        assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
    }

    @Test
    fun `should not include divider row when only completed items exist`() {
        val completedItems = listOf(TodoItem("1", "Buy milk", "list-1", isCompleted = true, completedAt = 1000L))
        adapter.submitList(emptyList(), completedItems)

        assertEquals(1, adapter.itemCount)
        assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
    }

    @Test
    fun `should include divider row between active and completed items when both sections are non-empty`() {
        val activeItems = listOf(TodoItem("1", "Buy milk", "list-1"))
        val completedItems = listOf(TodoItem("2", "Call dentist", "list-1", isCompleted = true, completedAt = 1000L))
        adapter.submitList(activeItems, completedItems)

        assertEquals(3, adapter.itemCount)
        assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
        assertEquals(TodoListAdapter.VIEW_TYPE_DIVIDER, adapter.getItemViewType(1))
        assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(2))
    }

    @Test
    fun `should bind divider with correct completed count`() {
        val activeItems = listOf(TodoItem("1", "Buy milk", "list-1"))
        val completedItems = listOf(
            TodoItem("2", "Call dentist", "list-1", isCompleted = true, completedAt = 1000L),
            TodoItem("3", "Walk the dog", "list-1", isCompleted = true, completedAt = 2000L)
        )
        adapter.submitList(activeItems, completedItems)

        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_DIVIDER) as TodoListAdapter.DividerViewHolder
        adapter.onBindViewHolder(holder, 1)

        val label = holder.itemView.findViewById<com.google.android.material.textview.MaterialTextView>(fr.mandarine.todolist.R.id.textDividerLabel)
        assertEquals("2", label.text.toString())
    }

    @Test
    fun `should create divider view holder for VIEW_TYPE_DIVIDER`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_DIVIDER)
        assertEquals(TodoListAdapter.DividerViewHolder::class.java, holder.javaClass)
    }

    @Test
    fun `should invoke onToggle when complete button is clicked on active item`() {
        val parent = FrameLayout(themedContext)
        val item = TodoItem("item-1", "Buy milk", "list-1")
        adapter.submitList(listOf(item), emptyList())
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM) as TodoListAdapter.ItemViewHolder
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()

        verify { onToggle("item-1") }
    }

    @Test
    fun `should invoke onToggle when uncomplete button is clicked on completed item`() {
        val parent = FrameLayout(themedContext)
        val item = TodoItem("item-2", "Buy milk", "list-1", isCompleted = true, completedAt = 1000L)
        adapter.submitList(emptyList(), listOf(item))
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM) as TodoListAdapter.ItemViewHolder
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<MaterialButton>(R.id.btnToggleComplete).performClick()

        verify { onToggle("item-2") }
    }

    @Test
    fun `should invoke onDelete when delete button is clicked`() {
        val parent = FrameLayout(themedContext)
        val item = TodoItem("item-1", "Buy milk", "list-1")
        adapter.submitList(listOf(item), emptyList())
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM) as TodoListAdapter.ItemViewHolder
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<MaterialButton>(R.id.btnDelete).performClick()

        verify { onDelete("item-1") }
    }

    @Test
    fun `should show inline edit field and hide title when edit button is clicked`() {
        val parent = FrameLayout(themedContext)
        val item = TodoItem("item-1", "Buy milk", "list-1")
        adapter.submitList(listOf(item), emptyList())
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM) as TodoListAdapter.ItemViewHolder
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<MaterialButton>(R.id.btnEdit).performClick()

        val titleView = holder.itemView.findViewById<MaterialTextView>(R.id.textTitle)
        val editField = holder.itemView.findViewById<TextInputEditText>(R.id.editTitleInline)
        assertEquals(View.GONE, titleView.visibility)
        assertEquals(View.VISIBLE, editField.visibility)
    }

    @Test
    fun `should prefill inline edit field with current title when edit button is clicked`() {
        val parent = FrameLayout(themedContext)
        val item = TodoItem("item-1", "Buy milk", "list-1")
        adapter.submitList(listOf(item), emptyList())
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM) as TodoListAdapter.ItemViewHolder
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<MaterialButton>(R.id.btnEdit).performClick()

        val editField = holder.itemView.findViewById<TextInputEditText>(R.id.editTitleInline)
        assertEquals("Buy milk", editField.text.toString())
    }

    @Test
    fun `should invoke onEdit with new title when IME Done is triggered on inline edit field`() {
        val parent = FrameLayout(themedContext)
        val item = TodoItem("item-1", "Buy milk", "list-1")
        adapter.submitList(listOf(item), emptyList())
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM) as TodoListAdapter.ItemViewHolder
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<MaterialButton>(R.id.btnEdit).performClick()

        val editField = holder.itemView.findViewById<TextInputEditText>(R.id.editTitleInline)
        editField.setText("Whole milk")
        editField.onEditorAction(EditorInfo.IME_ACTION_DONE)

        verify { onEdit("item-1", "Whole milk") }
    }

    @Test
    fun `should not invoke onEdit when IME Done is triggered with blank title`() {
        val parent = FrameLayout(themedContext)
        val item = TodoItem("item-1", "Buy milk", "list-1")
        adapter.submitList(listOf(item), emptyList())
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM) as TodoListAdapter.ItemViewHolder
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<MaterialButton>(R.id.btnEdit).performClick()

        val editField = holder.itemView.findViewById<TextInputEditText>(R.id.editTitleInline)
        editField.setText("   ")
        editField.onEditorAction(EditorInfo.IME_ACTION_DONE)

        verify(exactly = 0) { onEdit(any(), any()) }
    }

    @Test
    fun `should restore title view after IME Done on inline edit field`() {
        val parent = FrameLayout(themedContext)
        val item = TodoItem("item-1", "Buy milk", "list-1")
        adapter.submitList(listOf(item), emptyList())
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM) as TodoListAdapter.ItemViewHolder
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<MaterialButton>(R.id.btnEdit).performClick()

        val editField = holder.itemView.findViewById<TextInputEditText>(R.id.editTitleInline)
        val titleView = holder.itemView.findViewById<MaterialTextView>(R.id.textTitle)
        editField.setText("Whole milk")
        editField.onEditorAction(EditorInfo.IME_ACTION_DONE)

        assertEquals(View.VISIBLE, titleView.visibility)
        assertEquals(View.GONE, editField.visibility)
    }

    @Test
    fun `should invoke onEdit with new title when focus is lost on inline edit field`() {
        val parent = FrameLayout(themedContext)
        val item = TodoItem("item-1", "Buy milk", "list-1")
        adapter.submitList(listOf(item), emptyList())
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM) as TodoListAdapter.ItemViewHolder
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<MaterialButton>(R.id.btnEdit).performClick()

        val editField = holder.itemView.findViewById<TextInputEditText>(R.id.editTitleInline)
        editField.setText("Whole milk")
        editField.clearFocus()

        verify { onEdit("item-1", "Whole milk") }
    }

    @Test
    fun `should not have a checkbox view on item row`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM)
        val hasCheckbox = (holder.itemView as android.view.ViewGroup)
            .let { vg -> (0 until vg.childCount).any { vg.getChildAt(it) is com.google.android.material.checkbox.MaterialCheckBox } }
        assertFalse(hasCheckbox)
    }

    @Test
    fun `should show title with strikethrough and reduced alpha for completed item`() {
        val parent = FrameLayout(themedContext)
        val item = TodoItem("item-1", "Done item", "list-1", isCompleted = true, completedAt = 1000L)
        adapter.submitList(emptyList(), listOf(item))
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM) as TodoListAdapter.ItemViewHolder
        adapter.onBindViewHolder(holder, 0)

        val titleView = holder.itemView.findViewById<MaterialTextView>(R.id.textTitle)
        assertEquals(0.5f, titleView.alpha, 0.01f)
        assertTrue((titleView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG) != 0)
    }

    @Test
    fun `should show title without strikethrough and full alpha for active item`() {
        val parent = FrameLayout(themedContext)
        val item = TodoItem("item-1", "Active item", "list-1")
        adapter.submitList(listOf(item), emptyList())
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM) as TodoListAdapter.ItemViewHolder
        adapter.onBindViewHolder(holder, 0)

        val titleView = holder.itemView.findViewById<MaterialTextView>(R.id.textTitle)
        assertEquals(1.0f, titleView.alpha, 0.01f)
        assertEquals(0, titleView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG)
    }

    @Test
    fun `should have complete button and no checkbox on item row`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM)
        assertNotNull(holder.itemView.findViewById<MaterialButton>(R.id.btnToggleComplete))
    }

    @Test
    fun `should have delete button on item row`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM)
        assertNotNull(holder.itemView.findViewById<MaterialButton>(R.id.btnDelete))
    }

    @Test
    fun `should have edit button on item row`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM)
        assertNotNull(holder.itemView.findViewById<MaterialButton>(R.id.btnEdit))
    }
}
