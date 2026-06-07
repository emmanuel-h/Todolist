package fr.mandarine.todolist.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoItem
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
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
    private lateinit var onSubmit: (String) -> Unit
    private lateinit var adapter: TodoListAdapter
    private lateinit var themedContext: Context

    @Before
    fun setUp() {
        onToggle = mockk(relaxed = true)
        onSubmit = mockk(relaxed = true)
        adapter = TodoListAdapter(onToggle = onToggle, onSubmit = onSubmit)
        themedContext = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_ToDoList
        )
    }

    @Test
    fun `should return items size plus one when getItemCount is called with items`() {
        val items = listOf(
            TodoItem("1", "Buy milk", "list-1"),
            TodoItem("2", "Call dentist", "list-1")
        )
        adapter.submitList(items)

        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `should return one when getItemCount is called with empty list`() {
        adapter.submitList(emptyList())

        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `should return VIEW_TYPE_ITEM for item positions`() {
        val items = listOf(TodoItem("1", "Buy milk", "list-1"))
        adapter.submitList(items)

        assertEquals(TodoListAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
    }

    @Test
    fun `should return VIEW_TYPE_ADD for last position`() {
        val items = listOf(TodoItem("1", "Buy milk", "list-1"))
        adapter.submitList(items)

        assertEquals(TodoListAdapter.VIEW_TYPE_ADD, adapter.getItemViewType(1))
    }

    @Test
    fun `should return VIEW_TYPE_ADD at position zero when list is empty`() {
        adapter.submitList(emptyList())

        assertEquals(TodoListAdapter.VIEW_TYPE_ADD, adapter.getItemViewType(0))
    }

    @Test
    fun `should invoke onSubmit with title when non-blank text is submitted via inline input`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ADD)
        val editText = holder.itemView.findViewById<TextInputEditText>(R.id.editInlineAdd)

        editText.setText("Buy milk")
        editText.onEditorAction(EditorInfo.IME_ACTION_DONE)

        verify { onSubmit("Buy milk") }
    }

    @Test
    fun `should not invoke onSubmit when blank text is submitted via inline input`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ADD)
        val editText = holder.itemView.findViewById<TextInputEditText>(R.id.editInlineAdd)

        editText.setText("   ")
        editText.onEditorAction(EditorInfo.IME_ACTION_DONE)

        verify(exactly = 0) { onSubmit(any()) }
    }

    @Test
    fun `should not invoke onSubmit when empty text is submitted via inline input`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ADD)
        val editText = holder.itemView.findViewById<TextInputEditText>(R.id.editInlineAdd)

        editText.setText("")
        editText.onEditorAction(EditorInfo.IME_ACTION_DONE)

        verify(exactly = 0) { onSubmit(any()) }
    }

    @Test
    fun `should clear editText after valid submission via inline input`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ADD)
        val editText = holder.itemView.findViewById<TextInputEditText>(R.id.editInlineAdd)

        editText.setText("Buy milk")
        editText.onEditorAction(EditorInfo.IME_ACTION_DONE)

        assertEquals("", editText.text.toString())
    }

    @Test
    fun `should return false for non-done IME action on inline input`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ADD)
        val editText = holder.itemView.findViewById<TextInputEditText>(R.id.editInlineAdd)

        editText.setText("Buy milk")
        val handled = editText.onEditorAction(EditorInfo.IME_ACTION_GO)

        verify(exactly = 0) { onSubmit(any()) }
    }

    @Test
    fun `should create item view holder for VIEW_TYPE_ITEM`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ITEM)
        assertEquals(TodoListAdapter.ItemViewHolder::class.java, holder.javaClass)
    }

    @Test
    fun `should create add view holder for VIEW_TYPE_ADD`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ADD)
        assertEquals(TodoListAdapter.AddInputViewHolder::class.java, holder.javaClass)
    }

    @Test
    fun `should invoke onSubmit when submit button is clicked with non-blank text`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ADD)
        val editText = holder.itemView.findViewById<TextInputEditText>(R.id.editInlineAdd)
        val submitButton = holder.itemView.findViewById<MaterialButton>(R.id.btnInlineSubmit)

        editText.setText("Buy milk")
        submitButton.performClick()

        verify { onSubmit("Buy milk") }
    }

    @Test
    fun `should not invoke onSubmit when submit button is clicked with blank text`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ADD)
        val editText = holder.itemView.findViewById<TextInputEditText>(R.id.editInlineAdd)
        val submitButton = holder.itemView.findViewById<MaterialButton>(R.id.btnInlineSubmit)

        editText.setText("   ")
        submitButton.performClick()

        verify(exactly = 0) { onSubmit(any()) }
    }

    @Test
    fun `should not invoke onSubmit when submit button is clicked with empty text`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ADD)
        val editText = holder.itemView.findViewById<TextInputEditText>(R.id.editInlineAdd)
        val submitButton = holder.itemView.findViewById<MaterialButton>(R.id.btnInlineSubmit)

        editText.setText("")
        submitButton.performClick()

        verify(exactly = 0) { onSubmit(any()) }
    }

    @Test
    fun `should clear editText after valid submission via submit button`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListAdapter.VIEW_TYPE_ADD)
        val editText = holder.itemView.findViewById<TextInputEditText>(R.id.editInlineAdd)
        val submitButton = holder.itemView.findViewById<MaterialButton>(R.id.btnInlineSubmit)

        editText.setText("Buy milk")
        submitButton.performClick()

        assertEquals("", editText.text.toString())
    }
}
