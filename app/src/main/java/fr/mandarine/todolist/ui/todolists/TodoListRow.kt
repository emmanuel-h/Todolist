package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.DueDateStatus
import fr.mandarine.todolist.domain.TodoListSummary
import fr.mandarine.todolist.ui.listmeta.DateJot
import fr.mandarine.todolist.ui.listmeta.ListProgress
import fr.mandarine.todolist.ui.nav.travellingName
import fr.mandarine.todolist.ui.paper.GlyphFoot
import fr.mandarine.todolist.ui.paper.IconSeat
import fr.mandarine.todolist.ui.paper.InkBudget
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.LocalPaperGutter
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PenStrikeState
import fr.mandarine.todolist.ui.paper.RowVerb
import fr.mandarine.todolist.ui.paper.pitchHeight
import fr.mandarine.todolist.ui.paper.fillingTheLine
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.highlightWash
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.penStrike
import fr.mandarine.todolist.ui.paper.rememberPenStrike
import fr.mandarine.todolist.ui.paper.rowVerbs
import fr.mandarine.todolist.ui.paper.seatOnRule
import fr.mandarine.todolist.ui.paper.spokenVerbs
import fr.mandarine.todolist.ui.paper.tearOff

private val NAME_END_GAP = 4.dp
private val PROGRESS_TO_JOT = 16.dp

/**
 * One list on the page of lists, written on two rules: the name on the first, with
 * the one mark that opens everything else that can be done to it at the end of the
 * line, and under it, printed small in the rule a row leaves blank, how far through
 * the list the reader is and the day it is about.
 *
 * The second rule is the row's own blank one. A list with nothing in it and no day
 * leaves it blank, which is the page's old grammar: a row is its writing and one
 * empty rule.
 */
@Composable
fun TodoListRow(
    summary: TodoListSummary,
    animated: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    tearing: Boolean = false,
    onTorn: () -> Unit = {},
    onActionsRequested: (() -> Unit)? = null,
    onRewriteDate: ((DateSelection) -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val palette = LocalPaperPalette.current
    val pitch = LocalPagePitch.current
    val verbs = rowVerbs(
        onMoveUp?.let { RowVerb(stringResource(R.string.move_up), it) },
        onMoveDown?.let { RowVerb(stringResource(R.string.move_down), it) }
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .tearOff(tearing, animated, onTorn)
            .spokenVerbs(verbs)
            .clickable(onClick = onOpen)
            .padding(start = LocalPaperGutter.current, end = PaperDimens.rowEndPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().pitchHeight(pitch),
            verticalAlignment = Alignment.Top
        ) {
            RowName(summary = summary, animated = animated)
            if (onActionsRequested != null) {
                InkIconButton(
                    painter = painterResource(R.drawable.ic_more),
                    contentDescription = stringResource(R.string.list_actions),
                    onClick = onActionsRequested,
                    tint = palette.inked(InkTone.Margin),
                    pressedTint = palette.inked(InkTone.Words),
                    seat = IconSeat.OnRule,
                    foot = GlyphFoot.dots,
                    glyphSize = PaperDimens.rowGlyph,
                    buttonWidth = PaperDimens.touchTarget
                )
            }
        }
        RowMarginalia(summary = summary, animated = animated, onRewriteDate = onRewriteDate)
    }
}

@Composable
private fun RowMarginalia(
    summary: TodoListSummary,
    animated: Boolean,
    onRewriteDate: ((DateSelection) -> Unit)?
) {
    val palette = LocalPaperPalette.current
    val targetDate = summary.list.targetDate
    val dueDate = summary.list.dueDate
    val dueDateStatus = summary.dueDateStatus
    val total = summary.activeCount + summary.completedCount
    Row(
        modifier = Modifier.fillMaxWidth().height(LocalPagePitch.current),
        verticalAlignment = Alignment.Top
    ) {
        if (total > 0) {
            ListProgress(
                done = summary.completedCount,
                total = total,
                modifier = Modifier.seatOnRule().padding(end = PROGRESS_TO_JOT),
                animated = animated
            )
        }
        if (targetDate != null) {
            DateJot(
                date = targetDate,
                kind = DateKind.TARGET,
                showYear = summary.showTargetYear,
                tint = palette.inked(targetTone(summary.isTargetDateElapsed)),
                struck = summary.isTargetDateElapsed,
                onRewrite = onRewriteDate
            )
        }
        if (dueDate != null && dueDateStatus != null) {
            DateJot(
                date = dueDate,
                kind = DateKind.DUE,
                showYear = summary.showDueDateYear,
                tint = palette.inked(dueTone(dueDateStatus)),
                onRewrite = onRewriteDate
            )
        }
    }
}

@Composable
private fun RowScope.RowName(summary: TodoListSummary, animated: Boolean) {
    val palette = LocalPaperPalette.current
    val style = MaterialTheme.typography.titleMedium.fillingTheLine()
    val strike = rememberPenStrike(summary.list.id, summary.allDone, animated)
    val ink = palette.inked(InkBudget.words(summary.allDone))
    val wash = palette.highlightWash(summary.list.colour)
    Text(
        text = remember(summary.list.name) { handwritten(summary.list.name) },
        modifier = Modifier
            .weight(1f)
            .padding(end = NAME_END_GAP)
            .seatOnRule()
            .nameWash(wash, strike)
            .penStrike(strike, ink)
            .travellingName(summary.list.id),
        style = style,
        color = ink,
        onTextLayout = strike::onTextLayout
    )
}

/**
 * A highlighter band behind the name: one filled rectangle per text line, each
 * ending where that line's last glyph ends rather than at the composable's full
 * width. Transparent for [ListColour.None] so no rectangle is drawn at all.
 *
 * Nothing is allocated inside the draw lambda: the layout is read in the cache
 * block so a change to it rebuilds the cache rather than allocating per frame.
 */
private fun Modifier.nameWash(wash: Color, state: PenStrikeState): Modifier {
    if (wash == Color.Transparent) return this
    return drawWithCache {
        val layout = state.layout
        onDrawBehind {
            if (layout == null) return@onDrawBehind
            for (i in 0 until layout.lineCount) {
                val top = layout.getLineTop(i)
                val bottom = layout.getLineBottom(i)
                val right = layout.getLineRight(i).coerceAtMost(size.width)
                if (right <= 0f) continue
                drawRect(
                    color = wash,
                    topLeft = Offset(0f, top),
                    size = Size(right, bottom - top)
                )
            }
        }
    }
}

/**
 * A date the reader set for themselves is marginalia until the day is behind them,
 * and then it is spent like anything else that is done. A due date is the one jot
 * allowed to raise its voice: amber the day it falls, red once it is missed.
 *
 * Spent is carried by the strike through the date rather than by this tone alone:
 * the two greys sit a twentieth of a step apart, which is a distinction only the
 * code can see.
 */
internal fun targetTone(elapsed: Boolean): InkTone =
    if (elapsed) InkTone.Crossed else InkTone.Margin

internal fun dueTone(status: DueDateStatus): InkTone = when (status) {
    DueDateStatus.FUTURE -> InkTone.Margin
    DueDateStatus.TODAY -> InkTone.Today
    DueDateStatus.OVERDUE -> InkTone.Lost
}
