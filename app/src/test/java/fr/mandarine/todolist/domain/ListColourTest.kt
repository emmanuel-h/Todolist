package fr.mandarine.todolist.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ListColourTest {

    @Test
    fun `should answer the colour when the stored name is one of them`() {
        assertEquals(ListColour.Mint, ListColour.named("Mint"))
    }

    @Test
    fun `should answer the first colour when the stored name is that one`() {
        assertEquals(ListColour.None, ListColour.named("None"))
    }

    @Test
    fun `should answer the last colour when the stored name is that one`() {
        assertEquals(ListColour.Lilac, ListColour.named("Lilac"))
    }

    /**
     * The case the app used to die on. A row written by a version that had a colour
     * this one does not — restored from a backup, say — reads as no colour rather
     * than throwing on every launch with no way back into the app.
     */
    @Test
    fun `should answer None when the stored name is not a colour`() {
        assertEquals(ListColour.None, ListColour.named("Vermilion"))
    }

    @Test
    fun `should answer None when the stored name is empty`() {
        assertEquals(ListColour.None, ListColour.named(""))
    }
}
