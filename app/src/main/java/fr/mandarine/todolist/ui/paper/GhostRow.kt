package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import fr.mandarine.todolist.R

@Composable
fun GhostRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    painter: Painter = painterResource(R.drawable.ic_add),
    hint: String = stringResource(R.string.add_item_ghost_hint),
    minHeight: Dp = PaperDimens.itemRowHeight,
    tint: Color = PaperInk.inkBlue
) {
    RuledRow(modifier = modifier, minHeight = minHeight, onClick = onClick) {
        Box(
            modifier = Modifier.size(PaperDimens.iconButton),
            contentAlignment = Alignment.Center
        ) {
            InkIcon(painter = painter, contentDescription = null, tint = tint)
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = tint
        )
    }
}
