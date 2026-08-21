package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import kotlinx.coroutines.delay

private const val PREVIEW_PAPER = 0xFFFAF5EA
private const val PREVIEW_TAKEN_MILLIS = 900L

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
                RuledRow(onClick = {}) {
                    InkIcon(
                        painter = painterResource(R.drawable.ic_drag_handle),
                        contentDescription = stringResource(R.string.drag_handle),
                        tint = PaperInk.pencil
                    )
                    Text(
                        text = "🍎 Apples",
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = PaperInk.ink
                    )
                    InkIconButton(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.item_mark_completed),
                        onClick = {},
                        tint = PaperInk.inkBlue
                    )
                    InkIconButton(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.item_delete),
                        onClick = {},
                        tint = PaperInk.inkRedSoft
                    )
                }
                RuledRow(minHeight = PaperDimens.listRowHeight) {
                    Text(
                        text = "🛒 Groceries",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = PaperInk.ink
                    )
                    CountBadge(
                        painter = painterResource(R.drawable.ic_radio_button_unchecked),
                        count = 3
                    )
                    Spacer(Modifier.width(4.dp))
                    CountBadge(
                        painter = painterResource(R.drawable.ic_check_circle),
                        count = 12,
                        tint = PaperInk.pencil,
                        borderColor = PaperInk.rule
                    )
                }
                RuledRow {}
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = PREVIEW_PAPER)
@Composable
internal fun GhostRowPreview() {
    PaperTheme {
        PaperSurface(Modifier.height(160.dp)) {
            Column {
                GhostRow(onClick = {})
                GhostRow(
                    onClick = {},
                    minHeight = PaperDimens.ghostRowCollapsedHeight,
                    tint = PaperInk.pencil
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = PREVIEW_PAPER)
@Composable
internal fun CountBadgePreview() {
    PaperTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountBadge(painterResource(R.drawable.ic_radio_button_unchecked), 0)
            CountBadge(painterResource(R.drawable.ic_radio_button_unchecked), 7)
            CountBadge(
                painter = painterResource(R.drawable.ic_check_circle),
                count = 128,
                tint = PaperInk.pencil,
                borderColor = PaperInk.rule
            )
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
                tint = PaperInk.inkBlue
            )
            InkIconButton(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.item_edit),
                onClick = {}
            )
            InkIconButton(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.item_delete),
                onClick = {},
                tint = PaperInk.inkRedSoft,
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
