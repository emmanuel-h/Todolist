package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R

private val TAB_WIDTH = 44.dp
private val PERFORATION_HEIGHT = 26.dp
private val PERFORATION_STROKE = 1.5.dp
private val PERFORATION_GAP = 8.dp
private const val PERFORATION_DOTS = 5
private const val DOT_OF_GAP = 0.45f

/**
 * The perforated corner of a row, with the trash drawn on it.
 *
 * A row could always be torn off by dragging it away, but nothing at rest said so:
 * the trash appeared only once the finger was already moving, so the gesture had to
 * be guessed before it could be seen, and a list written by mistake read as
 * permanent. The corner is that same tear standing still — the perforation says
 * where the paper comes away and the trash says what happens when it does.
 *
 * Pressing it tears the row off exactly as the drag does, so the tear-off and the
 * slip that offers the tear back are the ones already there.
 */
@Composable
fun TearTab(onTear: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    val palette = LocalPaperPalette.current
    val ink = palette.inked(InkTone.Margin)
    Row(
        modifier = modifier
            .width(TAB_WIDTH)
            .clickable(onClickLabel = contentDescription, role = Role.Button, onClick = onTear)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.Top
    ) {
        OnRuleSlot {
            Canvas(
                modifier = Modifier
                    .width(PERFORATION_STROKE)
                    .height(PERFORATION_HEIGHT)
            ) {
                val step = size.height / PERFORATION_DOTS
                val dot = step * DOT_OF_GAP
                repeat(PERFORATION_DOTS) { at ->
                    val top = at * step + (step - dot) / 2f
                    drawLine(
                        color = ink,
                        start = Offset(size.width / 2f, top),
                        end = Offset(size.width / 2f, top + dot),
                        strokeWidth = size.width,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        Spacer(Modifier.width(PERFORATION_GAP))
        OnRuleSlot {
            InkIcon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                tint = ink,
                size = PaperDimens.jotGlyph
            )
        }
    }
}
