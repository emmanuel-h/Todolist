package fr.mandarine.todolist.domain

import java.time.LocalDate

/**
 * Everything the app can do to lists, stated without saying where they are kept.
 * Implemented by [fr.mandarine.todolist.data.RoomTodoListRepository]. See
 * [TodoRepository] for the notes on blocking calls and threading, which apply here
 * too.
 *
 * [addAtTop] exists separately from [add] because a newly written list belongs at
 * the top of the page: it shifts every existing position up by one and inserts at
 * zero, in one transaction. [add] inserts at whatever position the object carries
 * and is only used where the caller has already decided the slot.
 */
interface TodoListRepository {
    /** All lists, ordered by `position` ascending. */
    fun getAll(): List<TodoList>

    fun add(todoList: TodoList)

    /** Inserts at position 0, pushing everything else down. */
    fun addAtTop(todoList: TodoList)

    fun delete(todoListId: String)

    /**
     * Writes all of the editable fields at once. Passing the whole set rather than
     * one field at a time keeps the "target or due, never both" invariant checkable
     * at a single point — see [EditTodoListUseCase].
     */
    fun update(todoListId: String, name: String, targetDate: LocalDate?, dueDate: LocalDate?, colour: ListColour)

    /** Renumbers only the ids named, leaving lists not named where they were. */
    fun reorder(orderedActiveIds: List<String>)
}
