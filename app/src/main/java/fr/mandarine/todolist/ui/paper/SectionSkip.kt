package fr.mandarine.todolist.ui.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SectionSkip(completedCount: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LocalPagePitch.current)
    ) {
        Text(
            text = completedCount.toString(),
            modifier = Modifier.width(PaperDimens.gutter),
            style = PaperType.margin,
            color = LocalPaperPalette.current.pencil,
            textAlign = TextAlign.Center
        )
    }
}
