package fr.mandarine.todolist.ui.listmeta

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.InkProgress
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.inked

private const val ONE_LINE = 1
private val STROKE_TO_COUNT = 8.dp
private val ROW_STROKE = 44.dp

/**
 * How far through a list the reader is: a pen line laid over a pencil one, and the
 * tally of ticked against written beside it in print. A list with nothing written
 * on it yet draws nothing at all — there is no progress to speak of, and an empty
 * pencil line reads as a list the reader has failed at rather than one not begun.
 *
 * It is read out as one phrase ("2 of 5 done") rather than as a bar and a fraction.
 */
@Composable
fun ListProgress(
    done: Int,
    total: Int,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    strokeWidth: Dp = ROW_STROKE,
    style: TextStyle = PaperType.meta,
    countText: String = stringResource(R.string.progress_fraction, done, total)
) {
    if (total <= 0) return
    val spoken = pluralStringResource(R.plurals.progress_done, total, done, total)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically
    ) {
        InkProgress(
            done = done,
            total = total,
            modifier = Modifier.width(strokeWidth),
            animated = animated
        )
        Spacer(Modifier.width(STROKE_TO_COUNT))
        Text(
            text = countText,
            style = style,
            color = LocalPaperPalette.current.inked(InkTone.Margin),
            softWrap = false,
            maxLines = ONE_LINE
        )
    }
}
