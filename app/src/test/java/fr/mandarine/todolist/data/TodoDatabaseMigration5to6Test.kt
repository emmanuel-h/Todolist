package fr.mandarine.todolist.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class TodoDatabaseMigration5to6Test {

    @Test
    fun `should execute ADD COLUMN targetDate on todo_lists when migration 5 to 6 runs`() {
        val migration = getMigration("MIGRATION_5_6")
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        migration.migrate(db)

        verify { db.execSQL("ALTER TABLE todo_lists ADD COLUMN targetDate INTEGER") }
    }

    private fun getMigration(fieldName: String) =
        TodoDatabase::class.java
            .getDeclaredField(fieldName)
            .also { it.isAccessible = true }
            .get(null)!!
            .let { it as androidx.room.migration.Migration }
}
