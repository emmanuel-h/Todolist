package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.EditTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsWithStatusUseCase
import fr.mandarine.todolist.domain.ReorderTodoListsUseCase

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

    fun createList(name: String) {
        createTodoListUseCase(name)
    }

    fun submitInlineInput(name: String): Boolean {
        if (name.isBlank()) return false
        createTodoListUseCase(name)
        return true
    }

    fun deleteList(todoListId: String) {
        deleteTodoListUseCase(todoListId)
    }

    fun editList(todoListId: String, newName: String) {
        if (newName.isBlank()) return
        editTodoListUseCase(todoListId, newName)
    }

    fun reorderLists(fromIndex: Int, toIndex: Int) {
        reorderTodoListsUseCase(fromIndex, toIndex)
    }
}
