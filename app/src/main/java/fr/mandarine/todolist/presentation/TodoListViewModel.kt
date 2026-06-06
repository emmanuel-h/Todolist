package fr.mandarine.todolist.presentation

import fr.mandarine.todolist.domain.AddTodoUseCase
import fr.mandarine.todolist.domain.GetTodosUseCase

class TodoListViewModel(
    private val addTodoUseCase: AddTodoUseCase,
    private val getTodosUseCase: GetTodosUseCase,
    private val listId: String
) {
    val state: TodoListState
        get() {
            val items = getTodosUseCase(listId)
            return if (items.isEmpty()) TodoListState.Empty else TodoListState.Content(items)
        }

    fun addTodo(title: String) {
        addTodoUseCase(title, listId)
    }
}
