package fr.mandarine.todolist.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class TodoDatabaseMigration6to7Test {

    @Test
    fun `should execute ADD COLUMN dueDate on todo_lists when migration 6 to 7 runs`() {
        val migration = getMigration("MIGRATION_6_7")
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        migration.migrate(db)

        verify { db.execSQL("ALTER TABLE todo_lists ADD COLUMN dueDate INTEGER") }
    }

    private fun getMigration(fieldName: String) =
        TodoDatabase::class.java
            .getDeclaredField(fieldName)
            .also { it.isAccessible = true }
            .get(null)!!
            .let { it as androidx.room.migration.Migration }
}
