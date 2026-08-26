package fr.mandarine.todolist.ui.tutorial

import fr.mandarine.todolist.domain.TodoList
import fr.mandarine.todolist.domain.TodoListRepository
import java.time.LocalDate

internal const val FIRST_RULE_ON_AN_EMPTY_PAGE = 0

/**
 * The demo's list has to be written above the reader's, but a demonstration may not
 * rewrite a single row of theirs to make room. Laying it one rule higher than the
 * highest row already on the page puts it first without renumbering anything, so
 * removing it again leaves every reader row exactly as it was found.
 */
class NonShiftingTodoListRepository(
    private val page: TodoListRepository
) : TodoListRepository {

    override fun getAll(): List<TodoList> = page.getAll()

    override fun add(todoList: TodoList) {
        page.add(todoList)
    }

    override fun addAtTop(todoList: TodoList) {
        page.add(todoList.copy(position = aboveEveryRow()))
    }

    override fun delete(todoListId: String) {
        page.delete(todoListId)
    }

    override fun update(
        todoListId: String,
        name: String,
        targetDate: LocalDate?,
        dueDate: LocalDate?
    ) {
        page.update(todoListId, name, targetDate, dueDate)
    }

    override fun reorder(orderedActiveIds: List<String>) {
        page.reorder(orderedActiveIds)
    }

    private fun aboveEveryRow(): Int =
        page.getAll().minOfOrNull { it.position - 1 } ?: FIRST_RULE_ON_AN_EMPTY_PAGE
}
