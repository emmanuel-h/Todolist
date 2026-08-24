package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun CountBadge(
    painter: Painter,
    count: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalPaperPalette.current.pencil,
    borderColor: Color = LocalPaperPalette.current.pencil
) {
    Row(
        modifier = modifier
            .border(PaperDimens.rule, borderColor, CircleShape)
            .defaultMinSize(minWidth = PaperDimens.badgeMinWidth)
            .padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        InkIcon(
            painter = painter,
            contentDescription = null,
            tint = tint,
            size = PaperDimens.badgeIcon
        )
        Text(
            text = count.toString(),
            style = PaperType.caption,
            color = tint
        )
    }
}
