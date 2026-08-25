package fr.mandarine.todolist.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val ITERATIONS = 10
private const val FLINGS = 3
private const val GESTURE_MARGIN = 5

/**
 * What the profile is worth, measured rather than assumed: the same cold start and
 * the same fling, once against a package compiled from nothing and once against a
 * package compiled from the profile this module writes.
 *
 * Run with `./gradlew :baselineprofile:connectedBenchmarkAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupWithoutProfile() = startup(CompilationMode.None())

    @Test
    fun startupWithProfile() =
        startup(CompilationMode.Partial(BaselineProfileMode.Require))

    @Test
    fun scrollWithoutProfile() = scroll(CompilationMode.None())

    @Test
    fun scrollWithProfile() = scroll(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.COLD,
        compilationMode = compilationMode
    ) {
        pressHome()
        startActivityAndWait()
    }

    private fun scroll(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = compilationMode
    ) {
        startActivityAndWait()
        val page = device.findObject(By.pkg(PACKAGE).scrollable(true)) ?: return@measureRepeated
        page.setGestureMargin(device.displayWidth / GESTURE_MARGIN)
        repeat(FLINGS) {
            page.fling(Direction.DOWN)
            device.waitForIdle()
        }
        repeat(FLINGS) {
            page.fling(Direction.UP)
            device.waitForIdle()
        }
    }
}
