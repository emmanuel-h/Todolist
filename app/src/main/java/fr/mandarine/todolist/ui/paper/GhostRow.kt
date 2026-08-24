package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import fr.mandarine.todolist.R

private const val GLYPH_EM = 1.4f

@Composable
private fun handGlyphSize(style: TextStyle): Dp =
    with(LocalDensity.current) { (style.fontSize.toPx() * GLYPH_EM).toDp() }

@Composable
fun GhostRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    painter: Painter = painterResource(R.drawable.ic_add),
    hint: String = stringResource(R.string.add_item_ghost_hint),
    tint: Color = LocalPaperPalette.current.inkBlue,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    RuledRow(modifier = modifier, onClick = onClick) {
        OnRuleSlot(Modifier.width(PaperDimens.iconButton), alignment = Alignment.TopCenter) {
            InkIcon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.seatGlyphOnRule(GlyphFoot.plus),
                tint = tint,
                size = handGlyphSize(style)
            )
        }
        Text(
            text = hint,
            modifier = Modifier.seatOnRule(),
            style = style,
            color = tint
        )
    }
}
