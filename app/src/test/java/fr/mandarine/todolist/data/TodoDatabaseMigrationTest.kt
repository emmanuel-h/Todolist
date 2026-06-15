package fr.mandarine.todolist.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class TodoDatabaseMigrationTest {

    @Test
    fun `should execute ADD COLUMN completed when migration 1 to 2 runs`() {
        val migration = getMigration("MIGRATION_1_2")
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        migration.migrate(db)
        verify { db.execSQL("ALTER TABLE todo_items ADD COLUMN completed INTEGER NOT NULL DEFAULT 0") }
    }

    @Test
    fun `should execute ADD COLUMN completedAt when migration 2 to 3 runs`() {
        val migration = getMigration("MIGRATION_2_3")
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        migration.migrate(db)
        verify { db.execSQL("ALTER TABLE todo_items ADD COLUMN completedAt INTEGER") }
    }

    private fun getMigration(fieldName: String) =
        TodoDatabase::class.java
            .getDeclaredField(fieldName)
            .also { it.isAccessible = true }
            .get(null)!!
            .let { it as androidx.room.migration.Migration }
}
