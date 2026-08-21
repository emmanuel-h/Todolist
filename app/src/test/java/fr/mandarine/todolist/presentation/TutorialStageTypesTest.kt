package fr.mandarine.todolist.presentation

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TutorialStageTypesTest {

    @Test
    fun `should expose every edge of the bounds it was built with`() {
        val bounds = TutorialBounds(left = 12, top = 34, width = 56, height = 78)

        assertEquals(12, bounds.left)
        assertEquals(34, bounds.top)
        assertEquals(56, bounds.width)
        assertEquals(78, bounds.height)
    }

    @Test
    fun `should expose the banner content it was built with`() {
        val dueDate = LocalDate.of(2026, 8, 22)
        val content = TutorialBannerContent("🛒 Groceries", dueDate)

        assertEquals("🛒 Groceries", content.listName)
        assertEquals(dueDate, content.dueDate)
    }

    @Test
    fun `should allow banner content without a due date`() {
        assertNull(TutorialBannerContent("🛒 Groceries", null).dueDate)
    }
}
