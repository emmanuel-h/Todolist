package fr.mandarine.todolist.domain

/**
 * A [TodoList] plus everything the page of lists needs to draw its row, worked out
 * once against today's date.
 *
 * The point of this type is that no date arithmetic happens in the UI. A composable
 * can be recomposed many times per second and has no business asking what day it
 * is; [GetTodoListsWithStatusUseCase] answers all of those questions once per read
 * and hands down plain booleans and enums.
 *
 * - [allDone] — every item ticked, and there is at least one item. Drives the
 *   split into the active and finished sections on screen.
 * - [isTargetDateElapsed] — a soft date that has passed; the date is drawn struck
 *   through rather than removed.
 * - [showTargetYear] / [showDueDateYear] — the year is written only when it is not
 *   the current one, so `12 Mar` stays short in the common case.
 * - [dueDateStatus] — null when the list has no hard date; otherwise which side of
 *   today it falls on, which picks the ink colour.
 */
data class TodoListSummary(
    val list: TodoList,
    val allDone: Boolean,
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val isTargetDateElapsed: Boolean = false,
    val showTargetYear: Boolean = false,
    val dueDateStatus: DueDateStatus? = null,
    val showDueDateYear: Boolean = false
)
