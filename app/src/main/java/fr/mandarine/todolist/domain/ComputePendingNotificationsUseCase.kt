package fr.mandarine.todolist.domain

/**
 * Decides which lists deserve a notification today. Pure: given the lists and a
 * clock, the answer is a value — nothing here knows what a notification *is* on
 * Android.
 *
 * The two rules are deliberately asymmetric:
 * - a **hard** date notifies on the day itself ("this is due today");
 * - a **soft** date notifies the day *before* ("you wanted this done tomorrow"),
 *   because a target is something to plan around, and a warning on the day is too
 *   late to act on.
 *
 * A list can only carry one of the two dates ([TodoList]), so the `when` branches
 * cannot both fire. `mapNotNull` maps and drops the nulls in one pass, which is how
 * "some lists produce nothing" is expressed without an intermediate filter.
 *
 * Note there is no "overdue" notification: nagging every day about a date that has
 * passed was judged worse than saying it once.
 */
class ComputePendingNotificationsUseCase(private val clock: Clock) {
    operator fun invoke(lists: List<TodoList>): List<ListNotification> {
        val today = clock.today()
        val tomorrow = today.plusDays(1)
        return lists.mapNotNull { list ->
            when {
                list.dueDate == today -> ListNotification.DueDateToday(list)
                list.targetDate == tomorrow -> ListNotification.TargetDateTomorrow(list)
                else -> null
            }
        }
    }
}
