package fr.mandarine.todolist.ui.tutorial

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
    fun putBounds(anchor: TutorialAnchor, bounds: TutorialBounds)
    fun removeBounds(anchor: TutorialAnchor)
    fun boundsOf(anchor: TutorialAnchor): TutorialBounds?
}

class TutorialAnchors : TutorialAnchorHost {

    private val bounds = mutableMapOf<TutorialAnchor, TutorialBounds>()

    override fun putBounds(anchor: TutorialAnchor, bounds: TutorialBounds) {
        this.bounds[anchor] = bounds
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

@Composable
fun Modifier.tutorialAnchor(host: TutorialAnchorHost, anchor: TutorialAnchor): Modifier {
    val root = LocalView.current
    DisposableEffect(host, anchor) {
        onDispose { host.removeBounds(anchor) }
    }
    return onGloballyPositioned { coordinates ->
        host.putBounds(anchor, coordinates.screenBounds(root))
    }
}
