package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CreateTodoListUseCaseColourTest {

    private lateinit var repository: TodoListRepository
    private lateinit var useCase: CreateTodoListUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = CreateTodoListUseCase(repository, generateId = { "fixed-id" })
    }

    @Test
    fun `should default colour to None when not specified`() {
        val result = useCase("Groceries")

        assertEquals(ListColour.None, result.colour)
    }

    @Test
    fun `should store Mint colour when created with Mint`() {
        val result = useCase("Groceries", colour = ListColour.Mint)

        assertEquals(ListColour.Mint, result.colour)
    }

    @Test
    fun `should store Butter colour when created with Butter`() {
        val result = useCase("Groceries", colour = ListColour.Butter)

        assertEquals(ListColour.Butter, result.colour)
    }

    @Test
    fun `should store Sky colour when created with Sky`() {
        val result = useCase("Groceries", colour = ListColour.Sky)

        assertEquals(ListColour.Sky, result.colour)
    }

    @Test
    fun `should store Rose colour when created with Rose`() {
        val result = useCase("Groceries", colour = ListColour.Rose)

        assertEquals(ListColour.Rose, result.colour)
    }

    @Test
    fun `should store Peach colour when created with Peach`() {
        val result = useCase("Groceries", colour = ListColour.Peach)

        assertEquals(ListColour.Peach, result.colour)
    }

    @Test
    fun `should store Lilac colour when created with Lilac`() {
        val result = useCase("Groceries", colour = ListColour.Lilac)

        assertEquals(ListColour.Lilac, result.colour)
    }

    @Test
    fun `should add list with Mint colour to repository`() {
        val result = useCase("Groceries", colour = ListColour.Mint)

        verify { repository.addAtTop(result) }
    }

    @Test
    fun `should add list with Sky colour to repository`() {
        val result = useCase("Groceries", colour = ListColour.Sky)

        verify { repository.addAtTop(result) }
    }
}
