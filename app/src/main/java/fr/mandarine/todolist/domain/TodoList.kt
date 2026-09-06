package fr.mandarine.todolist.domain

import java.time.LocalDate

/**
 * One list in the notebook.
 *
 * A list carries at most one date, and the two kinds mean different things:
 * [targetDate] is "I would like this done by then" (soft, drawn with a calendar
 * glyph) and [dueDate] is "this must be done by then" (hard, drawn with an alarm
 * glyph and notified about on the day). Holding both would leave the row with two
 * dates and no way to say which one governs, so the pair is mutually exclusive.
 *
 * The `init` block is Kotlin's constructor body; it runs on every construction,
 * including the ones `copy()` makes. `require(...)` throws
 * `IllegalArgumentException` when the condition fails — this is the project's
 * "validate at the boundary" rule, and it is deliberately duplicated in
 * [CreateTodoListUseCase] and [EditTodoListUseCase] so a bad call is rejected
 * before it reaches the repository as well as when the object is built.
 *
 * Dates are `java.time.LocalDate`, which needs core library desugaring on
 * `minSdk 24` — that is enabled in `app/build.gradle.kts` and lint's `NewApi`
 * check is fatal to keep it that way.
 */
data class TodoList(
    val id: String,
    val name: String,
    val position: Int = 0,
    val targetDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val colour: ListColour = ListColour.None
) {
    init {
        require(targetDate == null || dueDate == null) {
            "A list cannot have both a target date and a due date"
        }
    }
}
