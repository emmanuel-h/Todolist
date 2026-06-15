package fr.mandarine.todolist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TodoListEntity::class, TodoItemEntity::class], version = 3, exportSchema = false)
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

        @Volatile
        private var instance: TodoDatabase? = null

        fun getInstance(context: Context): TodoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
    }
}
