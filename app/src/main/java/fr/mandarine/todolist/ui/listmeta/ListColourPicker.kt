package fr.mandarine.todolist.ui.listmeta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.domain.ListColour
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.circledInInk
import fr.mandarine.todolist.ui.paper.highlightWash
import fr.mandarine.todolist.ui.paper.inked

private val SWATCH_TOUCH = 36.dp
private val SWATCH_RING = 26.dp
private val SWATCH_STROKE = 1.dp

/**
 * Seven swatches in a row — the six named hues and [ListColour.None] (bare paper).
 * On a sheet too narrow for seven full swatches they shrink together rather than
 * pushing the last one off the edge.
 * The chosen one is circled in ink. None draws a hairline border so it reads as a
 * real choice on the sheet rather than an empty slot.
 *
 * It is held out wherever a list's colour is shown or asked about: on the slip the
 * card's ⋯ opens, and on the slip the opened list's tape opens. It used to sit only
 * at the foot of the rename sheet, behind a line that had to say "or colour" for
 * anyone to find it.
 */
@Composable
fun ListColourPicker(
    selected: ListColour,
    onSelect: (ListColour) -> Unit,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ListColour.entries.forEach { colour ->
            ColourSwatch(
                colour = colour,
                selected = colour == selected,
                onSelect = { onSelect(colour) },
                animated = animated,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

@Composable
private fun ColourSwatch(
    colour: ListColour,
    selected: Boolean,
    onSelect: () -> Unit,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalPaperPalette.current
    val label = stringResource(colourStringRes(colour))
    val swatchColour = if (colour == ListColour.None) palette.paper else palette.highlightWash(colour)
    val borderColour = palette.pencil
    val ringColour = palette.inked(InkTone.Acted)
    val isNone = colour == ListColour.None

    Box(
        modifier = modifier
            .widthIn(max = SWATCH_TOUCH)
            .aspectRatio(1f)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(SWATCH_RING / SWATCH_TOUCH)
                .drawWithCache {
                    val strokeWidth = SWATCH_STROKE.toPx()
                    onDrawBehind {
                        drawCircle(color = swatchColour)
                        if (isNone) {
                            drawCircle(
                                color = borderColour,
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }
                }
                .circledInInk(
                    circled = selected,
                    seed = colour.ordinal,
                    color = ringColour,
                    animated = animated
                ),
            contentAlignment = Alignment.Center
        ) { }
    }
}

private fun colourStringRes(colour: ListColour): Int = when (colour) {
    ListColour.None -> R.string.colour_none
    ListColour.Butter -> R.string.colour_butter
    ListColour.Mint -> R.string.colour_mint
    ListColour.Rose -> R.string.colour_rose
    ListColour.Sky -> R.string.colour_sky
    ListColour.Peach -> R.string.colour_peach
    ListColour.Lilac -> R.string.colour_lilac
}
