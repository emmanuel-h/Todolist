package fr.mandarine.todolist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TodoListDao {
    @Query("SELECT * FROM todo_lists ORDER BY position ASC")
    fun getAll(): List<TodoListEntity>

    @Insert
    fun insert(todoList: TodoListEntity)

    @Query("DELETE FROM todo_lists WHERE id = :id")
    fun deleteById(id: String)

    @Query("UPDATE todo_lists SET name = :name, targetDate = :targetDate, dueDate = :dueDate WHERE id = :id")
    fun update(id: String, name: String, targetDate: Long?, dueDate: Long?)

    @Query("UPDATE todo_lists SET position = :position WHERE id = :id")
    fun updatePosition(id: String, position: Int)

    @Query("UPDATE todo_lists SET position = position + 1")
    fun incrementAllPositions()

    @Transaction
    fun insertAtTop(todoList: TodoListEntity) {
        incrementAllPositions()
        insert(todoList)
    }

    @Transaction
    fun updatePositions(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }
}
