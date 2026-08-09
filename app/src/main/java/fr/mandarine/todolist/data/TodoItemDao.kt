package fr.mandarine.todolist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TodoItemDao {
    @Query("SELECT * FROM todo_items WHERE listId = :listId ORDER BY position ASC")
    fun getAllByListId(listId: String): List<TodoItemEntity>

    @Query("SELECT * FROM todo_items WHERE id = :id LIMIT 1")
    fun getById(id: String): TodoItemEntity?

    @Query(
        "SELECT listId, " +
            "COUNT(CASE WHEN completed = 0 THEN 1 END) AS activeCount, " +
            "COUNT(CASE WHEN completed = 1 THEN 1 END) AS completedCount " +
            "FROM todo_items GROUP BY listId"
    )
    fun countsByList(): List<TodoCountsRow>

    @Insert
    fun insert(item: TodoItemEntity)

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

    @Transaction
    fun updatePositions(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }
}
