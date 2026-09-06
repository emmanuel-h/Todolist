package fr.mandarine.todolist.ui.listmeta

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.LocalRuledHand
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.TallyRoll
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.seatOnRule

private const val NOTHING_LEFT = 0
private const val NO_TALLY = ""
private const val ONE_LINE = 1

@Composable
/**
 * The tally in the margin: how many items on this list are still open.
 *
 * Drawn as digits only, with no word beside them, and **nothing at all** when the
 * count is zero — a finished list says so with its whole appearance rather than
 * with a `0`. The blank is a real empty string rather than a hidden composable so
 * the margin column keeps its width and the rows below stay aligned.
 *
 * Words are supplied to screen readers only, via `pluralStringResource` (which
 * picks the right grammatical plural for the locale — not just one-versus-many;
 * some languages have more forms). `contentDescription` is set only when there is
 * something to count, so a finished list is silent there too.
 *
 * [TallyRoll] is what animates a change of digits; this composable supplies the
 * text for whichever value the roll is currently showing, which is why the lambda
 * parameter `tally` is used rather than `count` inside it.
 */
fun OpenCount(count: Int, modifier: Modifier = Modifier, animated: Boolean = true) {
    val spoken = pluralStringResource(R.plurals.open_items, count, count)
    TallyRoll(
        count = count,
        modifier = modifier
            .semantics { if (count > NOTHING_LEFT) contentDescription = spoken },
        animated = animated
    ) { tally ->
        Text(
            text = if (tally > NOTHING_LEFT) tally.toString() else NO_TALLY,
            modifier = Modifier
                .then(if (tally > NOTHING_LEFT) Modifier.widthIn(min = PaperDimens.marginColumn) else Modifier)
                .seatOnRule(),
            style = LocalRuledHand.current.margin,
            color = LocalPaperPalette.current.inked(InkTone.Margin),
            textAlign = TextAlign.End,
            softWrap = false,
            maxLines = ONE_LINE
        )
    }
}
