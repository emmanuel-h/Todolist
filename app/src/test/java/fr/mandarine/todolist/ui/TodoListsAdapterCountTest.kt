package fr.mandarine.todolist.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoListsAdapterCountTest {

    private lateinit var adapter: TodoListsAdapter
    private lateinit var themedContext: Context

    @Before
    fun setUp() {
        adapter = TodoListsAdapter(
            onListClick = mockk(relaxed = true),
            onDeleteClick = mockk(relaxed = true),
            onRenameClick = mockk(relaxed = true),
            onDragStart = mockk(relaxed = true)
        )
        themedContext = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_ToDoList
        )
    }

    private fun createItemHolder(): TodoListsAdapter.ViewHolder {
        val parent = FrameLayout(themedContext)
        return adapter.onCreateViewHolder(parent, TodoListsAdapter.VIEW_TYPE_ITEM) as TodoListsAdapter.ViewHolder
    }

    private fun bindAt(position: Int): TodoListsAdapter.ViewHolder {
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, position)
        return holder
    }

    @Test
    fun `should display zero in active badge when list has no items`() {
        val summary = TodoListSummary(TodoList("1", "Empty"), allDone = false, activeCount = 0, completedCount = 0)
        adapter.submitList(listOf(summary), emptyList())

        val activeBadge = bindAt(0).itemView.findViewById<MaterialTextView>(R.id.badgeActiveCount)

        assertEquals("0", activeBadge.text.toString())
    }

    @Test
    fun `should display zero in completed badge when list has no items`() {
        val summary = TodoListSummary(TodoList("1", "Empty"), allDone = false, activeCount = 0, completedCount = 0)
        adapter.submitList(listOf(summary), emptyList())

        val completedBadge = bindAt(0).itemView.findViewById<MaterialTextView>(R.id.badgeCompletedCount)

        assertEquals("0", completedBadge.text.toString())
    }

    @Test
    fun `should display active count in active badge when list has active items only`() {
        val summary = TodoListSummary(TodoList("1", "Groceries"), allDone = false, activeCount = 3, completedCount = 0)
        adapter.submitList(listOf(summary), emptyList())

        val activeBadge = bindAt(0).itemView.findViewById<MaterialTextView>(R.id.badgeActiveCount)

        assertEquals("3", activeBadge.text.toString())
    }

    @Test
    fun `should display zero in completed badge when list has active items only`() {
        val summary = TodoListSummary(TodoList("1", "Groceries"), allDone = false, activeCount = 3, completedCount = 0)
        adapter.submitList(listOf(summary), emptyList())

        val completedBadge = bindAt(0).itemView.findViewById<MaterialTextView>(R.id.badgeCompletedCount)

        assertEquals("0", completedBadge.text.toString())
    }

    @Test
    fun `should display zero in active badge when list has completed items only`() {
        val summary = TodoListSummary(TodoList("1", "Work"), allDone = true, activeCount = 0, completedCount = 2)
        adapter.submitList(emptyList(), listOf(summary))

        val activeBadge = bindAt(0).itemView.findViewById<MaterialTextView>(R.id.badgeActiveCount)

        assertEquals("0", activeBadge.text.toString())
    }

    @Test
    fun `should display completed count in completed badge when list has completed items only`() {
        val summary = TodoListSummary(TodoList("1", "Work"), allDone = true, activeCount = 0, completedCount = 2)
        adapter.submitList(emptyList(), listOf(summary))

        val completedBadge = bindAt(0).itemView.findViewById<MaterialTextView>(R.id.badgeCompletedCount)

        assertEquals("2", completedBadge.text.toString())
    }

    @Test
    fun `should display active count in active badge when list has both active and completed items`() {
        val summary = TodoListSummary(TodoList("1", "Tasks"), allDone = false, activeCount = 3, completedCount = 2)
        adapter.submitList(listOf(summary), emptyList())

        val activeBadge = bindAt(0).itemView.findViewById<MaterialTextView>(R.id.badgeActiveCount)

        assertEquals("3", activeBadge.text.toString())
    }

    @Test
    fun `should display completed count in completed badge when list has both active and completed items`() {
        val summary = TodoListSummary(TodoList("1", "Tasks"), allDone = false, activeCount = 3, completedCount = 2)
        adapter.submitList(listOf(summary), emptyList())

        val completedBadge = bindAt(0).itemView.findViewById<MaterialTextView>(R.id.badgeCompletedCount)

        assertEquals("2", completedBadge.text.toString())
    }

    @Test
    fun `should display one in active badge for list with single active item`() {
        val summary = TodoListSummary(TodoList("1", "Solo"), allDone = false, activeCount = 1, completedCount = 0)
        adapter.submitList(listOf(summary), emptyList())

        val activeBadge = bindAt(0).itemView.findViewById<MaterialTextView>(R.id.badgeActiveCount)

        assertEquals("1", activeBadge.text.toString())
    }

    @Test
    fun `should display one in completed badge for list with single completed item`() {
        val summary = TodoListSummary(TodoList("1", "Solo"), allDone = true, activeCount = 0, completedCount = 1)
        adapter.submitList(emptyList(), listOf(summary))

        val completedBadge = bindAt(0).itemView.findViewById<MaterialTextView>(R.id.badgeCompletedCount)

        assertEquals("1", completedBadge.text.toString())
    }
}
