package fr.mandarine.todolist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/**
 * Room's data access object for `todo_lists`. See [TodoItemDao] for the notes on
 * code generation, compile-time query checking and threading, which apply here too.
 */
@Dao
interface TodoListDao {
    @Query("SELECT * FROM todo_lists ORDER BY position ASC")
    fun getAll(): List<TodoListEntity>

    @Insert
    fun insert(todoList: TodoListEntity)

    @Query("DELETE FROM todo_lists WHERE id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM todo_items WHERE listId = :id")
    fun deleteItemsOf(id: String)

    /**
     * Tearing a list off takes its items with it, in one transaction.
     *
     * The foreign key already cascades, so the explicit item delete is belt and
     * braces; what `@Transaction` adds is that the two statements cannot be torn
     * apart by a process death, which is what used to leave an emptied list behind.
     */
    @Transaction
    fun deleteWithItems(id: String) {
        deleteItemsOf(id)
        deleteById(id)
    }

    /**
     * Writes every editable field at once. The default on [colour] is a Kotlin
     * default argument, not a SQL one — Room still always binds a value.
     */
    @Query("UPDATE todo_lists SET name = :name, targetDate = :targetDate, dueDate = :dueDate, colour = :colour WHERE id = :id")
    fun update(id: String, name: String, targetDate: Long?, dueDate: Long?, colour: String = "None")

    @Query("UPDATE todo_lists SET position = :position WHERE id = :id")
    fun updatePosition(id: String, position: Int)

    @Query("UPDATE todo_lists SET position = position + 1")
    fun incrementAllPositions()

    /**
     * A new list goes to the top of the page, which means shifting every existing
     * list down by one and inserting at whatever position the entity carries (zero).
     *
     * `@Transaction` is what makes this safe: between the shift and the insert there
     * is a moment with no list at position 0, and a concurrent read landing there
     * would see a gap. Wrapped, no reader ever observes it.
     */
    @Transaction
    fun insertAtTop(todoList: TodoListEntity) {
        incrementAllPositions()
        insert(todoList)
    }

    /** Renumbers to `0..n-1` in one transaction. See [TodoItemDao.updatePositions]. */
    @Transaction
    fun updatePositions(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }
}
