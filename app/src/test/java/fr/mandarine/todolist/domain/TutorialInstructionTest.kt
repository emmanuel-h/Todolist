package fr.mandarine.todolist.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TutorialInstructionTest {

    @Test
    fun `should expose the index each indexed anchor was built with`() {
        assertEquals(3, TutorialAnchor.ActiveItemToggle(3).index)
        assertEquals(4, TutorialAnchor.CompletedItemToggle(4).index)
        assertEquals(5, TutorialAnchor.ActiveItemDragHandle(5).index)
        assertEquals(6, TutorialAnchor.ActiveItemRow(6).index)
    }

    @Test
    fun `should expose the index each indexed action was built with`() {
        assertEquals(7, TutorialAction.ToggleActiveItem(7).index)
        assertEquals(8, TutorialAction.ToggleCompletedItem(8).index)
    }

    @Test
    fun `should expose both endpoints of a move action`() {
        val move = TutorialAction.MoveActiveItem(from = 4, to = 2)

        assertEquals(4, move.from)
        assertEquals(2, move.to)
    }

    @Test
    fun `should expose both endpoints of a reorder commit`() {
        val commit = TutorialAction.CommitReorder(from = 6, to = 3)

        assertEquals(6, commit.from)
        assertEquals(3, commit.to)
    }

    @Test
    fun `should expose the text each typing action was built with`() {
        assertEquals("🛒 Groceries", TutorialAction.TypeListName("🛒 Groceries").text)
        assertEquals("🍎 Apples", TutorialAction.TypeItemTitle("🍎 Apples").text)
    }

    @Test
    fun `should expose the date a due date action was built with`() {
        val date = LocalDate.of(2026, 8, 22)

        assertEquals(date, TutorialAction.PickDueDate(date).date)
    }
}
