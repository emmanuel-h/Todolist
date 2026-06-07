package fr.mandarine.todolist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TodoItemDao {
    @Query("SELECT * FROM todo_items WHERE listId = :listId")
    fun getAllByListId(listId: String): List<TodoItemEntity>

    @Insert
    fun insert(item: TodoItemEntity)

    @Query("DELETE FROM todo_items WHERE listId = :listId")
    fun deleteAllByListId(listId: String)
}
