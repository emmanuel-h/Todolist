package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
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
    fun `should report the tap on a ghost row and show only the ellipsis hint`() {
        var tapped = false
        composeRule.setContent {
            PaperTheme {
                GhostRow(onClick = { tapped = true }, modifier = Modifier.testTag("ghost"))
            }
        }

        composeRule.onNodeWithText("…").assertIsDisplayed()
        composeRule.onNodeWithTag("ghost").performClick()

        assertTrue(tapped)
    }

    @Test
    fun `should render a count badge as the bare number`() {
        composeRule.setContent {
            PaperTheme {
                CountBadge(
                    painter = painterResource(R.drawable.ic_check_circle),
                    count = 7
                )
            }
        }

        composeRule.onNodeWithText("7").assertIsDisplayed()
    }

    @Test
    fun `should report taps on an ink icon button and refuse them when disabled`() {
        var taps = 0
        composeRule.setContent {
            PaperTheme {
                Row {
                    InkIconButton(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "edit",
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

        composeRule.onNodeWithContentDescription("edit").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("delete").assertIsNotEnabled()

        assertEquals(1, taps)
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
}
