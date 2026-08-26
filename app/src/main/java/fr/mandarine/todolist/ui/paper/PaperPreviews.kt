package fr.mandarine.todolist.ui.paper

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import kotlinx.coroutines.delay

private const val PREVIEW_PAPER = 0xFFFAF5EA
private const val PREVIEW_NIGHT_PAPER = 0xFF201D19
private const val PREVIEW_TAKEN_MILLIS = 900L
private const val PREVIEW_APPLES = "🍎 Apples"
private const val PREVIEW_GROCERIES = "🛒 Groceries"
private const val PREVIEW_OPEN_COUNT = "3"

@Preview(showBackground = true, backgroundColor = PREVIEW_PAPER, heightDp = 320)
@Composable
internal fun PaperSurfacePreview() {
    PaperTheme {
        PaperSurface(Modifier.height(320.dp)) {}
    }
}

@Preview(showBackground = true, backgroundColor = PREVIEW_PAPER)
@Composable
internal fun RuledRowPreview() {
    PaperTheme {
        PaperSurface(Modifier.height(200.dp)) {
            Column {
                RuledRow {
                    InkRing(
                        checked = false,
                        onToggle = {},
                        seed = 1,
                        contentDescription = stringResource(R.string.item_mark_completed),
                        stateDescription = stringResource(R.string.item_state_active)
                    )
                    Text(
                        text = PREVIEW_APPLES,
                        modifier = Modifier.weight(1f).seatOnRule(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = LocalPaperPalette.current.inked(InkTone.Words)
                    )
                }
                RuledRow(onClick = {}) {
                    Text(
                        text = PREVIEW_GROCERIES,
                        modifier = Modifier.weight(1f).seatOnRule(),
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalPaperPalette.current.inked(InkTone.Words)
                    )
                    Text(
                        text = PREVIEW_OPEN_COUNT,
                        modifier = Modifier
                            .width(PaperDimens.marginColumn)
                            .seatOnRule(),
                        style = LocalRuledHand.current.margin,
                        color = LocalPaperPalette.current.inked(InkTone.Margin),
                        textAlign = TextAlign.End
                    )
                }
                RuledRow {}
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = PREVIEW_PAPER)
@Composable
internal fun InkRingPreview() {
    PaperTheme {
        PaperSurface(Modifier.height(200.dp)) {
            var checked by remember { mutableStateOf(false) }
            Column {
                RuledRow {
                    InkRing(
                        checked = checked,
                        onToggle = { checked = !checked },
                        seed = 7,
                        contentDescription = stringResource(R.string.item_mark_completed),
                        stateDescription = stringResource(R.string.item_state_active)
                    )
                }
                RuledRow {
                    InkRing(
                        checked = true,
                        onToggle = {},
                        seed = 11,
                        contentDescription = stringResource(R.string.item_mark_incomplete),
                        stateDescription = stringResource(R.string.item_state_completed),
                        animated = false
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = PREVIEW_PAPER)
@Composable
internal fun InkRolesPreview() {
    PaperTheme {
        val palette = LocalPaperPalette.current
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = PREVIEW_APPLES, color = palette.inked(InkTone.Words))
            Text(text = PREVIEW_OPEN_COUNT, color = palette.inked(InkTone.Margin), style = LocalRuledHand.current.margin)
            Text(text = PREVIEW_GROCERIES, color = palette.inked(InkTone.Acted))
            Text(text = PREVIEW_APPLES, color = palette.inked(InkTone.Lost))
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = PREVIEW_NIGHT_PAPER,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    heightDp = 320
)
@Composable
internal fun PaperSurfaceByLamplightPreview() {
    PaperTheme {
        PaperSurface(Modifier.height(320.dp)) {}
    }
}

@Preview(
    showBackground = true,
    backgroundColor = PREVIEW_NIGHT_PAPER,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
internal fun InkRolesByLamplightPreview() {
    PaperTheme {
        val palette = LocalPaperPalette.current
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = PREVIEW_APPLES, color = palette.inked(InkTone.Words))
            Text(
                text = PREVIEW_OPEN_COUNT,
                color = palette.inked(InkTone.Margin),
                style = LocalRuledHand.current.margin
            )
            Text(text = PREVIEW_GROCERIES, color = palette.inked(InkTone.Acted))
            Text(text = PREVIEW_APPLES, color = palette.inked(InkTone.Lost))
        }
    }
}

@Preview(showBackground = true, backgroundColor = PREVIEW_PAPER)
@Composable
internal fun InkAddLinePreview() {
    PaperTheme {
        PaperSurface(Modifier.height(160.dp)) {
            Column {
                InkAddLine(
                    text = "",
                    onTextChange = {},
                    onCommit = {},
                    armed = false,
                    onPenUp = {},
                    onPenDown = {},
                    spoken = ""
                )
                InkAddLine(
                    text = PREVIEW_GROCERIES,
                    onTextChange = {},
                    onCommit = {},
                    armed = false,
                    onPenUp = {},
                    onPenDown = {},
                    spoken = ""
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = PREVIEW_PAPER)
@Composable
internal fun InkIconPreview() {
    PaperTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InkIcon(
                painter = painterResource(R.drawable.ic_event),
                contentDescription = null,
                tint = LocalPaperPalette.current.inked(InkTone.Margin)
            )
            InkIconButton(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.set_target_date),
                onClick = {}
            )
            InkIconButton(
                painter = painterResource(R.drawable.ic_alarm),
                contentDescription = stringResource(R.string.set_due_date),
                onClick = {},
                tint = LocalPaperPalette.current.inked(InkTone.Lost),
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = PREVIEW_PAPER)
@Composable
internal fun StickyNotePadPreview() {
    PaperTheme {
        var taken by remember { mutableStateOf(false) }
        LaunchedEffect(taken) {
            if (taken) {
                delay(PREVIEW_TAKEN_MILLIS)
                taken = false
            }
        }
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StickyNotePad(
                onTake = { taken = !taken },
                contentDescription = stringResource(R.string.add_list_fab_description),
                taken = taken
            )
            StickyNotePad(
                onTake = {},
                contentDescription = stringResource(R.string.add_list_fab_description),
                taken = true
            )
        }
    }
}
