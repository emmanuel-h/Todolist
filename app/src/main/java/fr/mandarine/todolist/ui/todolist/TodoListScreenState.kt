package fr.mandarine.todolist.ui.todolist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.mandarine.todolist.ui.tutorial.TutorialAnchorHost
import fr.mandarine.todolist.ui.tutorial.TutorialAnchors

class TodoListScreenState : TutorialAnchorHost by TutorialAnchors() {

    var addRowExpanded by mutableStateOf(false)

    var addRowText by mutableStateOf("")

    var editingItemId by mutableStateOf<String?>(null)

    var previewOrder by mutableStateOf<List<String>?>(null)

    var animationsEnabled by mutableStateOf(true)

    var hideKeyboardSignal by mutableStateOf(0)
        private set

    fun requestHideKeyboard() {
        hideKeyboardSignal += 1
    }
}
