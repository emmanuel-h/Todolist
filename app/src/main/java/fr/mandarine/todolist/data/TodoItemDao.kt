package fr.mandarine.todolist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/**
 * Room's data access object for `todo_items`.
 *
 * A `@Dao` interface has no implementation in this repo — Room's annotation
 * processor generates `TodoItemDao_Impl` at build time and verifies every `@Query`
 * string against the schema, so a typo in the SQL or a missing column is a
 * *compile* error rather than a crash. The generated class appears in
 * `app/build/generated/`; when the schema changes, a stale build cache is the usual
 * reason a query suddenly stops making sense.
 *
 * Every method is blocking. Room would normally refuse to run these on the main
 * thread (`allowMainThreadQueries` is not set), which is enforcement of the rule
 * that all calls come in on `AppContainer.databaseDispatcher`.
 *
 * `:name` in a query binds to the parameter of the same name.
 */
@Dao
interface TodoItemDao {
    @Query("SELECT * FROM todo_items WHERE listId = :listId ORDER BY position ASC")
    fun getAllByListId(listId: String): List<TodoItemEntity>

    @Query("SELECT * FROM todo_items WHERE id = :id LIMIT 1")
    fun getById(id: String): TodoItemEntity?

    /**
     * One row per list that has any items, counting done and not-done separately.
     *
     * `COUNT(CASE WHEN ... THEN 1 END)` counts only the rows where the case yields
     * non-null, which is the portable way to do a conditional count. The `AS`
     * aliases must keep matching the property names of [TodoCountsRow] — that is
     * how Room maps the projection.
     *
     * This exists so the page of lists costs one query instead of one per list.
     */
    @Query(
        "SELECT listId, " +
            "COUNT(CASE WHEN completed = 0 THEN 1 END) AS activeCount, " +
            "COUNT(CASE WHEN completed = 1 THEN 1 END) AS completedCount " +
            "FROM todo_items GROUP BY listId"
    )
    fun countsByList(): List<TodoCountsRow>

    @Insert
    fun insert(item: TodoItemEntity)

    /**
     * The three fields that move together when an item is ticked. They are written
     * in one statement because a partial write — done but with no timestamp, or
     * done but still holding its old position — is a state the rest of the app does
     * not expect to read.
     */
    @Query("UPDATE todo_items SET completed = :completed, completedAt = :completedAt, position = :position WHERE id = :id")
    fun updateCompletedAndPosition(id: String, completed: Boolean, completedAt: Long?, position: Int)

    @Query("DELETE FROM todo_items WHERE id = :id")
    fun deleteById(id: String)

    @Query("UPDATE todo_items SET title = :title WHERE id = :id")
    fun updateTitle(id: String, title: String)

    @Query("DELETE FROM todo_items WHERE listId = :listId")
    fun deleteAllByListId(listId: String)

    @Query("UPDATE todo_items SET position = :position WHERE id = :id")
    fun updatePosition(id: String, position: Int)

    /**
     * Renumbers a run of items to `0..n-1` in the order given.
     *
     * `@Transaction` on a Kotlin interface method with a body makes Room wrap the
     * whole body in one database transaction — so a reorder is all-or-nothing and
     * no reader ever sees the list half-renumbered. Without it these would be N
     * separate commits.
     *
     * Note the caller is expected to have already worked out which ids to pass and
     * in which order; this method does no filtering. See [RoomTodoRepository.reorder].
     */
    @Transaction
    fun updatePositions(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }
}
