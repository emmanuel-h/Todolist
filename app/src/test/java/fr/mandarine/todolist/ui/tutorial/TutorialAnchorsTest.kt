package fr.mandarine.todolist.ui.tutorial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TutorialBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TutorialAnchorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val anchors = TutorialAnchors()

    @Before
    fun startTheDemo() {
        anchors.recordingAnchors = true
    }

    @Test
    fun `should report the bounds an anchored composable measured`() {
        composeRule.setContent { AnchoredRow(present = true) }

        assertNotNull(anchors.boundsOf(ANCHOR))
    }

    /**
     * Reporting a rectangle costs a layout callback and a snapshot write on every
     * frame the page moves, so a page nobody is being shown around measures nothing.
     */
    @Test
    fun `should measure nothing while no demo is running`() {
        anchors.recordingAnchors = false

        composeRule.setContent { AnchoredRow(present = true) }

        assertNull(anchors.boundsOf(ANCHOR))
    }

    @Test
    fun `should start measuring when a demo begins`() {
        anchors.recordingAnchors = false
        composeRule.setContent { AnchoredRow(present = true) }
        assertNull(anchors.boundsOf(ANCHOR))

        anchors.recordingAnchors = true
        composeRule.waitForIdle()

        assertNotNull(anchors.boundsOf(ANCHOR))
    }

    @Test
    fun `should drop what it measured when the demo ends`() {
        composeRule.setContent { AnchoredRow(present = true) }
        composeRule.waitForIdle()
        assertNotNull(anchors.boundsOf(ANCHOR))

        anchors.recordingAnchors = false
        composeRule.waitForIdle()

        assertNull(anchors.boundsOf(ANCHOR))
    }

    @Test
    fun `should measure nothing before anything has started it`() {
        assertFalse(TutorialAnchors().recordingAnchors)
    }

    @Test
    fun `should forget an anchor whose composable has left the page`() {
        var present by mutableStateOf(true)
        composeRule.setContent { AnchoredRow(present = present) }

        present = false
        composeRule.waitForIdle()

        assertNull(anchors.boundsOf(ANCHOR))
    }

    /**
     * The overlay reads these bounds from composition, so an anchor arriving or
     * leaving has to reach a reader — otherwise the phantom hand keeps pointing at
     * a row that is no longer on the page.
     */
    @Test
    fun `should tell a composition watching an anchor that it has left the page`() {
        var present by mutableStateOf(true)
        var seen: TutorialBounds? = null
        composeRule.setContent {
            AnchoredRow(present = present)
            val resolved = anchors.boundsOf(ANCHOR)
            SideEffect { seen = resolved }
        }
        composeRule.waitForIdle()
        assertNotNull(seen)

        present = false
        composeRule.waitForIdle()

        assertNull(seen)
    }

    @Test
    fun `should hold each anchor apart from the others`() {
        anchors.putBounds(ANCHOR, BOUNDS)

        assertEquals(BOUNDS, anchors.boundsOf(ANCHOR))
        assertNull(anchors.boundsOf(TutorialAnchor.FirstListRow))
    }

    @Composable
    private fun AnchoredRow(present: Boolean) {
        if (present) {
            Box(Modifier.size(48.dp).tutorialAnchor(anchors, ANCHOR))
        }
    }

    private companion object {
        val ANCHOR = TutorialAnchor.CreateListButton
        val BOUNDS = TutorialBounds(left = 1, top = 2, width = 3, height = 4)
    }
}
