package fr.mandarine.todolist.ui.paper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

/**
 * Every action a row still answers to is a gesture, and a gesture is unreachable
 * by a screen reader or a switch. Each row therefore names its verbs here, on the
 * node the reader actually lands on, so the same list is available without one.
 */
@Immutable
class RowVerb(val label: String, val perform: () -> Unit)

fun rowVerbs(vararg verbs: RowVerb?): List<RowVerb> = verbs.filterNotNull()

fun Modifier.spokenVerbs(verbs: List<RowVerb>): Modifier {
    if (verbs.isEmpty()) return this
    return semantics {
        customActions = verbs.map { verb ->
            CustomAccessibilityAction(verb.label) {
                verb.perform()
                true
            }
        }
    }
}
