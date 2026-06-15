package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InMemoryTodoRepositoryReorderTest {

    private lateinit var repository: InMemoryTodoRepository

    @Before
    fun setUp() {
        repository = InMemoryTodoRepository()
    }

    @Test
    fun `should move item from first to last position when reorder is called on three active items`() {
        repository.add(TodoItem("1", "First", "list-1", position = 0))
        repository.add(TodoItem("2", "Second", "list-1", position = 1))
        repository.add(TodoItem("3", "Third", "list-1", position = 2))

        repository.reorder("list-1", 0, 2)

        val items = repository.getAllByListId("list-1")
        assertEquals(listOf("Second", "Third", "First"), items.map { it.title })
    }

    @Test
    fun `should move item from last to first position when reorder moves upward`() {
        repository.add(TodoItem("1", "First", "list-1", position = 0))
        repository.add(TodoItem("2", "Second", "list-1", position = 1))
        repository.add(TodoItem("3", "Third", "list-1", position = 2))

        repository.reorder("list-1", 2, 0)

        val items = repository.getAllByListId("list-1")
        assertEquals(listOf("Third", "First", "Second"), items.map { it.title })
    }

    @Test
    fun `should move item one position down when adjacent reorder is called`() {
        repository.add(TodoItem("1", "First", "list-1", position = 0))
        repository.add(TodoItem("2", "Second", "list-1", position = 1))
        repository.add(TodoItem("3", "Third", "list-1", position = 2))

        repository.reorder("list-1", 0, 1)

        val items = repository.getAllByListId("list-1")
        assertEquals(listOf("Second", "First", "Third"), items.map { it.title })
    }

    @Test
    fun `should not change order when fromIndex equals toIndex`() {
        repository.add(TodoItem("1", "First", "list-1", position = 0))
        repository.add(TodoItem("2", "Second", "list-1", position = 1))

        repository.reorder("list-1", 1, 1)

        val items = repository.getAllByListId("list-1")
        assertEquals(listOf("First", "Second"), items.map { it.title })
    }

    @Test
    fun `should only reorder active items and leave completed items unaffected`() {
        repository.add(TodoItem("1", "Active A", "list-1", position = 0))
        repository.add(TodoItem("2", "Active B", "list-1", position = 1))
        repository.add(TodoItem("3", "Done", "list-1", isCompleted = true, completedAt = 1000L, position = 0))

        repository.reorder("list-1", 0, 1)

        val items = repository.getAllByListId("list-1")
        val active = items.filter { !it.isCompleted }
        val completed = items.filter { it.isCompleted }
        assertEquals(listOf("Active B", "Active A"), active.map { it.title })
        assertEquals(listOf("Done"), completed.map { it.title })
    }

    @Test
    fun `should persist updated positions so getAllByListId returns items in new order`() {
        repository.add(TodoItem("1", "First", "list-1", position = 0))
        repository.add(TodoItem("2", "Second", "list-1", position = 1))
        repository.add(TodoItem("3", "Third", "list-1", position = 2))

        repository.reorder("list-1", 0, 2)

        val items = repository.getAllByListId("list-1")
        assertEquals(0, items.first { it.title == "Second" }.position)
        assertEquals(1, items.first { it.title == "Third" }.position)
        assertEquals(2, items.first { it.title == "First" }.position)
    }

    @Test
    fun `should do nothing when reorder is called for a list with no items`() {
        repository.reorder("list-empty", 0, 1)
        assertEquals(emptyList<TodoItem>(), repository.getAllByListId("list-empty"))
    }

    @Test
    fun `should sort by position field before reordering when items are added out of position order`() {
        repository.add(TodoItem("1", "Third", "list-1", position = 2))
        repository.add(TodoItem("2", "First", "list-1", position = 0))
        repository.add(TodoItem("3", "Second", "list-1", position = 1))

        repository.reorder("list-1", 0, 2)

        val items = repository.getAllByListId("list-1")
        assertEquals(listOf("Second", "Third", "First"), items.map { it.title })
    }

    @Test
    fun `should not affect items of other lists when reorder is called`() {
        repository.add(TodoItem("1", "List1 A", "list-1", position = 0))
        repository.add(TodoItem("2", "List1 B", "list-1", position = 1))
        repository.add(TodoItem("3", "List2 A", "list-2", position = 0))

        repository.reorder("list-1", 0, 1)

        val list2Items = repository.getAllByListId("list-2")
        assertEquals(listOf("List2 A"), list2Items.map { it.title })
        assertEquals(0, list2Items.first().position)
    }

    @Test
    fun `should assign positions starting at zero after reorder`() {
        repository.add(TodoItem("1", "First", "list-1", position = 0))
        repository.add(TodoItem("2", "Second", "list-1", position = 1))
        repository.add(TodoItem("3", "Third", "list-1", position = 2))

        repository.reorder("list-1", 2, 0)

        val items = repository.getAllByListId("list-1")
        val active = items.filter { !it.isCompleted }
        assertEquals(listOf(0, 1, 2), active.map { it.position })
    }
}
