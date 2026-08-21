package fr.mandarine.todolist.ui.todolist

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.domain.TutorialAnchor
import fr.mandarine.todolist.presentation.TutorialBounds
import kotlin.math.roundToInt

class TodoListScreenState {

    private val anchorBounds = mutableMapOf<TutorialAnchor, TutorialBounds>()

    var addRowExpanded by mutableStateOf(false)

    var addRowText by mutableStateOf("")

    var editingItemId by mutableStateOf<String?>(null)

    var previewOrder by mutableStateOf<List<String>?>(null)

    var animationsEnabled by mutableStateOf(true)

    var hideKeyboardSignal by mutableStateOf(0)
        private set

    fun putBounds(anchor: TutorialAnchor, bounds: TutorialBounds) {
        anchorBounds[anchor] = bounds
    }

    fun removeBounds(anchor: TutorialAnchor) {
        anchorBounds.remove(anchor)
    }

    fun boundsOf(anchor: TutorialAnchor): TutorialBounds? = anchorBounds[anchor]

    fun requestHideKeyboard() {
        hideKeyboardSignal += 1
    }
}

fun orderActive(items: List<TodoItem>, order: List<String>?): List<TodoItem> {
    if (order == null) return items
    val byId = items.associateBy { it.id }
    val ordered = order.mapNotNull { byId[it] }
    return if (ordered.size == items.size) ordered else items
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
internal fun Modifier.tutorialAnchor(
    state: TodoListScreenState,
    anchor: TutorialAnchor
): Modifier {
    val root = LocalView.current
    DisposableEffect(state, anchor) {
        onDispose { state.removeBounds(anchor) }
    }
    return onGloballyPositioned { coordinates ->
        state.putBounds(anchor, coordinates.screenBounds(root))
    }
}
