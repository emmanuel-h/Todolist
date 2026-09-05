package fr.mandarine.todolist.ui

/**
 * A delete waiting for the reader's answer. [name] is shown in the prompt.
 * [cascadeCount] is null for item deletes — no cascade line is shown — and
 * a non-null count for list deletes, shown only when the count is above zero.
 */
data class ConfirmDeleteRequest(
    val id: String,
    val name: String,
    val cascadeCount: Int?
)
