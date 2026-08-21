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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.paperRule(): Modifier = drawWithCache {
    val thickness = PaperDimens.rule.toPx()
    val start = PaperDimens.gutter.toPx()
    onDrawWithContent {
        drawContent()
        drawRect(
            color = PaperInk.rule,
            topLeft = Offset(start, size.height - thickness),
            size = Size(size.width - start, thickness)
        )
    }
}

@Composable
fun RuledRow(
    modifier: Modifier = Modifier,
    minHeight: Dp = PaperDimens.itemRowHeight,
    verticalPadding: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val clickModifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .paperRule()
            .heightIn(min = minHeight)
            .padding(start = PaperDimens.gutter, end = PaperDimens.rowEndPadding)
            .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
