package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InMemoryTodoListRepositoryReorderTest {

    private lateinit var repository: InMemoryTodoListRepository

    @Before
    fun setUp() {
        repository = InMemoryTodoListRepository()
    }

    @Test
    fun `should move list from first to last position when reorder is called on three lists`() {
        repository.add(TodoList("1", "First", position = 0))
        repository.add(TodoList("2", "Second", position = 1))
        repository.add(TodoList("3", "Third", position = 2))

        repository.reorder(0, 2)

        val lists = repository.getAll()
        assertEquals(listOf("Second", "Third", "First"), lists.map { it.name })
    }

    @Test
    fun `should move list from last to first position when reorder moves upward`() {
        repository.add(TodoList("1", "First", position = 0))
        repository.add(TodoList("2", "Second", position = 1))
        repository.add(TodoList("3", "Third", position = 2))

        repository.reorder(2, 0)

        val lists = repository.getAll()
        assertEquals(listOf("Third", "First", "Second"), lists.map { it.name })
    }

    @Test
    fun `should move list one position down when adjacent reorder is called`() {
        repository.add(TodoList("1", "First", position = 0))
        repository.add(TodoList("2", "Second", position = 1))
        repository.add(TodoList("3", "Third", position = 2))

        repository.reorder(0, 1)

        val lists = repository.getAll()
        assertEquals(listOf("Second", "First", "Third"), lists.map { it.name })
    }

    @Test
    fun `should not change order when fromIndex equals toIndex`() {
        repository.add(TodoList("1", "First", position = 0))
        repository.add(TodoList("2", "Second", position = 1))

        repository.reorder(1, 1)

        val lists = repository.getAll()
        assertEquals(listOf("First", "Second"), lists.map { it.name })
    }

    @Test
    fun `should do nothing when reorder is called and repository is empty`() {
        repository.reorder(0, 1)
        assertEquals(emptyList<TodoList>(), repository.getAll())
    }

    @Test
    fun `should persist updated positions so getAll returns lists in new order`() {
        repository.add(TodoList("1", "First", position = 0))
        repository.add(TodoList("2", "Second", position = 1))
        repository.add(TodoList("3", "Third", position = 2))

        repository.reorder(0, 2)

        val lists = repository.getAll()
        assertEquals(0, lists.first { it.name == "Second" }.position)
        assertEquals(1, lists.first { it.name == "Third" }.position)
        assertEquals(2, lists.first { it.name == "First" }.position)
    }

    @Test
    fun `should sort by position field before reordering when lists are added out of position order`() {
        repository.add(TodoList("1", "Third", position = 2))
        repository.add(TodoList("2", "First", position = 0))
        repository.add(TodoList("3", "Second", position = 1))

        repository.reorder(0, 2)

        val lists = repository.getAll()
        assertEquals(listOf("Second", "Third", "First"), lists.map { it.name })
    }

    @Test
    fun `should assign positions starting at zero after reorder`() {
        repository.add(TodoList("1", "First", position = 0))
        repository.add(TodoList("2", "Second", position = 1))
        repository.add(TodoList("3", "Third", position = 2))

        repository.reorder(2, 0)

        val lists = repository.getAll()
        assertEquals(listOf(0, 1, 2), lists.map { it.position })
    }

    @Test
    fun `should return lists sorted by position when they are added out of order`() {
        repository.add(TodoList("1", "Third", position = 2))
        repository.add(TodoList("2", "First", position = 0))
        repository.add(TodoList("3", "Second", position = 1))

        val lists = repository.getAll()
        assertEquals(listOf("First", "Second", "Third"), lists.map { it.name })
    }

    @Test
    fun `should preserve positions zero-based after reorder moving forward`() {
        repository.add(TodoList("1", "First", position = 0))
        repository.add(TodoList("2", "Second", position = 1))
        repository.add(TodoList("3", "Third", position = 2))

        repository.reorder(0, 2)

        val lists = repository.getAll()
        assertEquals(listOf(0, 1, 2), lists.map { it.position })
    }
}
