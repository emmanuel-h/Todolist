package fr.mandarine.todolist.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The `todo_items` table row. Room generates the SQL from these annotations at
 * compile time.
 *
 * This is deliberately a **separate type** from the domain's
 * [fr.mandarine.todolist.domain.TodoItem], even though the fields currently line up
 * almost one to one. The mapping between them lives in [RoomTodoRepository]. Keeping
 * them apart means the storage shape can change (a column renamed, a date stored as
 * an epoch `Long`) without the domain or the UI knowing, and means `domain/` never
 * has to import Room.
 *
 * The `ForeignKey` with `CASCADE` makes SQLite delete an item when its list goes.
 * [fr.mandarine.todolist.domain.DeleteTodoListUseCase] also deletes items
 * explicitly — this is the belt to that braces, and it also protects against orphan
 * rows written by any other path.
 *
 * The `Index("listId")` matters: every read of a screen is
 * `WHERE listId = ?`, and without an index that is a full table scan of every item
 * in every list.
 *
 * Adding or removing a field here means bumping the version in [TodoDatabase] and
 * writing a `Migration`.
 */
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
    val completedAt: Long? = null,
    val position: Int = 0
)
