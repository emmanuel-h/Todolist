package fr.mandarine.todolist.domain

class GetTodoListsUseCase(private val repository: TodoListRepository) {
    operator fun invoke(): List<TodoList> = repository.getAll()
}
