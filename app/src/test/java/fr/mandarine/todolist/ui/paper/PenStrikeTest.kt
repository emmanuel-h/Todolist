package fr.mandarine.todolist.ui.paper

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PenStrikeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should start an already crossed off item fully struck without drawing it again`() {
        lateinit var strike: PenStrikeState
        composeRule.setContent {
            PaperTheme {
                strike = rememberPenStrike("1", struck = true)
                Text("Milk", modifier = Modifier.penStrike(strike, PaperPalette.light.inkDone))
            }
        }
        composeRule.waitForIdle()

        assertEquals(1f, strike.progress.value, 0f)
    }

    @Test
    fun `should leave an item that is still to do unstruck`() {
        lateinit var strike: PenStrikeState
        composeRule.setContent {
            PaperTheme {
                strike = rememberPenStrike("1", struck = false)
                Text("Apples", modifier = Modifier.penStrike(strike, PaperPalette.light.ink))
            }
        }
        composeRule.waitForIdle()

        assertEquals(0f, strike.progress.value, 0f)
    }

    @Test
    fun `should draw the stroke on when the item is crossed off`() {
        var struck by mutableStateOf(false)
        lateinit var strike: PenStrikeState
        composeRule.setContent {
            PaperTheme {
                strike = rememberPenStrike("1", struck)
                Text("Milk", modifier = Modifier.penStrike(strike, PaperPalette.light.inkDone))
            }
        }

        struck = true
        composeRule.waitForIdle()

        assertEquals(1f, strike.progress.value, 0.001f)
    }

    @Test
    fun `should lift the stroke off again when the item comes back`() {
        var struck by mutableStateOf(true)
        lateinit var strike: PenStrikeState
        composeRule.setContent {
            PaperTheme {
                strike = rememberPenStrike("1", struck)
                Text("Milk", modifier = Modifier.penStrike(strike, PaperPalette.light.inkDone))
            }
        }

        struck = false
        composeRule.waitForIdle()

        assertEquals(0f, strike.progress.value, 0.001f)
    }

    @Test
    fun `should snap the stroke on without animating it when motion is reduced`() {
        var struck by mutableStateOf(false)
        lateinit var strike: PenStrikeState
        composeRule.setContent {
            PaperTheme {
                strike = rememberPenStrike("1", struck, animated = false)
                Text("Milk", modifier = Modifier.penStrike(strike, PaperPalette.light.inkDone))
            }
        }

        struck = true
        composeRule.waitForIdle()

        assertEquals(1f, strike.progress.value, 0f)
    }

    @Test
    fun `should measure the words before it has a stroke to draw through them`() {
        lateinit var strike: PenStrikeState
        composeRule.setContent {
            PaperTheme {
                strike = rememberPenStrike("1", struck = true)
                Text(
                    text = "Milk",
                    modifier = Modifier.penStrike(strike, PaperPalette.light.inkDone),
                    onTextLayout = strike::onTextLayout
                )
            }
        }
        composeRule.waitForIdle()

        assertNotNull(strike.layout)
        assertEquals(1, strike.layout?.lineCount)
    }

    @Test
    fun `should jitter the stroke of two items differently so no two are identical`() {
        lateinit var first: PenStrikeState
        lateinit var second: PenStrikeState
        composeRule.setContent {
            PaperTheme {
                first = rememberPenStrike("apples", struck = true)
                second = rememberPenStrike("milk", struck = true)
            }
        }
        composeRule.waitForIdle()

        assertTrue(first.seed != second.seed)
    }

    @Test
    fun `should give the same item the same stroke every time it is drawn`() {
        lateinit var first: PenStrikeState
        lateinit var second: PenStrikeState
        composeRule.setContent {
            PaperTheme {
                first = rememberPenStrike("apples", struck = true)
                second = rememberPenStrike("apples", struck = true)
            }
        }
        composeRule.waitForIdle()

        assertEquals(first.seed, second.seed)
    }
}
