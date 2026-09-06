package fr.mandarine.todolist.domain

/**
 * Removes a list and everything written on it.
 *
 * Items are deleted first and explicitly, even though `todo_items` has an
 * `ON DELETE CASCADE` foreign key to `todo_lists` that would do it anyway. The
 * cascade is a safety net at the storage level; doing it here means the behaviour
 * is stated in the domain, is visible to a reader of this class, and is testable
 * against a fake repository that has no SQL in it at all.
 *
 * Order matters: children before parent, so the database is never momentarily
 * holding items whose list is gone.
 */
class DeleteTodoListUseCase(private val todoListRepository: TodoListRepository) {
    operator fun invoke(todoListId: String) {
        todoListRepository.delete(todoListId)
    }
}
