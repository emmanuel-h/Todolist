package fr.mandarine.todolist.domain

/**
 * Which side of today a list's hard date falls on. Computed in
 * [GetTodoListsWithStatusUseCase] and used by the UI to choose the ink a date is
 * written in. Only lists that actually have a [TodoList.dueDate] get one; for the
 * rest [TodoListSummary.dueDateStatus] is null.
 */
enum class DueDateStatus { FUTURE, TODAY, OVERDUE }
