package fr.mandarine.todolist.presentation

/**
 * The list the demonstration writes and the two things it puts on it.
 *
 * These were Kotlin literals — a shopping list in English, on a page that had
 * already been translated around them, so a French reader watched the tour write
 * "🛒 Groceries" and then put "🍎 Apples" on it. They are handed in from the
 * composition root now, which is the only layer that can read a resource.
 */
data class TutorialDemoWords(
    val listName: String,
    val firstItem: String,
    val secondItem: String
)
