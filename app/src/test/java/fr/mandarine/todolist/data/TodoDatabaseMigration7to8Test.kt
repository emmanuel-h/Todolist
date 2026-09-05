package fr.mandarine.todolist.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class TodoDatabaseMigration7to8Test {

    @Test
    fun `should execute ADD COLUMN colour on todo_lists when migration 7 to 8 runs`() {
        val migration = getMigration("MIGRATION_7_8")
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        migration.migrate(db)

        verify { db.execSQL("ALTER TABLE todo_lists ADD COLUMN colour TEXT NOT NULL DEFAULT 'None'") }
    }

    private fun getMigration(fieldName: String) =
        TodoDatabase::class.java
            .getDeclaredField(fieldName)
            .also { it.isAccessible = true }
            .get(null)!!
            .let { it as androidx.room.migration.Migration }
}
