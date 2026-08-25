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
                .widthIn(min = PaperDimens.marginColumn)
                .seatOnRule(),
            style = LocalRuledHand.current.margin,
            color = LocalPaperPalette.current.inked(InkTone.Margin),
            textAlign = TextAlign.End,
            softWrap = false,
            maxLines = ONE_LINE
        )
    }
}
