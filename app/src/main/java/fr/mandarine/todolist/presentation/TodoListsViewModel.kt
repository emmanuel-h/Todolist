package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.CreateTodoListUseCase
import fr.mandarine.todolist.domain.DeleteTodoListUseCase
import fr.mandarine.todolist.domain.GetTodoListsUseCase

class TodoListsViewModel(
    private val createTodoListUseCase: CreateTodoListUseCase,
    private val deleteTodoListUseCase: DeleteTodoListUseCase,
    private val getTodoListsUseCase: GetTodoListsUseCase
) {
    val state: TodoListsState
        get() {
            val lists = getTodoListsUseCase()
            return if (lists.isEmpty()) TodoListsState.Empty else TodoListsState.Content(lists)
        }

    fun createList(name: String) {
        createTodoListUseCase(name)
    }

    fun deleteList(todoListId: String) {
        deleteTodoListUseCase(todoListId)
    }
}
