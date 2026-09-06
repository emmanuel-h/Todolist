package fr.mandarine.todolist.data

/**
 * The shape Room projects the grouped `COUNT` query into — see
 * [TodoItemDao.countsByList]. It is not a table and has no `@Entity`; Room matches
 * the constructor parameter names against the column aliases in the SQL, so
 * renaming a field here without renaming the `AS` alias breaks the query at
 * compile time.
 *
 * Mapped to [fr.mandarine.todolist.domain.TodoCounts] by [RoomTodoRepository]; the
 * two are identical today and are still kept apart so the query can change shape
 * without touching the domain.
 */
data class TodoCountsRow(
    val listId: String,
    val activeCount: Int,
    val completedCount: Int
)
