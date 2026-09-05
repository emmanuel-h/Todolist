package fr.mandarine.todolist.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TodoListEntityColourTest {

    @Test
    fun `should default colour to None when not specified`() {
        val entity = TodoListEntity("1", "Groceries")

        assertEquals("None", entity.colour)
    }

    @Test
    fun `should store Mint colour when created with Mint`() {
        val entity = TodoListEntity("1", "Groceries", colour = "Mint")

        assertEquals("Mint", entity.colour)
    }

    @Test
    fun `should store Butter colour when created with Butter`() {
        val entity = TodoListEntity("1", "Groceries", colour = "Butter")

        assertEquals("Butter", entity.colour)
    }

    @Test
    fun `should store Rose colour when created with Rose`() {
        val entity = TodoListEntity("1", "Groceries", colour = "Rose")

        assertEquals("Rose", entity.colour)
    }

    @Test
    fun `should store Sky colour when created with Sky`() {
        val entity = TodoListEntity("1", "Groceries", colour = "Sky")

        assertEquals("Sky", entity.colour)
    }

    @Test
    fun `should store Peach colour when created with Peach`() {
        val entity = TodoListEntity("1", "Groceries", colour = "Peach")

        assertEquals("Peach", entity.colour)
    }

    @Test
    fun `should store Lilac colour when created with Lilac`() {
        val entity = TodoListEntity("1", "Groceries", colour = "Lilac")

        assertEquals("Lilac", entity.colour)
    }

    @Test
    fun `should copy entity with updated colour`() {
        val original = TodoListEntity("1", "Groceries", colour = "None")

        val copy = original.copy(colour = "Mint")

        assertEquals("Mint", copy.colour)
    }

    @Test
    fun `should not be equal when colours differ`() {
        val a = TodoListEntity("1", "Groceries", colour = "Butter")
        val b = TodoListEntity("1", "Groceries", colour = "Mint")

        assert(a != b)
    }

    @Test
    fun `should be equal when colours are the same`() {
        val a = TodoListEntity("1", "Groceries", colour = "Sky")
        val b = TodoListEntity("1", "Groceries", colour = "Sky")

        assertEquals(a, b)
    }
}
