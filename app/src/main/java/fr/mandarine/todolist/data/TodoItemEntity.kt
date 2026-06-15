package fr.mandarine.todolist.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_items",
    foreignKeys = [
        ForeignKey(
            entity = TodoListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class TodoItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val listId: String,
    val completed: Boolean = false,
    val completedAt: Long? = null
)
