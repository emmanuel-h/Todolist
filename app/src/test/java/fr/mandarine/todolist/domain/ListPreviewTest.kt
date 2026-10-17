package fr.mandarine.todolist.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ListPreviewTest {

    @Test
    fun `should store listId as provided`() {
        val preview = ListPreview("list-42", listOf("Buy milk"))
        assertEquals("list-42", preview.listId)
    }

    @Test
    fun `should store titles as provided`() {
        val titles = listOf("Buy milk", "Walk dog")
        val preview = ListPreview("list-1", titles)
        assertEquals(titles, preview.titles)
    }

    @Test
    fun `should store empty titles list when no titles provided`() {
        val preview = ListPreview("list-1", emptyList())
        assertEquals(emptyList<String>(), preview.titles)
    }

    @Test
    fun `should be equal to another ListPreview with same fields`() {
        val a = ListPreview("list-1", listOf("Alpha"))
        val b = ListPreview("list-1", listOf("Alpha"))
        assertEquals(a, b)
    }

    @Test
    fun `should produce copy with changed listId`() {
        val original = ListPreview("list-1", listOf("Alpha"))
        val copy = original.copy(listId = "list-2")
        assertEquals("list-2", copy.listId)
        assertEquals(listOf("Alpha"), copy.titles)
    }

    @Test
    fun `should produce copy with changed titles`() {
        val original = ListPreview("list-1", listOf("Alpha"))
        val copy = original.copy(titles = listOf("Beta", "Gamma"))
        assertEquals("list-1", copy.listId)
        assertEquals(listOf("Beta", "Gamma"), copy.titles)
    }
}
