package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddTodoUseCaseTest {

    private lateinit var repository: TodoRepository
    private lateinit var useCase: AddTodoUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = AddTodoUseCase(repository, generateId = { "fixed-id" })
    }

    @Test
    fun `should return item with correct title when title is valid`() {
        val result = useCase("Buy milk", "list-1")
        assertEquals("Buy milk", result.title)
    }

    @Test
    fun `should return item with generated id when title is valid`() {
        val result = useCase("Buy milk", "list-1")
        assertEquals("fixed-id", result.id)
    }

    @Test
    fun `should return item with correct listId when title is valid`() {
        val result = useCase("Buy milk", "list-1")
        assertEquals("list-1", result.listId)
    }

    @Test
    fun `should add item to repository when title is valid`() {
        val result = useCase("Buy milk", "list-1")
        verify { repository.add(result) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw when title is blank`() {
        useCase("   ", "list-1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw when title is empty`() {
        useCase("", "list-1")
    }

    @Test
    fun `should produce non-blank id when using default id generator`() {
        val defaultUseCase = AddTodoUseCase(repository)
        val result = defaultUseCase("Default id test", "list-1")
        assertTrue(result.id.isNotBlank())
    }

    @Test
    fun `should assign position 0 when list has no active items`() {
        every { repository.getAllByListId("list-1") } returns emptyList()
        val result = useCase("Buy milk", "list-1")
        assertEquals(0, result.position)
    }

    @Test
    fun `should assign position equal to active item count when active items already exist`() {
        every { repository.getAllByListId("list-1") } returns listOf(
            TodoItem("e1", "Existing", "list-1", position = 0),
            TodoItem("e2", "Also existing", "list-1", position = 1)
        )
        val result = useCase("Buy milk", "list-1")
        assertEquals(2, result.position)
    }

    @Test
    fun `should not count completed items toward the insertion position`() {
        every { repository.getAllByListId("list-1") } returns listOf(
            TodoItem("e1", "Active", "list-1", position = 0),
            TodoItem("e2", "Completed", "list-1", isCompleted = true, completedAt = 1000L, position = 1)
        )
        val result = useCase("Buy milk", "list-1")
        assertEquals(1, result.position)
    }

    @Test
    fun `should query existing items to determine the insertion position`() {
        every { repository.getAllByListId("list-1") } returns emptyList()
        useCase("Buy milk", "list-1")
        verify { repository.getAllByListId("list-1") }
    }
}
