package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val NO_TAIL = 0.dp

/**
 * A row torn off the foot of the page would drag the whole page down with it: the
 * scroll has nowhere left to go, so the viewport clamps by exactly the height the
 * row gave back. The page keeps that height as blank paper instead, and gives it
 * up again only where doing so cannot move anything the reader is looking at —
 * once the foot of the page is off screen, or once the page is back at its head.
 */
@Stable
class PageTail(private val step: Dp) {

    private var absorbed by mutableStateOf(emptySet<String>())

    val height: Dp get() = step * absorbed.size

    fun absorb(id: String) {
        absorbed = absorbed + id
    }

    fun release(id: String) {
        if (id in absorbed) absorbed = absorbed - id
    }

    fun settle() {
        if (absorbed.isNotEmpty()) absorbed = emptySet()
    }
}

@Composable
fun rememberPageTail(listState: LazyListState, step: Dp): PageTail {
    val tail = remember(step) { PageTail(step) }
    LaunchedEffect(tail, listState) {
        snapshotFlow { tail.height > NO_TAIL && listState.footIsOutOfSight() }
            .collect { spare -> if (spare) tail.settle() }
    }
    return tail
}

internal fun LazyListState.footIsOutOfSight(): Boolean {
    if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) return true
    val visible = layoutInfo.visibleItemsInfo
    val last = visible.lastOrNull() ?: return true
    return last.index < layoutInfo.totalItemsCount - 1
}
