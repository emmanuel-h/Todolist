package fr.mandarine.todolist.ui.todolist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.mandarine.todolist.domain.TodoItem
import fr.mandarine.todolist.ui.DeletionState
import fr.mandarine.todolist.ui.todolists.DateSelection
import fr.mandarine.todolist.ui.tutorial.TutorialAnchorHost
import fr.mandarine.todolist.ui.tutorial.TutorialAnchors

class TodoListScreenState : TutorialAnchorHost by TutorialAnchors() {

    val deletion = DeletionState()

    var addRowExpanded by mutableStateOf(false)

    var addRowText by mutableStateOf("")

    var editingItemId by mutableStateOf<String?>(null)

    var renamingList by mutableStateOf(false)

    var dateSheet by mutableStateOf<DateSelection?>(null)

    var pendingToggle by mutableStateOf<String?>(null)

    var previewOrder by mutableStateOf<List<String>?>(null)

    var animationsEnabled by mutableStateOf(true)

    var hideKeyboardSignal by mutableStateOf(0)
        private set

    fun inked(item: TodoItem): Boolean = item.isCompleted != (pendingToggle == item.id)

    fun requestHideKeyboard() {
        hideKeyboardSignal += 1
    }
}
