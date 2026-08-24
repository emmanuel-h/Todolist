package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import fr.mandarine.todolist.R

@Composable
fun GhostRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    painter: Painter = painterResource(R.drawable.ic_add),
    hint: String = stringResource(R.string.add_item_ghost_hint),
    tint: Color = LocalPaperPalette.current.inkBlue
) {
    RuledRow(modifier = modifier, onClick = onClick) {
        OnRuleSlot(Modifier.width(PaperDimens.iconButton)) {
            InkIcon(painter = painter, contentDescription = null, tint = tint)
        }
        Text(
            text = hint,
            modifier = Modifier.seatOnRule(MaterialTheme.typography.bodyMedium),
            style = MaterialTheme.typography.bodyMedium,
            color = tint
        )
    }
}
