package fr.mandarine.todolist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TodoItemDao {
    @Query("SELECT * FROM todo_items WHERE listId = :listId")
    fun getAllByListId(listId: String): List<TodoItemEntity>

    @Query("SELECT * FROM todo_items WHERE id = :id LIMIT 1")
    fun getById(id: String): TodoItemEntity?

    @Insert
    fun insert(item: TodoItemEntity)

    @Query("UPDATE todo_items SET completed = :completed, completedAt = :completedAt WHERE id = :id")
    fun updateCompleted(id: String, completed: Boolean, completedAt: Long?)

    @Query("DELETE FROM todo_items WHERE listId = :listId")
    fun deleteAllByListId(listId: String)
}
