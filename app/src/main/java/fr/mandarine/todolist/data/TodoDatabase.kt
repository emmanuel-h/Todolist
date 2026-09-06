package fr.mandarine.todolist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The SQLite database, its two tables, and the ladder of migrations that has taken
 * it from version 1 to version 8.
 *
 * ### Reading the migrations
 * Each `Migration(from, to)` is one released schema change, in order, and together
 * they tell the history of the app: `completed`, then `completedAt`, then item
 * `position`, then list `position`, then `targetDate`, then `dueDate`, then
 * `colour`. Room runs whichever subset a given device needs on first open.
 *
 * **Changing the schema is a three-step job**, and skipping any of them means a
 * crash on upgrade for existing users:
 * 1. change the `@Entity` data class;
 * 2. bump `version` here;
 * 3. add a `MIGRATION_n_n+1` and list it in `addMigrations(...)`.
 *
 * `exportSchema = true` writes the resulting schema as JSON under `app/schemas/`, which is
 * checked in. Only 7 and 8 were ever exported, so `TodoDatabaseMigrationTest` builds a
 * version 1 database by hand and opens it through `getInstance` — the real ladder against
 * real SQLite, which is the only thing that catches a wrong `DEFAULT` or a migration that
 * was written and never registered.
 *
 * ### The singleton
 * `getInstance` is the classic double-checked lock: the `@Volatile` field is read
 * without synchronisation on the happy path, and only contended callers pay for the
 * `synchronized` block. Room databases are expensive to open and are designed to be
 * held one per process, so this is created once and handed out by `AppContainer`.
 */
@Database(entities = [TodoListEntity::class, TodoItemEntity::class], version = 8, exportSchema = true)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoListDao(): TodoListDao
    abstract fun todoItemDao(): TodoItemDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_items ADD COLUMN completed INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_items ADD COLUMN completedAt INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_items ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_lists ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_lists ADD COLUMN targetDate INTEGER")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_lists ADD COLUMN dueDate INTEGER")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_lists ADD COLUMN colour TEXT NOT NULL DEFAULT 'None'")
            }
        }

        @Volatile
        private var instance: TodoDatabase? = null

        fun getInstance(context: Context): TodoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    /**
                     * Auto Backup copies the database file and its sidecars
                     * independently. Under WAL a snapshot can catch a committed
                     * row that lives only in the -wal half, so the restore is a
                     * torn one; truncating keeps every commit in the file that
                     * gets backed up.
                     */
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build()
                    .also { instance = it }
            }
    }
}
