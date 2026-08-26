package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R
import fr.mandarine.todolist.ui.paper.InkIcon
import fr.mandarine.todolist.ui.paper.InkIconButton
import fr.mandarine.todolist.ui.paper.InkTone
import fr.mandarine.todolist.ui.paper.LocalPagePitch
import fr.mandarine.todolist.ui.paper.LocalPaperPalette
import fr.mandarine.todolist.ui.paper.LocalRuledHand
import fr.mandarine.todolist.ui.paper.formatLocale
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.circledInInk
import fr.mandarine.todolist.ui.paper.inked
import fr.mandarine.todolist.ui.paper.seatOnRule
import java.util.Locale

private const val TARGET_RING_SEED = 0x7A26
private const val DUE_RING_SEED = 0x5C11
private const val ONE_LINE = 1
private val RING_BOX = 30.dp
private val KIND_GLYPH = 20.dp
private val DATE_GAP = 8.dp

/**
 * The marks a date wears on a rule, wherever that rule is: the two kinds as
 * glyphs with the chosen one circled in ink, the day written out beside them and
 * a strike-out to rub it away. The line being written on the page and the sheet an
 * existing list is edited on carry the same marks, so a date is jotted the same
 * way whether the list exists yet or not.
 */
@Composable
fun RowScope.DateMarks(
    selection: DateSelection,
    onKindChange: (DateKind) -> Unit,
    onPickDate: () -> Unit,
    onClearDate: () -> Unit,
    targetModifier: Modifier = Modifier,
    dueModifier: Modifier = Modifier
) {
    val palette = LocalPaperPalette.current
    val locale = formatLocale
    KindGlyph(
        iconRes = R.drawable.ic_event,
        descriptionRes = R.string.set_target_date,
        seed = TARGET_RING_SEED,
        selected = selection.kind == DateKind.TARGET,
        onClick = { onKindChange(DateKind.TARGET) },
        modifier = targetModifier
    )
    KindGlyph(
        iconRes = R.drawable.ic_alarm,
        descriptionRes = R.string.set_due_date,
        seed = DUE_RING_SEED,
        selected = selection.kind == DateKind.DUE,
        onClick = { onKindChange(DateKind.DUE) },
        modifier = dueModifier
    )
    WrittenDate(
        selection = selection,
        locale = locale,
        onClick = onPickDate,
        modifier = Modifier.weight(1f)
    )
    if (selection.date != null) {
        InkIconButton(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(
                if (selection.kind == DateKind.TARGET) {
                    R.string.clear_target_date
                } else {
                    R.string.clear_due_date
                }
            ),
            onClick = onClearDate,
            modifier = Modifier.align(Alignment.Bottom),
            tint = palette.inked(InkTone.Margin)
        )
    }
}

@Composable
private fun RowScope.KindGlyph(
    iconRes: Int,
    descriptionRes: Int,
    seed: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val palette = LocalPaperPalette.current
    Box(
        modifier = modifier
            .align(Alignment.Bottom)
            .size(PaperDimens.iconButton)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(RING_BOX)
                .circledInInk(circled = selected, seed = seed, color = palette.inked(InkTone.Acted)),
            contentAlignment = Alignment.Center
        ) {
            InkIcon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(descriptionRes),
                tint = palette.inked(if (selected) InkTone.Acted else InkTone.Margin),
                size = KIND_GLYPH
            )
        }
    }
}

@Composable
private fun RowScope.WrittenDate(
    selection: DateSelection,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val palette = LocalPaperPalette.current
    val description = stringResource(
        if (selection.kind == DateKind.TARGET) R.string.set_target_date else R.string.set_due_date
    )
    val date = selection.date
    Box(
        modifier = modifier
            .align(Alignment.Bottom)
            .height(LocalPagePitch.current)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(start = DATE_GAP)
    ) {
        Text(
            text = date?.let { formatListDate(it, showYear = true, locale = locale) }
                ?: stringResource(R.string.add_line_hint),
            modifier = Modifier.seatOnRule(),
            style = LocalRuledHand.current.margin,
            color = palette.inked(if (date == null) InkTone.Margin else InkTone.Words),
            maxLines = ONE_LINE,
            overflow = TextOverflow.Ellipsis
        )
    }
}
