package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InMemoryTodoListRepositoryEditTest {

    private lateinit var repository: InMemoryTodoListRepository

    @Before
    fun setUp() {
        repository = InMemoryTodoListRepository()
    }

    @Test
    fun `should update name when updateName is called with existing id`() {
        repository.add(TodoList("1", "Groceries"))
        repository.updateName("1", "Supermarket")
        assertEquals("Supermarket", repository.getAll().first().name)
    }

    @Test
    fun `should preserve id after name update`() {
        repository.add(TodoList("1", "Groceries"))
        repository.updateName("1", "Supermarket")
        assertEquals("1", repository.getAll().first().id)
    }

    @Test
    fun `should only update the targeted list when multiple lists exist`() {
        repository.add(TodoList("1", "Groceries"))
        repository.add(TodoList("2", "Work tasks"))
        repository.updateName("1", "Supermarket")
        assertEquals("Supermarket", repository.getAll()[0].name)
        assertEquals("Work tasks", repository.getAll()[1].name)
    }

    @Test
    fun `should do nothing when updateName is called with a non-existent id`() {
        repository.add(TodoList("1", "Groceries"))
        repository.updateName("nonexistent", "Supermarket")
        assertEquals("Groceries", repository.getAll().first().name)
    }

    @Test
    fun `should preserve insertion order after name update`() {
        repository.add(TodoList("1", "Groceries"))
        repository.add(TodoList("2", "Work tasks"))
        repository.updateName("1", "Supermarket")
        assertEquals(listOf(TodoList("1", "Supermarket"), TodoList("2", "Work tasks")), repository.getAll())
    }

    @Test
    fun `should update the second item when it is targeted by id`() {
        repository.add(TodoList("1", "Groceries"))
        repository.add(TodoList("2", "Work tasks"))
        repository.updateName("2", "Office")
        assertEquals("Groceries", repository.getAll()[0].name)
        assertEquals("Office", repository.getAll()[1].name)
    }
}
