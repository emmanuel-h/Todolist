package fr.mandarine.todolist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TodoItemDao {
    @Query("SELECT * FROM todo_items WHERE listId = :listId ORDER BY position ASC")
    fun getAllByListId(listId: String): List<TodoItemEntity>

    @Query("SELECT * FROM todo_items WHERE id = :id LIMIT 1")
    fun getById(id: String): TodoItemEntity?

    @Insert
    fun insert(item: TodoItemEntity)

    @Query("UPDATE todo_items SET completed = :completed, completedAt = :completedAt WHERE id = :id")
    fun updateCompleted(id: String, completed: Boolean, completedAt: Long?)

    @Query("DELETE FROM todo_items WHERE id = :id")
    fun deleteById(id: String)

    @Query("UPDATE todo_items SET title = :title WHERE id = :id")
    fun updateTitle(id: String, title: String)

    @Query("DELETE FROM todo_items WHERE listId = :listId")
    fun deleteAllByListId(listId: String)

    @Query("UPDATE todo_items SET position = :position WHERE id = :id")
    fun updatePosition(id: String, position: Int)
}
