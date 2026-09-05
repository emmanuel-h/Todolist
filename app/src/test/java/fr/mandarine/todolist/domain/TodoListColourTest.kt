package fr.mandarine.todolist.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TodoListColourTest {

    @Test
    fun `should default colour to None when not specified`() {
        val list = TodoList("1", "Groceries")

        assertEquals(ListColour.None, list.colour)
    }

    @Test
    fun `should store Butter colour when created with Butter`() {
        val list = TodoList("1", "Groceries", colour = ListColour.Butter)

        assertEquals(ListColour.Butter, list.colour)
    }

    @Test
    fun `should store Mint colour when created with Mint`() {
        val list = TodoList("1", "Groceries", colour = ListColour.Mint)

        assertEquals(ListColour.Mint, list.colour)
    }

    @Test
    fun `should store Rose colour when created with Rose`() {
        val list = TodoList("1", "Groceries", colour = ListColour.Rose)

        assertEquals(ListColour.Rose, list.colour)
    }

    @Test
    fun `should store Sky colour when created with Sky`() {
        val list = TodoList("1", "Groceries", colour = ListColour.Sky)

        assertEquals(ListColour.Sky, list.colour)
    }

    @Test
    fun `should store Peach colour when created with Peach`() {
        val list = TodoList("1", "Groceries", colour = ListColour.Peach)

        assertEquals(ListColour.Peach, list.colour)
    }

    @Test
    fun `should store Lilac colour when created with Lilac`() {
        val list = TodoList("1", "Groceries", colour = ListColour.Lilac)

        assertEquals(ListColour.Lilac, list.colour)
    }

    @Test
    fun `should copy list with updated colour`() {
        val original = TodoList("1", "Groceries", colour = ListColour.None)

        val copy = original.copy(colour = ListColour.Sky)

        assertEquals(ListColour.Sky, copy.colour)
    }

    @Test
    fun `should preserve other fields when copying with new colour`() {
        val original = TodoList("1", "Groceries", colour = ListColour.Mint)

        val copy = original.copy(colour = ListColour.Peach)

        assertEquals("1", copy.id)
        assertEquals("Groceries", copy.name)
        assertEquals(ListColour.Peach, copy.colour)
    }

    @Test
    fun `should not be equal when colours differ`() {
        val a = TodoList("1", "Groceries", colour = ListColour.Butter)
        val b = TodoList("1", "Groceries", colour = ListColour.Mint)

        assert(a != b)
    }

    @Test
    fun `should be equal when colours are the same`() {
        val a = TodoList("1", "Groceries", colour = ListColour.Sky)
        val b = TodoList("1", "Groceries", colour = ListColour.Sky)

        assertEquals(a, b)
    }
}
