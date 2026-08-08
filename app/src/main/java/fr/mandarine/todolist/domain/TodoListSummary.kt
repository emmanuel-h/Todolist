package fr.mandarine.todolist.domain

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
