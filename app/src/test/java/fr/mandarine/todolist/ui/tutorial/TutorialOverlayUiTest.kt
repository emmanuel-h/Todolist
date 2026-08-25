package fr.mandarine.todolist.ui.tutorial

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.R
import fr.mandarine.todolist.presentation.TutorialBannerContent
import fr.mandarine.todolist.presentation.TutorialBounds
import fr.mandarine.todolist.presentation.TutorialCaption
import fr.mandarine.todolist.ui.paper.PaperTheme
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TutorialOverlayUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val state = TutorialOverlayState()
    private val anchors = TutorialAnchors()
    private var skipped = 0

    @Test
    fun `should draw nothing while the tutorial is not running`() {
        render()

        composeRule.onNodeWithContentDescription(skipDescription()).assertDoesNotExist()
    }

    @Test
    fun `should offer a way out once the tutorial is running`() {
        onFrames { state.begin() }
        render()

        composeRule.onNodeWithContentDescription(skipDescription()).assertIsDisplayed()
    }

    @Test
    fun `should report a request to leave the tutorial`() {
        onFrames { state.begin() }
        render()

        composeRule.onNodeWithContentDescription(skipDescription()).performClick()

        assertEquals(1, skipped)
    }

    @Test
    fun `should keep the page underneath out of a screen reader while the demo runs`() {
        renderPage(demoRunning = true)

        assertNotNull(hiddenFromAccessibility())
    }

    @Test
    fun `should leave the page readable when no demo is running`() {
        renderPage(demoRunning = false)

        assertNull(hiddenFromAccessibility())
    }

    @Test
    fun `should mark the two date kinds with their own glyph rather than an emoji`() {
        assertEquals(R.drawable.ic_event, captionGlyph(TutorialCaption.TARGET_DATE))
        assertEquals(R.drawable.ic_alarm, captionGlyph(TutorialCaption.DUE_DATE))
    }

    @Test
    fun `should read out the caption that distinguishes the two date kinds`() {
        onFrames {
            state.begin()
            state.showCaption(TutorialCaption.TARGET_DATE, ANCHOR)
        }
        render()

        composeRule.onNodeWithText(string(R.string.date_kind_target_caption)).assertIsDisplayed()
    }

    @Test
    fun `should swap the caption when the tutorial moves to the due date`() {
        onFrames {
            state.begin()
            state.showCaption(TutorialCaption.TARGET_DATE, ANCHOR)
            state.updateCaption(TutorialCaption.DUE_DATE)
        }
        render()

        composeRule.onNodeWithText(string(R.string.date_kind_due_caption)).assertIsDisplayed()
    }

    @Test
    fun `should show the notification banner while it is on screen`() {
        val content = TutorialBannerContent("Groceries", null)
        val scope = CoroutineScope(Dispatchers.Unconfined + ImmediateFrameClock())
        scope.launch {
            state.begin()
            state.showBanner(content)
        }
        render()

        composeRule.onNodeWithText(bannerTextFor(content)).assertIsDisplayed()

        scope.cancel()
    }

    @Test
    fun `should name the list and its deadline in the notification banner`() {
        val text = bannerTextFor(TutorialBannerContent("Groceries", LocalDate.of(2026, 3, 14)))

        assertTrue(text, text.startsWith("🔔 Groceries ⏰ "))
        assertTrue(text, text.contains("14"))
        assertTrue(text, text.contains("3"))
    }

    @Test
    fun `should name only the list when it carries no deadline`() {
        assertEquals("🔔 Groceries", bannerTextFor(TutorialBannerContent("Groceries", null)))
    }

    private fun render() {
        composeRule.setContent {
            PaperTheme {
                TutorialOverlay(
                    state = state,
                    anchors = anchors,
                    onSkip = { skipped += 1 }
                )
            }
        }
    }

    private fun renderPage(demoRunning: Boolean) {
        composeRule.setContent {
            PaperTheme {
                Box(
                    Modifier
                        .size(48.dp)
                        .semantics { testTag = PAGE }
                        .behindTutorial(demoRunning)
                )
            }
        }
    }

    private fun hiddenFromAccessibility(): Unit? =
        composeRule.onNodeWithTag(PAGE, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.HideFromAccessibility)

    private fun onFrames(block: suspend () -> Unit) = runBlocking(ImmediateFrameClock()) { block() }

    private fun skipDescription(): String = string(R.string.cancel)

    private fun string(resId: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(resId)

    private class ImmediateFrameClock : MonotonicFrameClock {
        private var nanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            nanos += FRAME_NANOS
            return onFrame(nanos)
        }

        private companion object {
            const val FRAME_NANOS = 16_000_000L
        }
    }

    private companion object {
        val ANCHOR = TutorialBounds(left = 0, top = 300, width = 100, height = 80)
        const val PAGE = "page"
    }
}
