package fr.mandarine.todolist.ui

import android.content.Context
import android.graphics.Paint
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.textview.MaterialTextView
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListSummary
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
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
class TodoListsAdapterTest {

    private lateinit var onListClick: (TodoList) -> Unit
    private lateinit var onDeleteConfirmed: (TodoList) -> Unit
    private lateinit var onRenameClick: (TodoList) -> Unit
    private lateinit var onDragStart: (androidx.recyclerview.widget.RecyclerView.ViewHolder) -> Unit
    private lateinit var adapter: TodoListsAdapter
    private lateinit var themedContext: Context

    @Before
    fun setUp() {
        onListClick = mockk(relaxed = true)
        onDeleteConfirmed = mockk(relaxed = true)
        onRenameClick = mockk(relaxed = true)
        onDragStart = mockk(relaxed = true)
        adapter = TodoListsAdapter(
            onListClick = onListClick,
            onDeleteConfirmed = onDeleteConfirmed,
            onRenameClick = onRenameClick,
            onDragStart = onDragStart
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

    private fun createDividerHolder(): TodoListsAdapter.DividerViewHolder {
        val parent = FrameLayout(themedContext)
        return adapter.onCreateViewHolder(parent, TodoListsAdapter.VIEW_TYPE_DIVIDER) as TodoListsAdapter.DividerViewHolder
    }

    private fun activeSummary(id: String, name: String) =
        TodoListSummary(TodoList(id, name), allDone = false)

    private fun doneSummary(id: String, name: String) =
        TodoListSummary(TodoList(id, name), allDone = true)

    // --- item count ---

    @Test
    fun `should return zero item count when no summaries submitted`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `should return correct item count when only active summaries are submitted`() {
        adapter.submitList(
            listOf(activeSummary("1", "A"), activeSummary("2", "B")),
            emptyList()
        )
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `should return correct item count when only done summaries are submitted`() {
        adapter.submitList(
            emptyList(),
            listOf(doneSummary("1", "A"), doneSummary("2", "B"))
        )
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `should include divider row in item count when both active and done are non-empty`() {
        adapter.submitList(
            listOf(activeSummary("1", "A")),
            listOf(doneSummary("2", "B"))
        )
        assertEquals(3, adapter.itemCount)
    }

    // --- buildRows ---

    @Test
    fun `should not insert divider when only active summaries exist`() {
        adapter.submitList(listOf(activeSummary("1", "A")), emptyList())

        assertEquals(1, adapter.itemCount)
        assertEquals(TodoListsAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
    }

    @Test
    fun `should not insert divider when only done summaries exist`() {
        adapter.submitList(emptyList(), listOf(doneSummary("1", "A")))

        assertEquals(1, adapter.itemCount)
        assertEquals(TodoListsAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
    }

    @Test
    fun `should insert divider between active and done rows when both are non-empty`() {
        adapter.submitList(
            listOf(activeSummary("1", "Active")),
            listOf(doneSummary("2", "Done"))
        )

        assertEquals(TodoListsAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
        assertEquals(TodoListsAdapter.VIEW_TYPE_DIVIDER, adapter.getItemViewType(1))
        assertEquals(TodoListsAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(2))
    }

    @Test
    fun `should set doneCount correctly on divider row`() {
        adapter.submitList(
            listOf(activeSummary("1", "Active")),
            listOf(doneSummary("2", "DoneA"), doneSummary("3", "DoneB"))
        )

        val holder = createDividerHolder()
        adapter.onBindViewHolder(holder, 1)

        val label = holder.itemView.findViewById<MaterialTextView>(R.id.textDividerLabel)
        assertEquals("2", label.text.toString())
    }

    @Test
    fun `should place active rows before divider and done rows after`() {
        adapter.submitList(
            listOf(activeSummary("1", "Active")),
            listOf(doneSummary("2", "Done"))
        )

        val activeHolder = createItemHolder()
        adapter.onBindViewHolder(activeHolder, 0)
        val activeName = activeHolder.itemView.findViewById<MaterialTextView>(R.id.textListName).text.toString()
        assertEquals("Active", activeName)

        val doneHolder = createItemHolder()
        adapter.onBindViewHolder(doneHolder, 2)
        val doneName = doneHolder.itemView.findViewById<MaterialTextView>(R.id.textListName).text.toString()
        assertEquals("Done", doneName)
    }

    // --- getItemViewType ---

    @Test
    fun `should return VIEW_TYPE_ITEM for active item positions`() {
        adapter.submitList(listOf(activeSummary("1", "A")), emptyList())
        assertEquals(TodoListsAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(0))
    }

    @Test
    fun `should return VIEW_TYPE_DIVIDER for divider position in mixed list`() {
        adapter.submitList(
            listOf(activeSummary("1", "A")),
            listOf(doneSummary("2", "B"))
        )
        assertEquals(TodoListsAdapter.VIEW_TYPE_DIVIDER, adapter.getItemViewType(1))
    }

    @Test
    fun `should return VIEW_TYPE_ITEM for done item position after divider`() {
        adapter.submitList(
            listOf(activeSummary("1", "A")),
            listOf(doneSummary("2", "B"))
        )
        assertEquals(TodoListsAdapter.VIEW_TYPE_ITEM, adapter.getItemViewType(2))
    }

    // --- activeItemCount ---

    @Test
    fun `should return zero activeItemCount when adapter is empty`() {
        assertEquals(0, adapter.activeItemCount())
    }

    @Test
    fun `should return active count only when mixed list is submitted`() {
        adapter.submitList(
            listOf(activeSummary("1", "A"), activeSummary("2", "B")),
            listOf(doneSummary("3", "C"))
        )
        assertEquals(2, adapter.activeItemCount())
    }

    @Test
    fun `should return zero activeItemCount when only done summaries are submitted`() {
        adapter.submitList(emptyList(), listOf(doneSummary("1", "A")))
        assertEquals(0, adapter.activeItemCount())
    }

    // --- moveItem ---

    @Test
    fun `should update item count after moveItem`() {
        adapter.submitList(
            listOf(activeSummary("1", "A"), activeSummary("2", "B")),
            emptyList()
        )
        adapter.moveItem(0, 1)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `should move item to new position after moveItem`() {
        adapter.submitList(
            listOf(activeSummary("1", "Alpha"), activeSummary("2", "Beta")),
            emptyList()
        )
        adapter.moveItem(0, 1)

        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)
        val nameAt0 = holder.itemView.findViewById<MaterialTextView>(R.id.textListName).text.toString()
        assertEquals("Beta", nameAt0)
    }

    // --- bind ---

    @Test
    fun `should bind list name to nameView`() {
        adapter.submitList(listOf(activeSummary("1", "Groceries")), emptyList())
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        val nameView = holder.itemView.findViewById<MaterialTextView>(R.id.textListName)
        assertEquals("Groceries", nameView.text.toString())
    }

    @Test
    fun `should invoke onListClick with correct TodoList when item view is clicked`() {
        val list = TodoList("list-1", "Groceries")
        adapter.submitList(listOf(TodoListSummary(list, allDone = false)), emptyList())
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.performClick()

        verify { onListClick(list) }
    }

    @Test
    fun `should not invoke onDeleteConfirmed when delete button is clicked once`() {
        val list = TodoList("list-1", "Groceries")
        adapter.submitList(listOf(TodoListSummary(list, allDone = false)), emptyList())
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<View>(R.id.btnDeleteList).performClick()

        verify(exactly = 0) { onDeleteConfirmed(list) }
    }

    @Test
    fun `should show confirm strip and hide row content when delete button is clicked`() {
        val list = TodoList("list-1", "Groceries")
        adapter.submitList(listOf(TodoListSummary(list, allDone = false)), emptyList())
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<View>(R.id.btnDeleteList).performClick()
        adapter.onBindViewHolder(holder, 0)

        assertEquals(View.VISIBLE, holder.itemView.findViewById<View>(R.id.layoutDeleteConfirm).visibility)
        assertEquals(View.INVISIBLE, holder.itemView.findViewById<View>(R.id.layoutListRowContent).visibility)
    }

    @Test
    fun `should show list name in delete confirm strip when armed`() {
        val list = TodoList("list-1", "Groceries")
        adapter.submitList(listOf(TodoListSummary(list, allDone = false)), emptyList())
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<View>(R.id.btnDeleteList).performClick()
        adapter.onBindViewHolder(holder, 0)

        val nameInStrip = holder.itemView.findViewById<MaterialTextView>(R.id.textDeleteConfirmName)
        assertEquals("Groceries", nameInStrip.text.toString())
    }

    @Test
    fun `should invoke onDeleteConfirmed with correct TodoList when confirm button is clicked after arming`() {
        val list = TodoList("list-1", "Groceries")
        adapter.submitList(listOf(TodoListSummary(list, allDone = false)), emptyList())
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<View>(R.id.btnDeleteList).performClick()
        holder.itemView.findViewById<View>(R.id.btnDeleteConfirm).performClick()

        verify { onDeleteConfirmed(list) }
    }

    @Test
    fun `should hide confirm strip and keep list when cancel button is clicked after arming`() {
        val list = TodoList("list-1", "Groceries")
        adapter.submitList(listOf(TodoListSummary(list, allDone = false)), emptyList())
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<View>(R.id.btnDeleteList).performClick()
        holder.itemView.findViewById<View>(R.id.btnDeleteCancel).performClick()
        adapter.onBindViewHolder(holder, 0)

        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.layoutDeleteConfirm).visibility)
        assertEquals(View.VISIBLE, holder.itemView.findViewById<View>(R.id.layoutListRowContent).visibility)
        verify(exactly = 0) { onDeleteConfirmed(list) }
    }

    @Test
    fun `should hide confirm strip when strip background is clicked after arming`() {
        val list = TodoList("list-1", "Groceries")
        adapter.submitList(listOf(TodoListSummary(list, allDone = false)), emptyList())
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<View>(R.id.btnDeleteList).performClick()
        holder.itemView.findViewById<View>(R.id.layoutDeleteConfirm).performClick()
        adapter.onBindViewHolder(holder, 0)

        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.layoutDeleteConfirm).visibility)
        verify(exactly = 0) { onDeleteConfirmed(list) }
    }

    @Test
    fun `should disarm first row when delete button on another row is clicked`() {
        val first = TodoList("list-1", "Groceries")
        val second = TodoList("list-2", "Work")
        adapter.submitList(
            listOf(TodoListSummary(first, allDone = false), TodoListSummary(second, allDone = false)),
            emptyList()
        )
        val firstHolder = createItemHolder()
        adapter.onBindViewHolder(firstHolder, 0)
        val secondHolder = createItemHolder()
        adapter.onBindViewHolder(secondHolder, 1)

        firstHolder.itemView.findViewById<View>(R.id.btnDeleteList).performClick()
        secondHolder.itemView.findViewById<View>(R.id.btnDeleteList).performClick()
        adapter.onBindViewHolder(firstHolder, 0)
        adapter.onBindViewHolder(secondHolder, 1)

        assertEquals(View.GONE, firstHolder.itemView.findViewById<View>(R.id.layoutDeleteConfirm).visibility)
        assertEquals(View.VISIBLE, secondHolder.itemView.findViewById<View>(R.id.layoutDeleteConfirm).visibility)
    }

    @Test
    fun `should do nothing when cancelDeleteConfirm is called while no row is armed`() {
        adapter.submitList(listOf(activeSummary("1", "Groceries")), emptyList())

        adapter.cancelDeleteConfirm()

        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.layoutDeleteConfirm).visibility)
    }

    @Test
    fun `should invoke onRenameClick with correct TodoList when edit button is clicked`() {
        val list = TodoList("list-1", "Groceries")
        adapter.submitList(listOf(TodoListSummary(list, allDone = false)), emptyList())
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.findViewById<View>(R.id.btnEditList).performClick()

        verify { onRenameClick(list) }
    }

    @Test
    fun `should apply strikethrough on name when allDone is true`() {
        adapter.submitList(emptyList(), listOf(doneSummary("1", "Groceries")))
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        val nameView = holder.itemView.findViewById<MaterialTextView>(R.id.textListName)
        assertTrue(nameView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG != 0)
    }

    @Test
    fun `should clear strikethrough on name when allDone is false`() {
        adapter.submitList(listOf(activeSummary("1", "Groceries")), emptyList())
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        val nameView = holder.itemView.findViewById<MaterialTextView>(R.id.textListName)
        assertTrue(nameView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG == 0)
    }

    @Test
    fun `should apply 0_5f alpha on name when allDone is true`() {
        adapter.submitList(emptyList(), listOf(doneSummary("1", "Groceries")))
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        val nameView = holder.itemView.findViewById<MaterialTextView>(R.id.textListName)
        assertEquals(0.5f, nameView.alpha)
    }

    @Test
    fun `should apply 1_0f alpha on name when allDone is false`() {
        adapter.submitList(listOf(activeSummary("1", "Groceries")), emptyList())
        val holder = createItemHolder()
        adapter.onBindViewHolder(holder, 0)

        val nameView = holder.itemView.findViewById<MaterialTextView>(R.id.textListName)
        assertEquals(1.0f, nameView.alpha)
    }



    @Test
    fun `should expose drag handle view`() {
        val holder = createItemHolder()
        assertNotNull(holder.dragHandle)
    }

    // --- view holder creation ---

    @Test
    fun `should create item view holder for VIEW_TYPE_ITEM`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListsAdapter.VIEW_TYPE_ITEM)
        assertEquals(TodoListsAdapter.ViewHolder::class.java, holder.javaClass)
    }

    @Test
    fun `should create divider view holder for VIEW_TYPE_DIVIDER`() {
        val parent = FrameLayout(themedContext)
        val holder = adapter.onCreateViewHolder(parent, TodoListsAdapter.VIEW_TYPE_DIVIDER)
        assertEquals(TodoListsAdapter.DividerViewHolder::class.java, holder.javaClass)
    }

}
