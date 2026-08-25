package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaperPrimitivesTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should build a grain tile at the documented size`() {
        val tile = paperGrainTile(density = 2f)

        assertEquals(PaperDimens.GRAIN_TILE_PIXELS, tile.width)
        assertEquals(PaperDimens.GRAIN_TILE_PIXELS, tile.height)
    }

    @Test
    fun `should build the same grain tile every time so the paper never shimmers`() {
        val first = paperGrainTile(density = 2f).toPixelMap().buffer
        val second = paperGrainTile(density = 2f).toPixelMap().buffer

        assertTrue(first.contentEquals(second))
    }

    @Test
    fun `should bake a different grain tile for a different density`() {
        val coarse = bakePaperGrainTile(density = 1f).toPixelMap().buffer
        val fine = bakePaperGrainTile(density = 3f).toPixelMap().buffer

        assertTrue(!coarse.contentEquals(fine))
    }

    @Test
    fun `should keep the grain within the darkening budget of the paper`() {
        val pixels = bakePaperGrainTile(density = 2f).toPixelMap()

        var darkest = 1f
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                darkest = minOf(darkest, pixels[x, y].red)
            }
        }

        assertTrue(darkest > 0.9f)
    }

    @Test
    fun `should draw its content on top of the paper surface`() {
        composeRule.setContent {
            PaperTheme {
                PaperSurface(Modifier.height(400.dp)) {
                    Text("on paper", modifier = Modifier.testTag("content"))
                }
            }
        }

        composeRule.onNodeWithTag("content").assertIsDisplayed()
    }

    @Test
    fun `should not react to taps when a ruled row is given no click handler`() {
        composeRule.setContent {
            PaperTheme {
                RuledRow(modifier = Modifier.testTag("row")) {
                    Text("🍎 Apples")
                }
            }
        }

        composeRule.onNodeWithTag("row").performClick()
        composeRule.onNodeWithText("🍎 Apples").assertIsDisplayed()
    }

    @Test
    fun `should report the tap when a ruled row is given a click handler`() {
        var tapped = false
        composeRule.setContent {
            PaperTheme {
                RuledRow(modifier = Modifier.testTag("row"), onClick = { tapped = true }) {
                    Text("🍎 Apples")
                }
            }
        }

        composeRule.onNodeWithTag("row").performClick()

        assertTrue(tapped)
    }

    @Test
    fun `should show only the ellipsis hint on an add line and take the pen when it is tapped`() {
        var penUp = false
        composeRule.setContent {
            PaperTheme {
                InkAddLine(
                    text = "",
                    onTextChange = {},
                    onCommit = {},
                    armed = false,
                    onPenUp = { penUp = true },
                    onPenDown = {},
                    modifier = Modifier.testTag("add-line")
                )
            }
        }

        composeRule.onNodeWithText("…").assertIsDisplayed()
        composeRule.onNodeWithTag("add-line").performClick()

        assertTrue(penUp)
    }

    @Test
    fun `should report taps on an ink icon button and refuse them when disabled`() {
        var taps = 0
        composeRule.setContent {
            PaperTheme {
                Row {
                    InkIconButton(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "add",
                        onClick = { taps++ }
                    )
                    InkIconButton(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "delete",
                        onClick = { taps++ },
                        enabled = false
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("add").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("delete").assertIsNotEnabled()

        assertEquals(1, taps)
    }

    @Test
    fun `should toggle the ink ring when it is tapped`() {
        var toggles = 0
        composeRule.setContent {
            PaperTheme {
                InkRing(
                    checked = false,
                    onToggle = { toggles++ },
                    seed = 1,
                    contentDescription = "tick",
                    stateDescription = "open"
                )
            }
        }

        composeRule.onNodeWithContentDescription("tick").performClick()
        composeRule.waitForIdle()

        assertEquals(1, toggles)
    }

    @Test
    fun `should stay silent when a ring is first drawn already ticked`() {
        val buzzes = mutableListOf<HapticFeedbackType>()
        composeRule.setContent {
            PaperTheme {
                CompositionLocalProvider(LocalHapticFeedback provides RecordingHaptics(buzzes)) {
                    InkRing(
                        checked = true,
                        onToggle = {},
                        seed = 1,
                        contentDescription = "tick",
                        stateDescription = "done"
                    )
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals(emptyList<HapticFeedbackType>(), buzzes)
    }

    @Test
    fun `should still buzz the toggle when the ring is snapped with motion reduced`() {
        val buzzes = mutableListOf<HapticFeedbackType>()
        var checked by mutableStateOf(false)
        composeRule.setContent {
            PaperTheme {
                CompositionLocalProvider(LocalHapticFeedback provides RecordingHaptics(buzzes)) {
                    InkRing(
                        checked = checked,
                        onToggle = { checked = !checked },
                        seed = 1,
                        contentDescription = "tick",
                        stateDescription = "open",
                        animated = false
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("tick").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(HapticFeedbackType.ToggleOn), buzzes)
    }

    private class RecordingHaptics(private val buzzes: MutableList<HapticFeedbackType>) :
        HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            buzzes += hapticFeedbackType
        }
    }

    @Test
    fun `should read the ink ring out as a checkbox carrying its own state`() {
        composeRule.setContent {
            PaperTheme {
                InkRing(
                    checked = true,
                    onToggle = {},
                    seed = 1,
                    contentDescription = "tick",
                    stateDescription = "done"
                )
            }
        }

        val node = composeRule.onNodeWithContentDescription("tick").fetchSemanticsNode()

        assertEquals(Role.Checkbox, node.config.getOrNull(SemanticsProperties.Role))
        assertEquals("done", node.config.getOrNull(SemanticsProperties.StateDescription))
        assertEquals(
            ToggleableState.On,
            node.config.getOrNull(SemanticsProperties.ToggleableState)
        )
    }

    @Test
    fun `should hand over a sheet when the sticky note pad is tapped`() {
        var taken = false
        composeRule.setContent {
            PaperTheme {
                StickyNotePad(onTake = { taken = true }, contentDescription = "add")
            }
        }

        composeRule.onNodeWithContentDescription("add").performClick()
        composeRule.waitForIdle()

        assertTrue(taken)
    }

    @Test
    fun `should hide the top sheet while the taken one is in use`() {
        composeRule.setContent {
            PaperTheme {
                StickyNotePad(onTake = {}, contentDescription = "add", taken = true)
            }
        }

        composeRule.onNodeWithContentDescription("add").assertDoesNotExist()
    }

    @Test
    fun `should hand over a sheet without peeling it when motion is reduced`() {
        var taken = false
        composeRule.setContent {
            PaperTheme {
                StickyNotePad(
                    onTake = { taken = true },
                    contentDescription = "add",
                    reducedMotion = true
                )
            }
        }

        composeRule.onNodeWithContentDescription("add").performClick()
        composeRule.waitForIdle()

        assertTrue(taken)
        composeRule.onNodeWithContentDescription("add").assertIsDisplayed()
    }

    @Test
    fun `should settle a replacement sheet in when the pad stops being taken`() {
        var taken by mutableStateOf(true)
        composeRule.setContent {
            PaperTheme {
                StickyNotePad(onTake = {}, contentDescription = "add", taken = taken)
            }
        }

        composeRule.onNodeWithContentDescription("add").assertDoesNotExist()

        taken = false
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("add").assertIsDisplayed()
    }

    @Test
    fun `should keep the replacement sheet still when motion is reduced`() {
        var taken by mutableStateOf(true)
        composeRule.setContent {
            PaperTheme {
                StickyNotePad(
                    onTake = {},
                    contentDescription = "add",
                    taken = taken,
                    reducedMotion = true
                )
            }
        }

        taken = false
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("add").assertIsDisplayed()
    }

    @Test
    fun `should hand the palette to everything drawn inside the theme`() {
        val captured = mutableListOf<PaperPalette>()
        composeRule.setContent {
            PaperTheme { captured += LocalPaperPalette.current }
        }
        composeRule.waitForIdle()

        assertEquals(PaperPalette.light, captured.single())
    }

    @Test
    fun `should write every line of the app in the one bundled hand`() {
        val captured = mutableListOf<androidx.compose.ui.text.TextStyle>()
        composeRule.setContent {
            PaperTheme {
                captured += MaterialTheme.typography.bodyLarge
                captured += MaterialTheme.typography.titleMedium
                captured += MaterialTheme.typography.labelSmall
            }
        }
        composeRule.waitForIdle()

        assertTrue(captured.all { it.fontFamily == PaperType.hand })
        assertEquals(PaperType.itemLine.fontSize, captured[0].fontSize)
        assertEquals(PaperType.listLine.fontSize, captured[1].fontSize)
    }

    @Test
    fun `should seat a mark with no baseline of its own by its foot where the hand writes`() {
        val baselines = mutableListOf<Float>()
        var pitch = 0
        composeRule.setContent {
            PaperTheme {
                pitch = with(LocalDensity.current) { LocalPagePitch.current.roundToPx() }
                Box {
                    Text(
                        text = SEATED_SAMPLE,
                        modifier = Modifier.seatOnRule(),
                        onTextLayout = { baselines += it.firstBaseline }
                    )
                    Box(Modifier.height(SEATED_GLYPH).seatOnRule().testTag(SEATED))
                }
            }
        }
        composeRule.waitForIdle()

        val written = composeRule.onNodeWithText(SEATED_SAMPLE, useUnmergedTree = true)
            .fetchSemanticsNode()
        val footed = composeRule.onNodeWithTag(SEATED, useUnmergedTree = true).fetchSemanticsNode()
        val line = written.positionInRoot.y + baselines.first()

        assertEquals(line, footed.positionInRoot.y + footed.size.height, ONE_PIXEL)
        assertTrue("expected $line to write above the rule at $pitch", line < pitch)
        assertTrue("expected $line to sit on the rule at $pitch", pitch - line < pitch / A_HAIR)
    }

    @Test
    fun `should map the ink palette onto the material colour roles`() {
        val captured = mutableListOf<Color>()
        composeRule.setContent {
            PaperTheme {
                captured += MaterialTheme.colorScheme.primary
                captured += MaterialTheme.colorScheme.surface
                captured += MaterialTheme.colorScheme.error
                captured += MaterialTheme.colorScheme.outlineVariant
                captured += MaterialTheme.colorScheme.surfaceTint
            }
        }
        composeRule.waitForIdle()

        assertEquals(PaperPalette.light.inkBlue, captured[0])
        assertEquals(PaperPalette.light.paper, captured[1])
        assertEquals(PaperPalette.light.inkRed, captured[2])
        assertEquals(PaperPalette.light.rule, captured[3])
        assertEquals(0f, captured[4].alpha, 0f)
    }

    private companion object {
        const val SEATED = "seated"
        const val SEATED_SAMPLE = "Ag"
        const val ONE_PIXEL = 1f
        const val A_HAIR = 8
        val SEATED_GLYPH = 12.dp
    }
}
