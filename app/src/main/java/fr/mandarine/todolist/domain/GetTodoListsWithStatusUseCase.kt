package fr.mandarine.todolist.domain

/**
 * The read behind the page of lists: every list, decorated with the counts and the
 * date judgements its row needs.
 *
 * This is the only place in the app where "how does this list stand today" is
 * decided. Doing it once, here, is what keeps date arithmetic out of composables —
 * a composable may be re-run many times a second and must not be asking what day
 * it is. Today's date comes from the injected [clock] so the whole thing is
 * testable on any day.
 *
 * ### Cost
 * Two queries, not N+1. [TodoRepository.countsByList] returns one grouped `COUNT`
 * row per list, and `associateBy` turns that list into a `Map<listId, TodoCounts>`
 * for O(1) lookup while walking the lists. Loading each list's items to count them
 * would make opening the app cost the whole database.
 *
 * ### The derived fields
 * - `allDone` requires `completedCount > 0` as well as `activeCount == 0`, so an
 *   empty list is *not* reported as finished — otherwise a list you just created
 *   would immediately drop into the finished section.
 * - `showYear` / `showDueDateYear` are true only when the date is in another
 *   calendar year, which keeps the common case short (`12 Mar`, not `12 Mar 26`).
 * - `isElapsed` applies to the soft date only; an elapsed target is drawn struck
 *   through rather than removed. A passed hard date is [DueDateStatus.OVERDUE]
 *   instead, which is a louder signal.
 *
 * Note `list.targetDate` is smart-cast inside the `!= null` check — Kotlin knows
 * a `val` on an immutable `data class` cannot change between the check and the use.
 */
class GetTodoListsWithStatusUseCase(
    private val todoListRepository: TodoListRepository,
    private val todoRepository: TodoRepository,
    private val clock: Clock
) {
    operator fun invoke(): List<TodoListSummary> {
        val today = clock.today()
        val countsByList = todoRepository.countsByList().associateBy { it.listId }
        return todoListRepository.getAll().map { list ->
            val counts = countsByList[list.id]
            val activeCount = counts?.activeCount ?: 0
            val completedCount = counts?.completedCount ?: 0
            val allDone = activeCount == 0 && completedCount > 0
            val isElapsed = list.targetDate != null && list.targetDate.isBefore(today)
            val showYear = list.targetDate != null && list.targetDate.year != today.year
            val dueDateStatus = list.dueDate?.let { d ->
                when {
                    d.isBefore(today) -> DueDateStatus.OVERDUE
                    d == today -> DueDateStatus.TODAY
                    else -> DueDateStatus.FUTURE
                }
            }
            val showDueDateYear = list.dueDate != null && list.dueDate.year != today.year
            TodoListSummary(list, allDone, activeCount, completedCount, isElapsed, showYear, dueDateStatus, showDueDateYear)
        }
    }
}
