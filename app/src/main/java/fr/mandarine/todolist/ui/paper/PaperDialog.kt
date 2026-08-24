package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * A sheet laid on the page rather than a card floating above it: square corners,
 * a hairline rule for an edge, and no elevation — the paper design has no drop
 * shadows, which is the one thing a Material dialog surface always draws.
 */
@Composable
fun PaperDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalPaperPalette.current
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .paperSheet(tone = palette.paperShade)
                .border(PaperDimens.rule, palette.rule)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            content = content
        )
    }
}
