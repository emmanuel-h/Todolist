package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AddTodoUseCaseBottomInsertTest {

    private lateinit var repository: TodoRepository
    private lateinit var useCase: AddTodoUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = AddTodoUseCase(repository, generateId = { "fixed-id" })
    }

    @Test
    fun `should assign position zero when list has no active items`() {
        every { repository.getAllByListId("list-1") } returns emptyList()

        val result = useCase("Buy milk", "list-1")

        assertEquals(0, result.position)
    }

    @Test
    fun `should assign position one when list has one active item`() {
        every { repository.getAllByListId("list-1") } returns listOf(
            TodoItem("existing", "Existing", "list-1", position = 0)
        )

        val result = useCase("Buy milk", "list-1")

        assertEquals(1, result.position)
    }

    @Test
    fun `should assign position equal to active item count when list has multiple active items`() {
        every { repository.getAllByListId("list-1") } returns listOf(
            TodoItem("1", "First", "list-1", position = 0),
            TodoItem("2", "Second", "list-1", position = 1)
        )

        val result = useCase("Buy milk", "list-1")

        assertEquals(2, result.position)
    }

    @Test
    fun `should not count completed items when computing position for new item`() {
        every { repository.getAllByListId("list-1") } returns listOf(
            TodoItem("1", "Active", "list-1", position = 0),
            TodoItem("2", "Done", "list-1", isCompleted = true, completedAt = 1000L, position = 0)
        )

        val result = useCase("New item", "list-1")

        assertEquals(1, result.position)
    }

    @Test
    fun `should call getAllByListId before add when computing position`() {
        every { repository.getAllByListId("list-1") } returns emptyList()

        val result = useCase("Buy milk", "list-1")

        verifyOrder {
            repository.getAllByListId("list-1")
            repository.add(result)
        }
    }
}
