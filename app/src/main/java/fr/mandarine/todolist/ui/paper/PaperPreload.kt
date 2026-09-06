package fr.mandarine.todolist.ui.paper

import androidx.activity.ComponentActivity
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Does the expensive parts of drawing paper *before* the first frame needs them.
 *
 * Two things in this app are slow the first time and free afterwards: generating
 * the grain tiles (a bitmap per [PaperGrain] entry, cached by density) and resolving
 * the handwriting font family. Left to happen lazily, both land on the first frame
 * and show up as a stutter as the page arrives.
 *
 * The grain runs on `Dispatchers.Default` because it is CPU work; the font resolver
 * has its own threading and is launched on the lifecycle scope directly. Both are
 * fire-and-forget — if the page wins the race it simply pays the cost itself.
 *
 * Called from `TodoListsActivity.onCreate`. Being an extension on
 * `ComponentActivity` is what gives it `lifecycleScope`, so the work is cancelled
 * with the window.
 */
fun ComponentActivity.preparePaperSheet() {
    val density = resources.displayMetrics.density
    lifecycleScope.launch {
        withContext(Dispatchers.Default) {
            PaperGrain.entries.forEach { paperGrainTile(density, it) }
        }
    }
    lifecycleScope.launch {
        createFontFamilyResolver(this@preparePaperSheet).preload(PaperType.hand)
    }
}
