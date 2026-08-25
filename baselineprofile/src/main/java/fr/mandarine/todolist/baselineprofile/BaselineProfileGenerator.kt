package fr.mandarine.todolist.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TUTORIAL_ARMS_MILLIS = 2_000L
private const val PAGE_SETTLES_MILLIS = 1_000L
private const val FIELD_WAIT_MILLIS = 3_000L
private const val LISTS_WRITTEN = 12
private const val ITEMS_WRITTEN = 18
private const val PAD_ACROSS = 0.89f
private const val PAD_DOWN = 0.925f
private const val FIRST_ROW_ACROSS = 0.4f
private const val FIRST_ROW_DOWN = 0.12f
private const val FLINGS = 2
private const val GESTURE_MARGIN = 5

/**
 * The journey the profile is cut from: the app opens onto its page, a page's worth
 * of lists is written on it, the page is flung both ways, one list is opened and
 * filled, and its items are flung too. Everything a first launch and a first
 * scroll touch is therefore already compiled by the time a reader does it.
 *
 * Run on a connected device with `./gradlew :app:generateBaselineProfile`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun openThePageAndWriteOnIt() = baselineProfileRule.collect(
        packageName = PACKAGE,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
        SystemClock.sleep(TUTORIAL_ARMS_MILLIS)
        device.pressBack()
        device.waitForIdle()

        startActivityAndWait()
        device.waitForIdle()

        device.takeASheetFromThePad()
        device.write(LISTS_WRITTEN)
        device.putThePenDown()
        device.flingThePage()

        device.openTheFirstRow()
        device.write(ITEMS_WRITTEN)
        device.putThePenDown()
        device.flingThePage()

        device.pressBack()
        device.waitForIdle()
    }
}

private fun UiDevice.takeASheetFromThePad() {
    click((displayWidth * PAD_ACROSS).toInt(), (displayHeight * PAD_DOWN).toInt())
    waitForIdle()
}

/**
 * The words go in through the keyboard rather than through the node, because the
 * line commits and recomposes on every Enter and a node handle held across that
 * goes stale.
 */
private fun UiDevice.write(lines: Int) {
    wait(Until.findObject(By.pkg(PACKAGE).clazz(EDIT_TEXT)), FIELD_WAIT_MILLIS) ?: return
    repeat(lines) { line ->
        executeShellCommand("input text $WRITTEN_LINE${line + 1}")
        pressEnter()
        waitForIdle()
    }
}

private fun UiDevice.putThePenDown() {
    pressBack()
    waitForIdle()
    SystemClock.sleep(PAGE_SETTLES_MILLIS)
}

private fun UiDevice.flingThePage() {
    val page = findObject(By.pkg(PACKAGE).scrollable(true)) ?: return
    page.setGestureMargin(displayWidth / GESTURE_MARGIN)
    repeat(FLINGS) {
        page.fling(Direction.DOWN)
        waitForIdle()
    }
    repeat(FLINGS) {
        page.fling(Direction.UP)
        waitForIdle()
    }
}

/**
 * The pen is already down by the time this runs, so the add line is folded away
 * and the first list is the first thing on the page.
 */
private fun UiDevice.openTheFirstRow() {
    val across = (displayWidth * FIRST_ROW_ACROSS).toInt()
    val down = (displayHeight * FIRST_ROW_DOWN).toInt()
    click(across, down)
    waitForIdle()
    SystemClock.sleep(PAGE_SETTLES_MILLIS)
}

private const val EDIT_TEXT = "android.widget.EditText"
private const val WRITTEN_LINE = "Ligne"
