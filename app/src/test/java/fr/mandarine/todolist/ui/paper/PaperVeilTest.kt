package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaperVeilTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val veil = PaperVeil()

    @Test
    fun `should leave the page unveiled while nothing is laid on it`() {
        assertEquals(0f, veil.depth, TOLERANCE)
    }

    @Test
    fun `should veil the page once a sheet is laid on it`() {
        veil.raise()

        assertTrue("page depth ${veil.depth}", veil.depth > 0f)
    }

    @Test
    fun `should veil the page no further when a sheet is laid on a sheet`() {
        veil.raise()
        val oneSheet = veil.depth
        veil.raise()

        assertEquals(oneSheet, veil.depth, TOLERANCE)
    }

    @Test
    fun `should keep the page veiled while the sheet underneath is still there`() {
        veil.raise()
        veil.raise()
        val stacked = veil.depth
        veil.lower()

        assertEquals(stacked, veil.depth, TOLERANCE)
    }

    @Test
    fun `should lift the veil once the last sheet is taken off the page`() {
        veil.raise()
        veil.lower()

        assertEquals(0f, veil.depth, TOLERANCE)
    }

    @Test
    fun `should veil the page while a paper sheet is laid on it`() {
        composeRule.setContent { Page(sheetLaid = true) }

        assertTrue("page depth ${veil.depth}", veil.depth > 0f)
    }

    @Test
    fun `should veil the page only once while two paper sheets are stacked`() {
        var calendarLaid by mutableStateOf(false)
        composeRule.setContent { Page(sheetLaid = true, secondSheetLaid = calendarLaid) }
        val oneSheet = veil.depth

        calendarLaid = true
        composeRule.waitForIdle()

        assertTrue("page depth $oneSheet", oneSheet > 0f)
        assertEquals(oneSheet, veil.depth, TOLERANCE)
    }

    @Test
    fun `should lift the veil off the page when the sheet is put down`() {
        var laid by mutableStateOf(true)
        composeRule.setContent { Page(sheetLaid = laid) }

        laid = false
        composeRule.waitForIdle()

        assertEquals(0f, veil.depth, TOLERANCE)
    }

    @Composable
    private fun Page(sheetLaid: Boolean, secondSheetLaid: Boolean = false) {
        PaperTheme {
            CompositionLocalProvider(LocalPaperVeil provides veil) {
                PaperSurface(Modifier.fillMaxSize()) {
                    if (sheetLaid) {
                        PaperDialog(onDismissRequest = {}) { Box(Modifier.fillMaxSize()) }
                    }
                    if (secondSheetLaid) {
                        PaperDialog(onDismissRequest = {}) { Box(Modifier.fillMaxSize()) }
                    }
                }
            }
        }
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
