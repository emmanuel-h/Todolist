package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.R

private const val ONE_LINE = 1
private const val OPEN = 0f
private const val FOLDED = -90f
private const val FOLD_LABEL = "sectionFold"
private val COUNT_GAP = 8.dp

/**
 * A printed heading over a run of rows: the section's name in small capitals and
 * how many rows are under it, written on a rule with one blank rule above it, so a
 * section reads as a new paragraph of the page rather than as one more row.
 *
 * A heading that can be folded carries a chevron at the end of its rule and answers
 * a tap anywhere along it. [spoken] is what a screen reader hears in place of the
 * printed name and numeral, which on their own read as a word and a stray digit.
 * The printed words stay in the tree under it, so the page's word guard sees them.
 */
@Composable
fun SectionHeading(
    title: String,
    count: Int,
    spoken: String,
    modifier: Modifier = Modifier,
    tone: InkTone = InkTone.Margin,
    animated: Boolean = true,
    folded: Boolean = false,
    onFold: (() -> Unit)? = null,
    foldedSpoken: String = "",
    openSpoken: String = ""
) {
    val palette = LocalPaperPalette.current
    val pitch = LocalPagePitch.current
    val ink = palette.inked(tone)
    val locale = formatLocale
    val printed = remember(title, locale) { title.uppercase(locale) }
    val turn by animateFloatAsState(
        targetValue = if (folded) FOLDED else OPEN,
        animationSpec = PaperMotion.pageMove,
        label = FOLD_LABEL
    )
    val press = if (onFold == null) {
        Modifier
    } else {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = PaperFocusMark,
            onClick = onFold
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(press)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = spoken
                if (onFold != null) {
                    role = Role.Button
                    stateDescription = if (folded) foldedSpoken else openSpoken
                }
            }
    ) {
        Spacer(Modifier.height(pitch))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(pitch)
                .padding(start = LocalPaperGutter.current, end = PaperDimens.rowEndPadding),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = printed,
                modifier = Modifier.seatOnRule(),
                style = PaperType.kicker,
                color = ink,
                softWrap = false,
                maxLines = ONE_LINE
            )
            Spacer(Modifier.width(COUNT_GAP))
            TallyRoll(count = count, animated = animated) { tally ->
                Text(
                    text = tally.toString(),
                    modifier = Modifier.seatOnRule(),
                    style = PaperType.kicker,
                    color = palette.inked(InkTone.Margin),
                    softWrap = false,
                    maxLines = ONE_LINE
                )
            }
            Spacer(Modifier.weight(1f))
            if (onFold != null) {
                InkIcon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = COUNT_GAP)
                        .seatGlyphOnRule(GlyphFoot.chevronDown)
                        .graphicsLayer { rotationZ = turn },
                    tint = palette.inked(InkTone.Margin),
                    size = PaperDimens.rowGlyph
                )
            }
        }
    }
}
