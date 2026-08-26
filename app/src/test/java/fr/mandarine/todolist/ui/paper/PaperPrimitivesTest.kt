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
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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

    private val PULLED_ROW = "pulled-row"
    private val PULL_PIXELS = 120f

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `should build a grain tile at the documented size`() {
        val tile = paperGrainTile(density = 2f, grain = PaperGrain.DarkFleck)

        assertEquals(PaperDimens.GRAIN_TILE_PIXELS, tile.width)
        assertEquals(PaperDimens.GRAIN_TILE_PIXELS, tile.height)
    }

    @Test
    fun `should build the same grain tile every time so the paper never shimmers`() {
        val first = paperGrainTile(density = 2f, grain = PaperGrain.DarkFleck).toPixelMap().buffer
        val second = paperGrainTile(density = 2f, grain = PaperGrain.DarkFleck).toPixelMap().buffer

        assertTrue(first.contentEquals(second))
    }

    @Test
    fun `should bake a different grain tile for a different density`() {
        val coarse = bakePaperGrainTile(density = 1f, grain = PaperGrain.DarkFleck).toPixelMap().buffer
        val fine = bakePaperGrainTile(density = 3f, grain = PaperGrain.DarkFleck).toPixelMap().buffer

        assertTrue(!coarse.contentEquals(fine))
    }

    @Test
    fun `should keep the grain within the darkening budget of the paper`() {
        val pixels = bakePaperGrainTile(density = 2f, grain = PaperGrain.DarkFleck).toPixelMap()

        var darkest = 1f
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                darkest = minOf(darkest, pixels[x, y].red)
            }
        }

        assertTrue(darkest > 0.9f)
    }

    @Test
    fun `should keep the night grain within the lighting budget of the paper`() {
        val pixels = bakePaperGrainTile(density = 2f, grain = PaperGrain.PaleFibre).toPixelMap()

        var brightest = 0f
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                brightest = maxOf(brightest, pixels[x, y].red)
            }
        }

        assertTrue("night grain lit nothing", brightest > 0f)
        assertTrue("night grain lit by $brightest", brightest < 0.15f)
    }

    /**
     * The pad is a second stock of paper rather than the one bright object on the
     * desk, so it is grained by the room it is in and not by what it is for. It
     * used to be the single tone that took daylight grain at night, which is the
     * same decision that made it read as a lamp.
     */
    @Test
    fun `should take the grain from the tone the sheet is drawn in`() {
        assertEquals(PaperGrain.DarkFleck, paperGrainOn(PaperPalette.light.paper))
        assertEquals(PaperGrain.DarkFleck, paperGrainOn(PaperPalette.light.stickyNote))
        assertEquals(PaperGrain.PaleFibre, paperGrainOn(PaperPalette.night.paper))
        assertEquals(PaperGrain.PaleFibre, paperGrainOn(PaperPalette.night.stickyNote))
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
                    spoken = "Add an item",
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

    /**
     * A row the tour can hold aside is still a row. Handing the demonstration the
     * only say over how far it is pulled left the first row on the page unable to
     * move under a finger at all — the gesture still fired, so nothing failed
     * except the paper, which simply stopped following the hand.
     */
    @Test
    fun `should follow the finger on a row a demonstration is not holding`() {
        composeRule.setContent {
            PaperTheme {
                SwipeRow(key = "1", onDelete = {}, staged = { null }) {
                    Text("Groceries", modifier = Modifier.testTag(PULLED_ROW))
                }
            }
        }
        val atRest = composeRule.onNodeWithTag(PULLED_ROW).fetchSemanticsNode().positionInRoot.x

        composeRule.onNodeWithTag(PULLED_ROW).performTouchInput {
            down(center)
            moveTo(center + Offset(-PULL_PIXELS, 0f))
        }
        composeRule.waitForIdle()

        val pulled = composeRule.onNodeWithTag(PULLED_ROW).fetchSemanticsNode().positionInRoot.x
        assertTrue("rested at $atRest, pulled to $pulled", pulled < atRest)
    }

    @Test
    fun `should let a demonstration hold a row aside with no finger on it`() {
        val held = mutableStateOf<Float?>(null)
        composeRule.setContent {
            PaperTheme {
                SwipeRow(key = "1", onDelete = {}, staged = { held.value }) {
                    Text("Groceries", modifier = Modifier.testTag(PULLED_ROW))
                }
            }
        }
        val atRest = composeRule.onNodeWithTag(PULLED_ROW).fetchSemanticsNode().positionInRoot.x

        composeRule.runOnIdle { held.value = -PULL_PIXELS }
        composeRule.waitForIdle()

        val pulled = composeRule.onNodeWithTag(PULLED_ROW).fetchSemanticsNode().positionInRoot.x
        assertTrue("rested at $atRest, held at $pulled", pulled < atRest)
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

    /**
     * The pad is still standing there with a sheet gone off it, so the sheet now
     * showing is what answers a press — and it says the opposite thing, because the
     * only thing left to do about a sheet already taken is to put it back.
     */
    @Test
    fun `should let the sheet now showing take back the one that was handed over`() {
        var putBack = false
        composeRule.setContent {
            PaperTheme {
                StickyNotePad(
                    onTake = {},
                    contentDescription = "add",
                    taken = true,
                    putBack = StickyNotePutBack(
                        painter = painterResource(R.drawable.ic_remove),
                        contentDescription = "put back",
                        onPress = { putBack = true }
                    )
                )
            }
        }

        composeRule.onNodeWithContentDescription("put back").performClick()
        composeRule.waitForIdle()

        assertTrue(putBack)
    }

    @Test
    fun `should offer nothing to put back while the pad is whole`() {
        composeRule.setContent {
            PaperTheme {
                StickyNotePad(
                    onTake = {},
                    contentDescription = "add",
                    putBack = StickyNotePutBack(
                        painter = painterResource(R.drawable.ic_remove),
                        contentDescription = "put back",
                        onPress = {}
                    )
                )
            }
        }

        composeRule.onNodeWithContentDescription("put back").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("add").assertIsDisplayed()
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
    @Config(qualifiers = "night")
    fun `should hand the night sheet to everything drawn after dark`() {
        val captured = mutableListOf<PaperPalette>()
        composeRule.setContent {
            PaperTheme { captured += LocalPaperPalette.current }
        }
        composeRule.waitForIdle()

        assertEquals(PaperPalette.night, captured.single())
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
