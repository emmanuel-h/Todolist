package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.TodoList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryTodoListRepositoryTest {

    private lateinit var repository: InMemoryTodoListRepository

    @Before
    fun setUp() {
        repository = InMemoryTodoListRepository()
    }

    @Test
    fun `should return empty list when no lists have been added`() {
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `should return added list when one list is added`() {
        val todoList = TodoList("1", "Groceries")
        repository.add(todoList)
        assertEquals(listOf(todoList), repository.getAll())
    }

    @Test
    fun `should return all lists when multiple lists are added`() {
        val list1 = TodoList("1", "Groceries")
        val list2 = TodoList("2", "Home")
        repository.add(list1)
        repository.add(list2)
        assertEquals(listOf(list1, list2), repository.getAll())
    }

    @Test
    fun `should return snapshot independent of future modifications`() {
        val list1 = TodoList("1", "Groceries")
        repository.add(list1)
        val snapshot = repository.getAll()
        repository.add(TodoList("2", "Home"))
        assertEquals(1, snapshot.size)
    }

    @Test
    fun `should remove list when delete is called with its id`() {
        val todoList = TodoList("1", "Groceries")
        repository.add(todoList)
        repository.delete("1")
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `should remove only the specified list when delete is called`() {
        repository.add(TodoList("1", "Groceries"))
        repository.add(TodoList("2", "Home"))
        repository.delete("1")
        assertEquals(listOf(TodoList("2", "Home")), repository.getAll())
    }

    @Test
    fun `should do nothing when delete is called with a non-existent id`() {
        repository.add(TodoList("1", "Groceries"))
        repository.delete("nonexistent")
        assertEquals(1, repository.getAll().size)
    }
}
