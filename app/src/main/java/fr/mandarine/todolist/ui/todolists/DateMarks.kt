package fr.mandarine.todolist.ui.todolists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import fr.mandarine.todolist.ui.paper.pressableBelowTheRule
import fr.mandarine.todolist.ui.paper.seatOnRule
import java.util.Locale

private const val TARGET_RING_SEED = 0x7A26
private const val DUE_RING_SEED = 0x5C11
private const val ONE_LINE = 1
private val RING_BOX = 30.dp
private val KIND_GLYPH = 20.dp
private val DATE_GAP = 8.dp

/**
 * What pressing a kind glyph does, which is decided by what is written beside it
 * rather than by the glyph. A kind is something a date has, not something chosen
 * before there is one — so a bare rule asks for a day, the other glyph moves a day
 * already written across to itself, and the glyph already ringed rubs it out.
 */
internal enum class KindPress { AskForADay, MoveTheDay, RubItOut }

internal fun kindPressOn(selection: DateSelection, pressed: DateKind): KindPress = when {
    selection.date == null -> KindPress.AskForADay
    selection.kind != pressed -> KindPress.MoveTheDay
    else -> KindPress.RubItOut
}

/**
 * The marks a date wears on a rule, wherever that rule is: the two kinds as
 * glyphs with the ringed one saying which kind the day beside it is, and the day
 * written out after them. The line being written on the page and the sheet an
 * existing list is edited on carry the same marks, so a date is jotted the same
 * way whether the list exists yet or not.
 *
 * With nothing written the rule is bare: neither glyph is ringed and nothing
 * trails them. A ring means a day, so a ring cannot appear before there is one.
 */
@Composable
fun RowScope.DateMarks(
    selection: DateSelection,
    said: DateKindSaid,
    onKindChange: (DateKind) -> Unit,
    onPickDate: (DateKind) -> Unit,
    onClearDate: () -> Unit
) {
    val locale = formatLocale
    KindGlyph(
        iconRes = R.drawable.ic_event,
        setRes = R.string.set_target_date,
        clearRes = R.string.clear_target_date,
        seed = TARGET_RING_SEED,
        kind = DateKind.TARGET,
        selection = selection,
        said = said,
        onKindChange = onKindChange,
        onPickDate = onPickDate,
        onClearDate = onClearDate
    )
    KindGlyph(
        iconRes = R.drawable.ic_alarm,
        setRes = R.string.set_due_date,
        clearRes = R.string.clear_due_date,
        seed = DUE_RING_SEED,
        kind = DateKind.DUE,
        selection = selection,
        said = said,
        onKindChange = onKindChange,
        onPickDate = onPickDate,
        onClearDate = onClearDate
    )
    WrittenDate(
        selection = selection,
        locale = locale,
        onClick = { onPickDate(selection.kind) },
        modifier = Modifier.weight(1f)
    )
}

@Composable
private fun RowScope.KindGlyph(
    iconRes: Int,
    setRes: Int,
    clearRes: Int,
    seed: Int,
    kind: DateKind,
    selection: DateSelection,
    said: DateKindSaid,
    onKindChange: (DateKind) -> Unit,
    onPickDate: (DateKind) -> Unit,
    onClearDate: () -> Unit
) {
    val palette = LocalPaperPalette.current
    val press = kindPressOn(selection, kind)
    val ringed = press == KindPress.RubItOut
    Box(
        modifier = Modifier
            .align(Alignment.Bottom)
            .size(PaperDimens.iconButton)
            .selectable(
                selected = ringed,
                role = Role.RadioButton,
                onClick = {
                    when (press) {
                        KindPress.AskForADay -> onPickDate(kind)

                        KindPress.MoveTheDay -> {
                            said.say(kind)
                            onKindChange(kind)
                        }

                        KindPress.RubItOut -> {
                            said.hush()
                            onClearDate()
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(RING_BOX)
                .circledInInk(circled = ringed, seed = seed, color = palette.inked(InkTone.Acted)),
            contentAlignment = Alignment.Center
        ) {
            InkIcon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(if (ringed) clearRes else setRes),
                tint = palette.inked(if (ringed) InkTone.Acted else InkTone.Margin),
                size = KIND_GLYPH
            )
        }
    }
}

/**
 * The day, once there is one. There is nothing to write before then and nothing to
 * press either: the glyphs are what ask for a day, so a placeholder here was a
 * mark on the rule that said only that the reader had not done anything yet.
 */
@Composable
private fun RowScope.WrittenDate(
    selection: DateSelection,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val palette = LocalPaperPalette.current
    val date = selection.date ?: run {
        Spacer(modifier)
        return
    }
    val pressing = stringResource(
        if (selection.kind == DateKind.TARGET) R.string.set_target_date else R.string.set_due_date
    )
    val spoken = stringResource(
        if (selection.kind == DateKind.TARGET) {
            R.string.target_date_description
        } else {
            R.string.due_date_description
        },
        formatListDate(date, showYear = true, locale = locale)
    )
    Box(
        modifier = modifier
            .align(Alignment.Bottom)
            .height(LocalPagePitch.current)
            .pressableBelowTheRule(onRule = true)
            .clickable(onClickLabel = pressing, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .padding(start = DATE_GAP)
    ) {
        Text(
            text = formatListDate(date, showYear = true, locale = locale),
            modifier = Modifier.seatOnRule(),
            style = LocalRuledHand.current.margin,
            color = palette.inked(InkTone.Words),
            maxLines = ONE_LINE,
            overflow = TextOverflow.Ellipsis
        )
    }
}
