package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class EditTodoListUseCaseColourTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: EditTodoListUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = EditTodoListUseCase(repository)
    }

    @Test
    fun `should default colour to None when not specified`() {
        useCase("list-1", "Groceries", null)

        verify { repository.update("list-1", "Groceries", null, null, ListColour.None) }
    }

    @Test
    fun `should pass Mint colour to repository update`() {
        useCase("list-1", "Groceries", null, colour = ListColour.Mint)

        verify { repository.update("list-1", "Groceries", null, null, ListColour.Mint) }
    }

    @Test
    fun `should pass Butter colour to repository update`() {
        useCase("list-1", "Groceries", null, colour = ListColour.Butter)

        verify { repository.update("list-1", "Groceries", null, null, ListColour.Butter) }
    }

    @Test
    fun `should pass Sky colour to repository update`() {
        useCase("list-1", "Groceries", null, colour = ListColour.Sky)

        verify { repository.update("list-1", "Groceries", null, null, ListColour.Sky) }
    }

    @Test
    fun `should pass Rose colour to repository update`() {
        useCase("list-1", "Groceries", null, colour = ListColour.Rose)

        verify { repository.update("list-1", "Groceries", null, null, ListColour.Rose) }
    }

    @Test
    fun `should pass Peach colour to repository update`() {
        useCase("list-1", "Groceries", null, colour = ListColour.Peach)

        verify { repository.update("list-1", "Groceries", null, null, ListColour.Peach) }
    }

    @Test
    fun `should pass Lilac colour to repository update`() {
        useCase("list-1", "Groceries", null, colour = ListColour.Lilac)

        verify { repository.update("list-1", "Groceries", null, null, ListColour.Lilac) }
    }

    @Test
    fun `should not call repository when name is blank even with a colour`() {
        verify(exactly = 0) { repository.update("list-1", "   ", null, null, ListColour.Mint) }
    }
}
