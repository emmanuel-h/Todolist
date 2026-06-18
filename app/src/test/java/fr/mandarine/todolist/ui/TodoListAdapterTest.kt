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
    fun `should return item count plus one for inline add row when active items exist`() {
        val items = listOf(
            TodoItem("1", "Buy milk", "list-1"),
            TodoItem("2", "Call dentist", "list-1")
        )
        adapter.submitList(items, emptyList())

        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `should return one for inline add row when list is empty`() {
        adapter.submitList(emptyList(), emptyList())

        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `should return VIEW_TYPE_ITEM for active item positions`() {
        val items = listOf(TodoItem("1", "Buy milk", "list-1"))
        adapter.submitList(items, emptyList())

        assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
    }

    @Test
    fun `should return VIEW_TYPE_INLINE_ADD at position after all active items`() {
        val items = listOf(TodoItem("1", "Buy milk", "list-1"))
        adapter.submitList(items, emptyList())

        assertEquals(TodoListAdapter.VIEW_TYPE_INLINE_ADD, adapter.getItemViewType(1))
    }

    @Test
    fun `should return VIEW_TYPE_INLINE_ADD at position zero when no active items exist`() {
        adapter.submitList(emptyList(), emptyList())

        assertEquals(TodoListAdapter.VIEW_TYPE_INLINE_ADD, adapter.getItemViewType(0))
    }

    @Test
    fun `should create item view holder for VIEW_TYPE_ITEM`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM)
        assertEquals(TodoListAdapter.ItemViewHolder::class.java, holder.javaClass)
    }

    @Test
    fun `should create inline add view holder for VIEW_TYPE_INLINE_ADD`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_INLINE_ADD)
        assertEquals(TodoListAdapter.InlineAddViewHolder::class.java, holder.javaClass)
    }

    @Test
    fun `should not include divider row when only active items exist`() {
        val activeItems = listOf(TodoItem("1", "Buy milk", "list-1"))
        adapter.submitList(activeItems, emptyList())

        assertEquals(2, adapter.itemCount)
        assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
        assertEquals(TodoListAdapter.VIEW_TYPE_INLINE_ADD, adapter.getItemViewType(1))
    }

    @Test
    fun `should not include divider row when only completed items exist`() {
        val completedItems = listOf(TodoItem("1", "Buy milk", "list-1", isCompleted = true, completedAt = 1000L))
        adapter.submitList(emptyList(), completedItems)

        assertEquals(2, adapter.itemCount)
        assertEquals(TodoListAdapter.VIEW_TYPE_INLINE_ADD, adapter.getItemViewType(0))
        assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(1))
    }

    @Test
    fun `should include divider row between inline add and completed items when both sections are non-empty`() {
        val activeItems = listOf(TodoItem("1", "Buy milk", "list-1"))
        val completedItems = listOf(TodoItem("2", "Call dentist", "list-1", isCompleted = true, completedAt = 1000L))
        adapter.submitList(activeItems, completedItems)

        assertEquals(4, adapter.itemCount)
        assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
        assertEquals(TodoListAdapter.VIEW_TYPE_INLINE_ADD, adapter.getItemViewType(1))
        assertEquals(TodoListAdapter.VIEW_TYPE_DIVIDER, adapter.getItemViewType(2))
        assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(3))
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
        adapter.onBindViewHolder(holder, 2)

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
        adapter.onBindViewHolder(holder, 1)

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
        adapter.onBindViewHolder(holder, 1)

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

    @Test
    fun `should invoke onSubmitInlineAdd when inline add edit text has non-blank title and IME Done is triggered`() {
        val parent = FrameLayout(themedContext)
        var submitted = ""
        adapter.onSubmitInlineAdd = { title -> submitted = title }
        adapter.submitList(emptyList(), emptyList())
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_INLINE_ADD) as TodoListAdapter.InlineAddViewHolder
        adapter.onBindViewHolder(holder, 0)

        holder.editText.setText("Buy milk")
        holder.editText.onEditorAction(EditorInfo.IME_ACTION_DONE)

        assertEquals("Buy milk", submitted)
    }

    @Test
    fun `should not invoke onSubmitInlineAdd when inline add edit text has blank title`() {
        val parent = FrameLayout(themedContext)
        var called = false
        adapter.onSubmitInlineAdd = { called = true }
        adapter.submitList(emptyList(), emptyList())
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_INLINE_ADD) as TodoListAdapter.InlineAddViewHolder
        adapter.onBindViewHolder(holder, 0)

        holder.editText.setText("   ")
        holder.editText.onEditorAction(EditorInfo.IME_ACTION_DONE)

        assertFalse(called)
    }

    @Test
    fun `should always include inline add row in rows regardless of list state`() {
        adapter.submitList(emptyList(), emptyList())
        val emptyHasInlineAdd = (0 until adapter.itemCount).any {
            adapter.getItemViewType(it) == TodoListAdapter.VIEW_TYPE_INLINE_ADD
        }

        adapter.submitList(listOf(TodoItem("1", "A", "l")), emptyList())
        val activeHasInlineAdd = (0 until adapter.itemCount).any {
            adapter.getItemViewType(it) == TodoListAdapter.VIEW_TYPE_INLINE_ADD
        }

        assertTrue(emptyHasInlineAdd)
        assertTrue(activeHasInlineAdd)
    }
}
