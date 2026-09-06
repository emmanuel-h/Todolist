package fr.mandarine.todolist.domain

import java.util.UUID

/**
 * Writes a new item at the foot of the list's *active* run.
 *
 * ### The `operator fun invoke` idiom
 * Every use case in this package is a class with a single method named `invoke`,
 * marked `operator`. That lets the caller write `addTodo(title, listId)` as if the
 * object were a function; it is exactly equivalent to `addTodo.invoke(title, listId)`.
 * The reason for a class rather than a plain function is the constructor: the
 * dependencies ([repository], [generateId]) are injected once at construction, in
 * `TodoListsActivity`, and the call site then needs only the arguments that vary.
 *
 * ### Why the position is computed this way
 * `position` is dense over active items only — a completed item keeps the position
 * it had when it was ticked. So the new item cannot take `items.size`; it takes one
 * past the last *active* item, or 0 if there are none. Getting this wrong makes new
 * items land in the middle of the list after a few things have been completed.
 *
 * [generateId] is a constructor parameter with a default so tests can supply
 * predictable ids instead of random UUIDs. Kotlin default arguments mean production
 * code never mentions it.
 *
 * @throws IllegalArgumentException if [title] is blank — validation happens here,
 * at the layer boundary, so the repository can assume it is given good data.
 */
class AddTodoUseCase(
    private val repository: TodoRepository,
    private val generateId: () -> String = { UUID.randomUUID().toString() }
) {
    operator fun invoke(title: String, listId: String): TodoItem {
        require(title.isNotBlank())
        val position = repository.getAllByListId(listId)
            .filter { !it.isCompleted }
            .lastOrNull()
            ?.position
            ?.plus(1) ?: 0
        val item = TodoItem(id = generateId(), title = title, listId = listId, position = position)
        repository.add(item)
        return item
    }
}
