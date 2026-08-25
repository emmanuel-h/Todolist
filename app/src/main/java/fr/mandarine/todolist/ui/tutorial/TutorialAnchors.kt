package fr.mandarine.todolist.ui.tutorial

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TutorialBounds
import kotlin.math.roundToInt

/**
 * The phantom hand is drawn in a View overlay above the whole window, so it needs
 * screen coordinates that no composable owns. Every anchored composable reports
 * its own, and drops them again when it leaves the composition.
 */
interface TutorialAnchorHost {
    var recordingAnchors: Boolean
    fun putBounds(anchor: TutorialAnchor, bounds: TutorialBounds)
    fun removeBounds(anchor: TutorialAnchor)
    fun boundsOf(anchor: TutorialAnchor): TutorialBounds?
}

/**
 * The bounds are snapshot state so the overlay can watch the thing it is pointing
 * at: a row that leaves the page takes the phantom hand with it. Layout reports
 * the same rectangle on every pass, so only a rectangle that actually moved is
 * written back.
 */
class TutorialAnchors : TutorialAnchorHost {

    private val bounds = mutableStateMapOf<TutorialAnchor, TutorialBounds>()

    override var recordingAnchors by mutableStateOf(false)

    override fun putBounds(anchor: TutorialAnchor, bounds: TutorialBounds) {
        if (this.bounds[anchor] != bounds) this.bounds[anchor] = bounds
    }

    override fun removeBounds(anchor: TutorialAnchor) {
        bounds.remove(anchor)
    }

    override fun boundsOf(anchor: TutorialAnchor): TutorialBounds? = bounds[anchor]
}

internal fun LayoutCoordinates.screenBounds(root: View): TutorialBounds {
    val location = IntArray(2)
    root.getLocationOnScreen(location)
    val position = positionInRoot()
    return TutorialBounds(
        left = location[0] + position.x.roundToInt(),
        top = location[1] + position.y.roundToInt(),
        width = size.width,
        height = size.height
    )
}

/**
 * Only the demo ever reads a rectangle, and reporting one costs a layout callback,
 * a screen-coordinate lookup and a snapshot write on every anchored row of every
 * frame the page moves. So nothing is measured while nothing is watching: the
 * anchors are live for as long as the demo is, and the page scrolls unwatched the
 * rest of the time.
 */
@Composable
fun Modifier.tutorialAnchor(host: TutorialAnchorHost, anchor: TutorialAnchor): Modifier {
    val root = LocalView.current
    val recording = host.recordingAnchors
    DisposableEffect(host, anchor, recording) {
        onDispose { host.removeBounds(anchor) }
    }
    return if (recording) {
        onGloballyPositioned { coordinates ->
            host.putBounds(anchor, coordinates.screenBounds(root))
        }
    } else {
        this
    }
}
