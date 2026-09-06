package fr.mandarine.todolist.domain

/**
 * One line on a list.
 *
 * `data class` is Kotlin's record: the compiler generates `equals`/`hashCode`/
 * `toString`, plus `copy(...)` for making a modified duplicate. Everything here is
 * `val`, so an instance never changes — a "change" is a new instance built with
 * `copy(isCompleted = true)`. Compose relies on this: it decides whether to redraw
 * by comparing old and new values, which only works if values cannot mutate
 * behind its back.
 *
 * [position] is the item's slot within its list, an index that is dense over the
 * *active* items only. Completed items keep whatever position they had when they
 * were ticked, so positions are not unique across the whole list and must never be
 * used as an identity — [id] is the identity.
 *
 * [completedAt] is epoch milliseconds and is null exactly when [isCompleted] is
 * false. It exists so the completed section can be shown most-recent-first.
 */
data class TodoItem(
    val id: String,
    val title: String,
    val listId: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val position: Int = 0
)
