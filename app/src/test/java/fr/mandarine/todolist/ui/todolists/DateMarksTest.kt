package fr.mandarine.todolist.ui.todolists

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A kind is something a date has, not something chosen before there is one. What a
 * kind glyph does when it is pressed follows from that, and follows from nothing
 * else: the rule is bare, or the day is on the other glyph, or the day is on this
 * one.
 */
class DateMarksTest {

    private val date: LocalDate = LocalDate.of(2026, 3, 14)

    @Test
    fun `should ask for a day when the calendar is pressed on a bare rule`() {
        assertEquals(
            KindPress.AskForADay,
            kindPressOn(DateSelection.None, DateKind.TARGET)
        )
    }

    @Test
    fun `should ask for a day when the alarm is pressed on a bare rule`() {
        assertEquals(
            KindPress.AskForADay,
            kindPressOn(DateSelection.None, DateKind.DUE)
        )
    }

    /**
     * The kind a bare rule happens to be holding must not decide anything. It is
     * left over from whatever was cleared last, and reading it is what made a new
     * list look like it already carried a target date.
     */
    @Test
    fun `should ask for a day whichever kind a bare rule is left holding`() {
        assertEquals(
            KindPress.AskForADay,
            kindPressOn(DateSelection(DateKind.DUE, null), DateKind.TARGET)
        )
    }

    @Test
    fun `should move the day across when the other glyph is pressed`() {
        assertEquals(
            KindPress.MoveTheDay,
            kindPressOn(DateSelection(DateKind.TARGET, date), DateKind.DUE)
        )
    }

    @Test
    fun `should rub the day out when the glyph already ringed is pressed`() {
        assertEquals(
            KindPress.RubItOut,
            kindPressOn(DateSelection(DateKind.TARGET, date), DateKind.TARGET)
        )
    }

    @Test
    fun `should say the kind that was chosen`() {
        val said = DateKindSaid()

        said.say(DateKind.DUE)

        assertEquals(DateKind.DUE, said.kind)
    }

    @Test
    fun `should stop saying it when hushed`() {
        val said = DateKindSaid()
        said.say(DateKind.DUE)

        said.hush()

        assertNull(said.kind)
    }

    /**
     * The words fade rather than cut, so the slip on its way out still has to know
     * which kind it is saying. Forgetting on the way out swapped the caption to the
     * other kind for the length of the fade.
     */
    @Test
    fun `should remember which kind it was saying while the words fade`() {
        val said = DateKindSaid()
        said.say(DateKind.DUE)

        said.hush()

        assertEquals(DateKind.DUE, said.last)
    }
}
