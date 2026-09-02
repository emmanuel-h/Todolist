package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A control written on a rule is one rule tall, and a rule is smaller than a
 * finger. This lets it stay one rule tall to everything that measures it while
 * being a whole touch target to everything that presses it: the box grows down
 * into the blank rule the row already carries under its writing, and the row's
 * height — counted in whole rules from the writing alone — does not notice.
 *
 * Flooring the height instead made the control taller than a rule, which pushed
 * the row to a second rule of writing it did not have, and every row on the page
 * grew by half again.
 */
fun Modifier.pressableBelowTheRule(onRule: Boolean): Modifier =
    if (!onRule) this else this
        .wrapContentHeight(Alignment.Top, unbounded = true)
        .heightIn(min = PaperDimens.touchTarget)

fun Modifier.paperRuling(
    pitch: Dp,
    color: Color,
    gutter: Dp = PaperDimens.gutter
): Modifier = drawWithCache {
    val thickness = PaperDimens.rule.toPx()
    val bare = gutter.toPx()
    // A DrawScope does not mirror: the gutter is bare paper on the row's start
    // edge, which is the right one in a right-to-left hand.
    val start = if (layoutDirection == LayoutDirection.Rtl) 0f else bare
    val step = pitch.toPx().coerceAtLeast(thickness)
    onDrawBehind {
        var line = size.height - thickness
        while (line > -thickness) {
            drawRect(
                color = color,
                topLeft = Offset(start, line),
                size = Size(size.width - bare, thickness)
            )
            line -= step
        }
    }
}

@Composable
fun RuledRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val pitch = LocalPagePitch.current
    val clickModifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .pitchHeight(pitch, extraRules = 1)
            .padding(start = LocalPaperGutter.current, end = PaperDimens.rowEndPadding),
        verticalAlignment = Alignment.Top,
        content = content
    )
}
