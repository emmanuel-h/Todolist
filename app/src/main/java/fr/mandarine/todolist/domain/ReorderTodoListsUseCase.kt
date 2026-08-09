package fr.mandarine.todolist.domain

class ReorderTodoListsUseCase(
    private val repository: TodoListRepository,
    private val getTodoListsWithStatusUseCase: GetTodoListsWithStatusUseCase
) {
    operator fun invoke(fromIndex: Int, toIndex: Int) {
        val summaries = getTodoListsWithStatusUseCase()
        val activeIds = summaries.filter { !it.allDone }.map { it.list.id }
        require(fromIndex in activeIds.indices)
        require(toIndex in activeIds.indices)
        val fromGlobal = summaries.indexOfFirst { it.list.id == activeIds[fromIndex] }
        val toGlobal = summaries.indexOfFirst { it.list.id == activeIds[toIndex] }
        repository.reorder(fromGlobal, toGlobal)
    }
}
