package fr.mandarine.todolist.ui.paper

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderSlipTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val day: LocalDate = LocalDate.of(2026, 3, 14)

    @Test
    fun `should write a reminder as a bell a name and the day it rings on`() {
        val text = reminderSlipText(ReminderNote("Groceries", day), Locale.UK)

        assertEquals("🔔 Groceries ⏰ 14/03", text)
    }

    /**
     * The order the day is written in belongs to the reader's locale, not to
     * English — the slip is the one place the app says a date in prose.
     */
    @Test
    fun `should not impose English field order on the day`() {
        val text = reminderSlipText(ReminderNote("Courses", day), Locale.US)

        assertEquals("🔔 Courses ⏰ 3/14", text)
    }

    @Test
    fun `should write a reminder with no day as the name alone`() {
        assertEquals("🔔 Groceries", reminderSlipText(ReminderNote("Groceries", null), Locale.UK))
    }

    @Test
    fun `should raise a note and let it go again`() {
        val notes = ReminderNotes()

        notes.raise(ReminderNote("Groceries", day))

        assertEquals("Groceries", notes.raised?.listName)

        notes.lower()

        assertNull(notes.raised)
    }

    /**
     * The slip slides away rather than cutting, so it still has to know what it was
     * saying while it goes. Forgetting on the way out emptied it mid-slide.
     */
    @Test
    fun `should remember what it was saying while the slip slides away`() {
        val notes = ReminderNotes()
        notes.raise(ReminderNote("Groceries", day))

        notes.lower()

        assertEquals("Groceries", notes.last?.listName)
    }

    @Test
    fun `should show nothing until a reminder is written`() {
        val notes = ReminderNotes()
        composeRule.setContent { PaperTheme { ReminderSlip(notes, animated = false) } }

        composeRule.onNodeWithText("🔔 Groceries", substring = true).assertDoesNotExist()
    }

    @Test
    fun `should drop a slip saying what was just written`() {
        val notes = ReminderNotes()
        composeRule.setContent { PaperTheme { ReminderSlip(notes, animated = false) } }

        composeRule.runOnIdle { notes.raise(ReminderNote("Groceries", day)) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("🔔 Groceries", substring = true).assertIsDisplayed()
    }
}
