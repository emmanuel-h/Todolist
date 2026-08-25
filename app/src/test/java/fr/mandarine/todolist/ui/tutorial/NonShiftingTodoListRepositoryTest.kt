package fr.mandarine.todolist.ui.tutorial

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListRepository
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class NonShiftingTodoListRepositoryTest {

    private val page = FakePage()
    private val repository = NonShiftingTodoListRepository(page)

    @Test
    fun `should write the first list on the first rule when the page is bare`() {
        repository.addAtTop(TodoList(id = "demo", name = "Groceries"))

        assertEquals(0, page.lists.single().position)
    }

    @Test
    fun `should write the list one rule above the highest row already on the page`() {
        page.lists += TodoList(id = "a", name = "Travail", position = 2)
        page.lists += TodoList(id = "b", name = "Voyage", position = 5)

        repository.addAtTop(TodoList(id = "demo", name = "Groceries"))

        assertEquals(1, page.lists.first { it.id == "demo" }.position)
    }

    @Test
    fun `should find the highest row wherever it is written on the page`() {
        page.lists += TodoList(id = "a", name = "Travail", position = 5)
        page.lists += TodoList(id = "b", name = "Voyage", position = 2)

        repository.addAtTop(TodoList(id = "demo", name = "Groceries"))

        assertEquals(1, page.lists.first { it.id == "demo" }.position)
    }

    @Test
    fun `should leave every row already on the page exactly where it was`() {
        page.lists += TodoList(id = "a", name = "Travail", position = 2)
        page.lists += TodoList(id = "b", name = "Voyage", position = 5)

        repository.addAtTop(TodoList(id = "demo", name = "Groceries"))

        assertEquals(2, page.lists.first { it.id == "a" }.position)
        assertEquals(5, page.lists.first { it.id == "b" }.position)
    }

    @Test
    fun `should write the list it was handed rather than one of its own`() {
        repository.addAtTop(
            TodoList(id = "demo", name = "Groceries", dueDate = DUE_DATE)
        )

        val written = page.lists.single()
        assertEquals("demo", written.id)
        assertEquals("Groceries", written.name)
        assertEquals(DUE_DATE, written.dueDate)
    }

    @Test
    fun `should never renumber the page to make room`() {
        page.lists += TodoList(id = "a", name = "Travail", position = 2)

        repository.addAtTop(TodoList(id = "demo", name = "Groceries"))

        assertEquals(0, page.renumberings)
    }

    @Test
    fun `should hand back every row the page holds`() {
        page.lists += TodoList(id = "a", name = "Travail", position = 2)

        assertEquals(page.lists.toList(), repository.getAll())
    }

    @Test
    fun `should write a plain list straight onto the page`() {
        repository.add(TodoList(id = "a", name = "Travail", position = 7))

        assertEquals(7, page.lists.single().position)
    }

    @Test
    fun `should tear a list off the page`() {
        page.lists += TodoList(id = "a", name = "Travail", position = 2)

        repository.delete("a")

        assertEquals(emptyList<TodoList>(), page.lists)
    }

    @Test
    fun `should rewrite a list on the page`() {
        page.lists += TodoList(id = "a", name = "Travail", position = 2)

        repository.update("a", "Bricolage", null, DUE_DATE)

        val written = page.lists.single()
        assertEquals("Bricolage", written.name)
        assertEquals(DUE_DATE, written.dueDate)
    }

    @Test
    fun `should reorder the page when the reader asks it to`() {
        page.lists += TodoList(id = "a", name = "Travail", position = 0)
        page.lists += TodoList(id = "b", name = "Voyage", position = 1)

        repository.reorder(1, 0)

        assertEquals(listOf("b", "a"), page.lists.map { it.id })
        assertEquals(1, page.renumberings)
    }

    private class FakePage : TodoListRepository {

        val lists = mutableListOf<TodoList>()
        var renumberings = 0

        override fun getAll(): List<TodoList> = lists

        override fun add(todoList: TodoList) {
            lists += todoList
        }

        override fun addAtTop(todoList: TodoList) {
            renumberings += 1
            lists.replaceAll { it.copy(position = it.position + 1) }
            lists.add(0, todoList)
        }

        override fun delete(todoListId: String) {
            lists.removeAll { it.id == todoListId }
        }

        override fun update(
            todoListId: String,
            name: String,
            targetDate: LocalDate?,
            dueDate: LocalDate?
        ) {
            val index = lists.indexOfFirst { it.id == todoListId }
            lists[index] = lists[index].copy(name = name, targetDate = targetDate, dueDate = dueDate)
        }

        override fun reorder(fromIndex: Int, toIndex: Int) {
            renumberings += 1
            val moved = lists.removeAt(fromIndex)
            lists.add(toIndex, moved)
        }
    }

    private companion object {
        val DUE_DATE: LocalDate = LocalDate.of(2026, 3, 14)
    }
}
