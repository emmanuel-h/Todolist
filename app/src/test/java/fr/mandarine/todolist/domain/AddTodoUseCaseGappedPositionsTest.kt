package fr.mandarine.todolist.domain

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AddTodoUseCaseGappedPositionsTest {

    private lateinit var repository: TodoRepository
    private lateinit var useCase: AddTodoUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = AddTodoUseCase(repository, generateId = { "fixed-id" })
    }

    @Test
    fun `should assign position past highest active position when active positions contain gaps`() {
        every { repository.getAllByListId("list-1") } returns listOf(
            TodoItem("1", "A", "list-1", position = 24),
            TodoItem("2", "B", "list-1", position = 25)
        )

        val result = useCase("New", "list-1")

        assertEquals(26, result.position)
    }

    @Test
    fun `should assign position past highest active position when active positions contain duplicates`() {
        every { repository.getAllByListId("list-1") } returns listOf(
            TodoItem("1", "A", "list-1", position = 5),
            TodoItem("2", "B", "list-1", position = 5)
        )

        val result = useCase("New", "list-1")

        assertEquals(6, result.position)
    }

    @Test
    fun `should assign position past restored items high water mark when earlier items have low positions`() {
        every { repository.getAllByListId("list-1") } returns listOf(
            TodoItem("1", "A", "list-1", position = 0),
            TodoItem("2", "B", "list-1", position = 1),
            TodoItem("3", "C", "list-1", position = 26),
            TodoItem("4", "D", "list-1", position = 27)
        )

        val result = useCase("New", "list-1")

        assertEquals(28, result.position)
    }

    @Test
    fun `should not collide with any existing position when active positions are 24 and 25 with duplicates`() {
        every { repository.getAllByListId("list-1") } returns listOf(
            TodoItem("1", "A", "list-1", position = 24),
            TodoItem("2", "B", "list-1", position = 24),
            TodoItem("3", "C", "list-1", position = 25),
            TodoItem("4", "D", "list-1", position = 25),
            TodoItem("5", "E", "list-1", position = 26),
            TodoItem("6", "F", "list-1", position = 27)
        )

        val result = useCase("New", "list-1")

        assertEquals(28, result.position)
    }

    @Test
    fun `should ignore completed items when determining highest active position with gaps`() {
        every { repository.getAllByListId("list-1") } returns listOf(
            TodoItem("1", "Active A", "list-1", position = 3),
            TodoItem("2", "Completed high", "list-1", isCompleted = true, completedAt = 1000L, position = 99)
        )

        val result = useCase("New", "list-1")

        assertEquals(4, result.position)
    }
}
