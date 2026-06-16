package fr.mandarine.todolist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TodoListDao {
    @Query("SELECT * FROM todo_lists ORDER BY position ASC")
    fun getAll(): List<TodoListEntity>

    @Insert
    fun insert(todoList: TodoListEntity)

    @Query("DELETE FROM todo_lists WHERE id = :id")
    fun deleteById(id: String)

    @Query("UPDATE todo_lists SET name = :name WHERE id = :id")
    fun updateName(id: String, name: String)

    @Query("UPDATE todo_lists SET position = :position WHERE id = :id")
    fun updatePosition(id: String, position: Int)
}
