package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

fun Modifier.paperRuling(
    pitch: Dp,
    color: Color,
    gutter: Dp = PaperDimens.gutter
): Modifier = drawWithCache {
    val thickness = PaperDimens.rule.toPx()
    val start = gutter.toPx()
    val step = pitch.toPx().coerceAtLeast(thickness)
    onDrawBehind {
        var line = size.height - thickness
        while (line > -thickness) {
            drawRect(
                color = color,
                topLeft = Offset(start, line),
                size = Size(size.width - start, thickness)
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
            .pitchHeight(pitch)
            .heightIn(min = pitch)
            .padding(start = LocalPaperGutter.current, end = PaperDimens.rowEndPadding),
        verticalAlignment = Alignment.Top,
        content = content
    )
}
