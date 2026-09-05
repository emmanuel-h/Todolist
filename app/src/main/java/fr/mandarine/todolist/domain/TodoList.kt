package fr.mandarine.todolist.domain

import java.time.LocalDate

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
