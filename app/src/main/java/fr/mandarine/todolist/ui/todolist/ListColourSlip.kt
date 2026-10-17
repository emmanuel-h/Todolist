package fr.mandarine.todolist.ui.todolist

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.ui.listmeta.ListColourPicker
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.PaperDialog
import fr.mandarine.todolist.ui.paper.PaperType
import fr.mandarine.todolist.ui.paper.handwritten
import fr.mandarine.todolist.ui.paper.inked

private const val TWO_LINES = 2
private val NAME_TO_SWATCHES = 12.dp

/**
 * The roll of tape the opened list is pinned with, held out at the foot of the
 * page: the list's name and the seven swatches. Pressing the tape is asking what
 * colour it should be, so one swatch answers it and the slip is put down.
 */
@Composable
fun ListColourSlip(
    listName: String,
    colour: ListColour,
    animated: Boolean,
    onDismiss: () -> Unit,
    onRecolour: (ListColour) -> Unit
) {
    val palette = LocalPaperPalette.current
    PaperDialog(onDismissRequest = onDismiss, alignment = Alignment.BottomCenter) {
        Text(
            text = remember(listName) { handwritten(listName) },
            style = PaperType.field,
            color = palette.inked(InkTone.Words),
            maxLines = TWO_LINES,
            overflow = TextOverflow.Ellipsis
        )
        ListColourPicker(
            selected = colour,
            onSelect = onRecolour,
            animated = animated,
            modifier = Modifier.padding(top = NAME_TO_SWATCHES)
        )
    }
}
