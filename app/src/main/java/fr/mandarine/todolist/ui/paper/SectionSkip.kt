package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
fun SectionSkip(completedCount: Int, modifier: Modifier = Modifier) {
    val spoken = pluralStringResource(R.plurals.done_items, completedCount, completedCount)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LocalPagePitch.current)
    ) {
        Text(
            text = completedCount.toString(),
            modifier = Modifier
                .width(PaperDimens.gutter)
                .seatOnRule()
                .semantics { contentDescription = spoken },
            style = LocalRuledHand.current.margin,
            color = LocalPaperPalette.current.pencil,
            textAlign = TextAlign.Center,
            softWrap = false,
            maxLines = ONE_LINE
        )
    }
}
