package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp

private const val DISABLED_TINT_ALPHA = 0.38f

@Composable
fun InkIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = PaperInk.ink,
    size: Dp = PaperDimens.iconGlyph
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}

@Composable
fun InkIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = PaperInk.inkSoft,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(PaperDimens.iconButton),
        enabled = enabled
    ) {
        InkIcon(
            painter = painter,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = DISABLED_TINT_ALPHA)
        )
    }
}
