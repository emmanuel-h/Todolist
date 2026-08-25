package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import fr.mandarine.todolist.R

private const val ONE_LINE = 1

@Composable
fun SectionSkip(completedCount: Int, modifier: Modifier = Modifier, animated: Boolean = true) {
    val spoken = pluralStringResource(R.plurals.done_items, completedCount, completedCount)
    TallyRoll(
        count = completedCount,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = spoken },
        animated = animated
    ) { tally ->
        Text(
            text = tally.toString(),
            modifier = Modifier
                .width(LocalPaperGutter.current)
                .seatOnRule(),
            style = LocalRuledHand.current.margin,
            color = LocalPaperPalette.current.inked(InkTone.Margin),
            textAlign = TextAlign.Center,
            softWrap = false,
            maxLines = ONE_LINE
        )
    }
}
