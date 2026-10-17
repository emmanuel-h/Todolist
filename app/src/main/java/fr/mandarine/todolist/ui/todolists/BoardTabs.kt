package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.paperSheet

private val BAR_SHADOW = 8.dp
private const val BAR_SHADOW_ALPHA = 0.25f
private val BAR_WIDTH = 480.dp
private val UNDERLINE_WIDTH = 36.dp
private val UNDERLINE_HEIGHT = 6.dp
private val UNDERLINE_STROKE = 2.dp
private val TAB_GAP = 2.dp

/** How tall the bar at the foot of the window stands, above the navigation bar. */
val BoardBarHeight: Dp = 64.dp

/**
 * The strip of paper along the foot of the window, with the two things the
 * notebook is: the board of lists, and the calendar the dated ones fall on. Lists
 * come first, because most lists have no day and the board is where they live.
 *
 * The open tab is written in ink, set a little heavier and underlined by hand; the
 * other waits in pencil.
 */
@Composable
fun BoardTabBar(
    selected: BoardTab,
    onSelect: (BoardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPaperPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(RectangleShape) {
                radius = BAR_SHADOW.toPx()
                alpha = BAR_SHADOW_ALPHA
                color = palette.shadow
            }
            .paperSheet(tone = palette.paperShade, lit = palette.paperSheet)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = BAR_WIDTH)
                .fillMaxWidth()
                .height(BoardBarHeight)
                .selectableGroup()
        ) {
            BoardTabItem(
                label = stringResource(R.string.tab_lists),
                painter = painterResource(R.drawable.ic_board),
                selected = selected == BoardTab.Lists,
                onClick = { onSelect(BoardTab.Lists) }
            )
            BoardTabItem(
                label = stringResource(R.string.tab_calendar),
                painter = painterResource(R.drawable.ic_event),
                selected = selected == BoardTab.Calendar,
                onClick = { onSelect(BoardTab.Calendar) }
            )
        }
    }
}

@Composable
private fun RowScope.BoardTabItem(
    label: String,
    painter: Painter,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalPaperPalette.current
    val ink = palette.inked(if (selected) InkTone.Words else InkTone.Margin)
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TAB_GAP, Alignment.CenterVertically)
    ) {
        InkIcon(painter = painter, contentDescription = null, tint = ink, size = PaperDimens.iconGlyph)
        Text(
            text = label,
            style = PaperType.tabLabel.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = ink,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .size(UNDERLINE_WIDTH, UNDERLINE_HEIGHT)
                .drawWithCache {
                    val stroke = Stroke(width = UNDERLINE_STROKE.toPx(), cap = StrokeCap.Round)
                    val path = Path().apply {
                        moveTo(stroke.width, size.height * 0.7f)
                        cubicTo(
                            size.width * 0.3f, size.height * 0.15f,
                            size.width * 0.65f, size.height * 0.2f,
                            size.width - stroke.width, size.height * 0.45f
                        )
                    }
                    onDrawBehind { if (selected) drawPath(path, ink, style = stroke) }
                }
        )
    }
}
