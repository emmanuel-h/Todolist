package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign

private const val ONE_LINE = 1

/**
 * The rule between what is left and what is done, carrying the tally in the
 * margin. What it is counting is handed in — both pages draw the same numeral
 * and it means a different noun on each.
 *
 * The description merges: the node always has the numeral under it, so without
 * merging TalkBack walks past the rule and reads the bare digit instead.
 */
@Composable
fun SectionSkip(
    completedCount: Int,
    spoken: String,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    TallyRoll(
        count = completedCount,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = spoken },
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
