package fr.mandarine.todolist.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ListColourTest {

    @Test
    fun `should have None constant`() {
        assertEquals(ListColour.None, ListColour.valueOf("None"))
    }

    @Test
    fun `should have Butter constant`() {
        assertEquals(ListColour.Butter, ListColour.valueOf("Butter"))
    }

    @Test
    fun `should have Mint constant`() {
        assertEquals(ListColour.Mint, ListColour.valueOf("Mint"))
    }

    @Test
    fun `should have Rose constant`() {
        assertEquals(ListColour.Rose, ListColour.valueOf("Rose"))
    }

    @Test
    fun `should have Sky constant`() {
        assertEquals(ListColour.Sky, ListColour.valueOf("Sky"))
    }

    @Test
    fun `should have Peach constant`() {
        assertEquals(ListColour.Peach, ListColour.valueOf("Peach"))
    }

    @Test
    fun `should have Lilac constant`() {
        assertEquals(ListColour.Lilac, ListColour.valueOf("Lilac"))
    }

    @Test
    fun `should store None by its name`() {
        assertEquals("None", ListColour.None.name)
    }

    @Test
    fun `should store Butter by its name`() {
        assertEquals("Butter", ListColour.Butter.name)
    }

    @Test
    fun `should store Mint by its name`() {
        assertEquals("Mint", ListColour.Mint.name)
    }

    @Test
    fun `should store Rose by its name`() {
        assertEquals("Rose", ListColour.Rose.name)
    }

    @Test
    fun `should store Sky by its name`() {
        assertEquals("Sky", ListColour.Sky.name)
    }

    @Test
    fun `should store Peach by its name`() {
        assertEquals("Peach", ListColour.Peach.name)
    }

    @Test
    fun `should store Lilac by its name`() {
        assertEquals("Lilac", ListColour.Lilac.name)
    }

    @Test
    fun `should have exactly seven constants`() {
        assertEquals(7, ListColour.entries.size)
    }
}
