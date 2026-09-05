package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.domain.TodoList
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RoomTodoListRepositoryColourTest {

    private lateinit var dao: TodoListDao
    private lateinit var repository: RoomTodoListRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = RoomTodoListRepository(dao)
    }

    @Test
    fun `should map entity Mint colour to domain ListColour Mint when reading`() {
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries", colour = "Mint"))

        val result = repository.getAll()

        assertEquals(ListColour.Mint, result[0].colour)
    }

    @Test
    fun `should map entity Butter colour to domain ListColour Butter when reading`() {
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries", colour = "Butter"))

        val result = repository.getAll()

        assertEquals(ListColour.Butter, result[0].colour)
    }

    @Test
    fun `should map entity Rose colour to domain ListColour Rose when reading`() {
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries", colour = "Rose"))

        val result = repository.getAll()

        assertEquals(ListColour.Rose, result[0].colour)
    }

    @Test
    fun `should map entity Sky colour to domain ListColour Sky when reading`() {
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries", colour = "Sky"))

        val result = repository.getAll()

        assertEquals(ListColour.Sky, result[0].colour)
    }

    @Test
    fun `should map entity None colour to domain ListColour None when reading`() {
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries", colour = "None"))

        val result = repository.getAll()

        assertEquals(ListColour.None, result[0].colour)
    }

    @Test
    fun `should map domain ListColour Mint to entity Mint string when inserting`() {
        repository.add(TodoList("1", "Groceries", colour = ListColour.Mint))

        verify { dao.insert(TodoListEntity("1", "Groceries", colour = "Mint")) }
    }

    @Test
    fun `should map domain ListColour Sky to entity Sky string when inserting at top`() {
        repository.addAtTop(TodoList("1", "Groceries", colour = ListColour.Sky))

        verify { dao.insertAtTop(TodoListEntity("1", "Groceries", colour = "Sky")) }
    }

    @Test
    fun `should map domain ListColour Butter to entity Butter string when inserting`() {
        repository.add(TodoList("1", "Groceries", colour = ListColour.Butter))

        verify { dao.insert(TodoListEntity("1", "Groceries", colour = "Butter")) }
    }

    @Test
    fun `should call dao update with colour name when update is called with Lilac`() {
        repository.update("1", "Groceries", null, null, ListColour.Lilac)

        verify { dao.update("1", "Groceries", null, null, "Lilac") }
    }

    @Test
    fun `should call dao update with colour name when update is called with Peach`() {
        repository.update("1", "Groceries", null, null, ListColour.Peach)

        verify { dao.update("1", "Groceries", null, null, "Peach") }
    }

    @Test
    fun `should call dao update with None when update is called with None colour`() {
        repository.update("1", "Groceries", null, null, ListColour.None)

        verify { dao.update("1", "Groceries", null, null, "None") }
    }

    @Test
    fun `should map entity Peach colour to domain ListColour Peach when reading`() {
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries", colour = "Peach"))

        val result = repository.getAll()

        assertEquals(ListColour.Peach, result[0].colour)
    }

    @Test
    fun `should map entity Lilac colour to domain ListColour Lilac when reading`() {
        every { dao.getAll() } returns listOf(TodoListEntity("1", "Groceries", colour = "Lilac"))

        val result = repository.getAll()

        assertEquals(ListColour.Lilac, result[0].colour)
    }
}
