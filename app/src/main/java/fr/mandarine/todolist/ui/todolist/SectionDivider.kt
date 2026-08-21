package fr.mandarine.todolist.ui.todolist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.mandarine.todolist.ui.paper.PaperDimens
import fr.mandarine.todolist.ui.paper.PaperInk

private const val LEADING_RULE_WEIGHT = 1f
private const val TRAILING_RULE_WEIGHT = 3f

@Composable
fun SectionDivider(completedCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PaperDimens.ghostRowCollapsedHeight)
            .padding(start = PaperDimens.sectionInset, end = 16.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionRule(weight = LEADING_RULE_WEIGHT)
        Text(
            text = completedCount.toString(),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = PaperInk.inkSoft
        )
        SectionRule(weight = TRAILING_RULE_WEIGHT)
    }
}

@Composable
private fun RowScope.SectionRule(weight: Float) {
    Box(
        modifier = Modifier
            .weight(weight)
            .height(PaperDimens.rule)
            .background(PaperInk.rule)
    )
}
