package fr.mandarine.todolist.ui

import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.textfield.TextInputEditText
import fr.mandarine.todolist.presentation.TutorialBounds
import kotlinx.coroutines.delay

private const val TYPE_CHAR_MILLIS = 80L

internal fun View.tutorialBounds(): TutorialBounds {
    val location = IntArray(2)
    getLocationOnScreen(location)
    return TutorialBounds(location[0], location[1], width, height)
}

/**
 * Hides the IME without clearing focus — clearing it would collapse the items
 * screen's inline add row and break the scene that follows.
 */
internal fun View.hideTutorialKeyboard() {
    context.getSystemService(InputMethodManager::class.java)
        .hideSoftInputFromWindow(windowToken, 0)
}

internal suspend fun TextInputEditText.typeTutorialText(text: String) {
    for (char in text) {
        delay(TYPE_CHAR_MILLIS)
        this.text?.append(char)
    }
    hideTutorialKeyboard()
}
