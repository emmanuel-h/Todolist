package fr.mandarine.todolist.domain

/**
 * Everything the app can do to items, stated without saying where they are kept.
 *
 * `domain/` owns this interface and `data/` implements it
 * ([fr.mandarine.todolist.data.RoomTodoRepository], backed by Room/SQLite). That
 * is the whole reason the layer split exists: use cases and tests depend on this
 * declaration, so a unit test can hand them a MockK fake and never touch Android.
 *
 * Every method here is **blocking** — there is no `suspend`. Callers are
 * responsible for being on a background thread; in practice the ViewModels launch
 * on `AppContainer.databaseDispatcher`, a single-threaded dispatcher, which also
 * serialises writes.
 */
interface TodoRepository {
    /** Items of one list, ordered by `position` ascending, active and completed mixed together. */
    fun getAllByListId(listId: String): List<TodoItem>

    /** One row per list that has at least one item. See [TodoCounts]. */
    fun countsByList(): List<TodoCounts>

    fun add(item: TodoItem)

    /** Flips done/not-done, and maintains `completedAt` and `position` accordingly. */
    fun toggle(todoId: String)

    fun delete(todoId: String)

    fun updateTitle(todoId: String, title: String)

    /** Used when a whole list is deleted; the foreign key would cascade anyway, this is explicit. */
    /**
     * Renumbers only the ids named, in the order given, leaving every item not
     * named where it was. See the implementation for why that matters.
     */
    fun reorder(listId: String, orderedActiveIds: List<String>)
}
