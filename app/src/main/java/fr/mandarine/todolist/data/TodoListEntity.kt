package fr.mandarine.todolist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The `todo_lists` table row; the storage twin of
 * [fr.mandarine.todolist.domain.TodoList]. See [TodoItemEntity] for why the two
 * types are kept separate.
 *
 * Two fields do not match the domain type, and the conversion is done in
 * [RoomTodoListRepository]:
 * - the dates are epoch **days** (`LocalDate.toEpochDay()`), not milliseconds — a
 *   date has no time of day, and storing one as an instant invites a time-zone bug;
 * - [colour] is the *name* of a [fr.mandarine.todolist.domain.ListColour] constant,
 *   stored as text. Renaming a constant in that enum is therefore a migration, and
 *   an unknown name will throw on read.
 *
 * Unlike [TodoItemEntity] this type carries no `init` check, so nothing at the
 * storage level stops both dates being set at once; the invariant is enforced in
 * the domain and, when a row is read, by [RoomTodoListRepository] preferring the
 * hard date and dropping the soft one.
 */
@Entity(tableName = "todo_lists")
data class TodoListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int = 0,
    val targetDate: Long? = null,
    val dueDate: Long? = null,
    val colour: String = "None"
)
