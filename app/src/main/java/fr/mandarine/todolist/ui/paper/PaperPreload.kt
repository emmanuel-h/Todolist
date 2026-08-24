package fr.mandarine.todolist.ui.paper

import androidx.activity.ComponentActivity
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun ComponentActivity.preparePaperSheet() {
    val density = resources.displayMetrics.density
    lifecycleScope.launch {
        withContext(Dispatchers.Default) { paperGrainTile(density) }
    }
    lifecycleScope.launch {
        createFontFamilyResolver(this@preparePaperSheet).preload(PaperType.hand)
    }
}
