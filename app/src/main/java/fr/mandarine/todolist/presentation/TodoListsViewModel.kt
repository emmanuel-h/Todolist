package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase
import java.time.LocalDate

class TodoListsViewModel(
    private val createTodoListUseCase: CreateTodoListUseCase,
    private val deleteTodoListUseCase: DeleteTodoListUseCase,
    private val editTodoListUseCase: EditTodoListUseCase,
    private val getTodoListsWithStatusUseCase: GetTodoListsWithStatusUseCase,
    private val reorderTodoListsUseCase: ReorderTodoListsUseCase
) {
    val state: TodoListsState
        get() {
            val summaries = getTodoListsWithStatusUseCase()
            if (summaries.isEmpty()) return TodoListsState.Empty
            val activeSummaries = summaries.filter { !it.allDone }
            val doneSummaries = summaries.filter { it.allDone }
            return TodoListsState.Content(activeSummaries, doneSummaries)
        }

    fun createList(name: String, targetDate: LocalDate? = null, dueDate: LocalDate? = null) {
        createTodoListUseCase(name, targetDate, dueDate)
    }

    fun submitInlineInput(name: String): Boolean {
        if (name.isBlank()) return false
        createTodoListUseCase(name, null)
        return true
    }

    fun deleteList(todoListId: String) {
        deleteTodoListUseCase(todoListId)
    }

    fun editList(todoListId: String, newName: String, targetDate: LocalDate?, dueDate: LocalDate? = null) {
        if (newName.isBlank()) return
        editTodoListUseCase(todoListId, newName, targetDate, dueDate)
    }

    fun reorderLists(fromIndex: Int, toIndex: Int) {
        reorderTodoListsUseCase(fromIndex, toIndex)
    }
}
