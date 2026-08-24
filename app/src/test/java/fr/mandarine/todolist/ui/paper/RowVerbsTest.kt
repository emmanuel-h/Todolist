package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RowVerbsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should keep every verb a row was given, in the order it was given them`() {
        val verbs = rowVerbs(
            RowVerb("complete") {},
            RowVerb("edit") {},
            RowVerb("delete") {}
        )

        assertEquals(listOf("complete", "edit", "delete"), verbs.map { it.label })
    }

    @Test
    fun `should drop the verbs a row cannot answer to`() {
        val verbs = rowVerbs(
            RowVerb("delete") {},
            null,
            RowVerb("move down") {},
            null
        )

        assertEquals(listOf("delete", "move down"), verbs.map { it.label })
    }

    @Test
    fun `should offer no verbs at all when a row has none`() {
        assertTrue(rowVerbs().isEmpty())
        assertTrue(rowVerbs(null, null).isEmpty())
    }

    @Test
    fun `should speak each verb under the label it was given`() {
        render(rowVerbs(RowVerb("delete") {}, RowVerb("move up") {}))

        assertEquals(listOf("delete", "move up"), spokenVerbs().map { it.label })
    }

    @Test
    fun `should run the verb behind the label and report it as handled`() {
        var performed = 0
        render(rowVerbs(RowVerb("delete") { performed += 1 }))

        assertEquals(true, spokenVerbs().single().action())
        assertEquals(1, performed)
    }

    @Test
    fun `should leave the node untouched when a row has nothing to offer`() {
        render(emptyList())

        assertNull(
            composeRule.onNodeWithTag(TAG).fetchSemanticsNode()
                .config.getOrNull(SemanticsActions.CustomActions)
        )
    }

    private fun render(verbs: List<RowVerb>) {
        composeRule.setContent {
            Box(
                Modifier
                    .size(48.dp)
                    .semantics { testTag = TAG }
                    .spokenVerbs(verbs)
            )
        }
    }

    private fun spokenVerbs(): List<CustomAccessibilityAction> =
        composeRule.onNodeWithTag(TAG).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.CustomActions)
            .orEmpty()

    private companion object {
        const val TAG = "verbs"
    }
}
