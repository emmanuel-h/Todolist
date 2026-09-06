package fr.mandarine.todolist.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import java.lang.reflect.Modifier
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var opened: TodoDatabase? = null
    private var raw: SupportSQLiteOpenHelper? = null

    @Before
    fun setUp() {
        resetSingleton()
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        opened?.close()
        opened = null
        raw?.close()
        raw = null
        resetSingleton()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun `should carry a version 1 row all the way to version 8 when the ladder runs`() {
        seedVersionOne()

        val database = TodoDatabase.getInstance(context).also { opened = it }
        val lists = offMainThread { database.todoListDao().getAll() }
        val items = offMainThread { database.todoItemDao().getAllByListId(LIST_ID) }

        assertEquals(1, lists.size)
        assertEquals("groceries", lists.single().name)
        assertEquals(1, items.size)
        assertEquals("milk", items.single().title)
    }

    @Test
    fun `should default the columns added after version 1 when the ladder runs`() {
        seedVersionOne()

        val database = TodoDatabase.getInstance(context).also { opened = it }
        val list = offMainThread { database.todoListDao().getAll() }.single()
        val item = offMainThread { database.todoItemDao().getAllByListId(LIST_ID) }.single()

        assertEquals(0, list.position)
        assertNull(list.targetDate)
        assertNull(list.dueDate)
        assertEquals("None", list.colour)
        assertEquals(false, item.completed)
        assertNull(item.completedAt)
        assertEquals(0, item.position)
    }

    @Test
    fun `should leave a schema Room accepts when the ladder runs`() {
        seedVersionOne()

        val database = TodoDatabase.getInstance(context).also { opened = it }
        offMainThread {
            database.todoListDao().insert(TodoListEntity("second", "chores", 1, null, null, "Blue"))
            database.todoItemDao().insert(TodoItemEntity("i2", "sweep", "second", true, 42L, 3))
        }

        val written = offMainThread { database.todoItemDao().getById("i2") }
        assertEquals(true, written?.completed)
        assertEquals(42L, written?.completedAt)
        assertEquals(3, written?.position)
        assertEquals("Blue", offMainThread { database.todoListDao().getAll() }.first { it.id == "second" }.colour)
    }

    @Test
    fun `should add every column the entities need when each migration runs in turn`() {
        val db = openVersionOne()

        assertTrue(columnsOf(db, "todo_items").containsAll(listOf("id", "title", "listId")))

        migrate(db, "MIGRATION_1_2")
        assertTrue("completed" in columnsOf(db, "todo_items"))
        migrate(db, "MIGRATION_2_3")
        assertTrue("completedAt" in columnsOf(db, "todo_items"))
        migrate(db, "MIGRATION_3_4")
        assertTrue("position" in columnsOf(db, "todo_items"))
        migrate(db, "MIGRATION_4_5")
        assertTrue("position" in columnsOf(db, "todo_lists"))
        migrate(db, "MIGRATION_5_6")
        assertTrue("targetDate" in columnsOf(db, "todo_lists"))
        migrate(db, "MIGRATION_6_7")
        assertTrue("dueDate" in columnsOf(db, "todo_lists"))
        migrate(db, "MIGRATION_7_8")
        assertTrue("colour" in columnsOf(db, "todo_lists"))
    }

    @Test
    fun `should keep the rows written before a migration when that migration runs`() {
        val db = openVersionOne()
        db.execSQL("INSERT INTO todo_lists (id, name) VALUES ('$LIST_ID', 'groceries')")

        migrate(db, "MIGRATION_1_2")
        migrate(db, "MIGRATION_2_3")
        migrate(db, "MIGRATION_3_4")
        migrate(db, "MIGRATION_4_5")
        migrate(db, "MIGRATION_5_6")
        migrate(db, "MIGRATION_6_7")
        migrate(db, "MIGRATION_7_8")

        db.query("SELECT name, position, colour FROM todo_lists").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("groceries", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals("None", cursor.getString(2))
        }
    }

    private fun seedVersionOne() {
        val db = openVersionOne()
        db.execSQL("INSERT INTO todo_lists (id, name) VALUES ('$LIST_ID', 'groceries')")
        db.execSQL("INSERT INTO todo_items (id, title, listId) VALUES ('i1', 'milk', '$LIST_ID')")
        raw?.close()
        raw = null
    }

    /**
     * A database file holding exactly what version 1 of the app shipped, written by
     * hand because `app/schemas/` only goes back to 7. Room reads `user_version`
     * from the file and runs the real migration list against it, so what these
     * tests exercise is the ladder itself rather than the text of its SQL.
     */
    private fun openVersionOne(): SupportSQLiteDatabase {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DATABASE_NAME)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `todo_lists` (" +
                                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `todo_items` (" +
                                "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `listId` TEXT NOT NULL, " +
                                "PRIMARY KEY(`id`), FOREIGN KEY(`listId`) REFERENCES `todo_lists`(`id`) " +
                                "ON UPDATE NO ACTION ON DELETE CASCADE )"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS `index_todo_items_listId` " +
                                "ON `todo_items` (`listId`)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        raw = helper
        return helper.writableDatabase
    }

    private fun migrate(db: SupportSQLiteDatabase, fieldName: String) {
        val migration = TodoDatabase::class.java
            .getDeclaredField(fieldName)
            .also { it.isAccessible = true }
            .get(null) as androidx.room.migration.Migration
        migration.migrate(db)
    }

    private fun columnsOf(db: SupportSQLiteDatabase, table: String): List<String> =
        db.query("PRAGMA table_info($table)").use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(1))
            }
        }

    /**
     * The DAOs are blocking and the production builder does not allow main-thread
     * queries, which is the rule that keeps them on `AppContainer.databaseDispatcher`.
     * Opening through `getInstance` is the point of these tests, so the calls move
     * off the main thread rather than the rule being relaxed for the test.
     */
    private fun <T> offMainThread(block: () -> T): T {
        var outcome: Result<T>? = null
        val worker = Thread { outcome = runCatching(block) }
        worker.start()
        worker.join()
        return checkNotNull(outcome).getOrThrow()
    }

    /**
     * `instance` is a companion property, so Kotlin puts its backing field on the
     * outer class as a static. Clearing it is what keeps one test's closed database
     * from being handed to the next one.
     */
    private fun resetSingleton() {
        listOf(TodoDatabase::class.java, TodoDatabase.Companion::class.java)
            .flatMap { it.declaredFields.asList() }
            .filter { it.name == "instance" }
            .forEach { field ->
                field.isAccessible = true
                if (Modifier.isStatic(field.modifiers)) {
                    field.set(null, null)
                } else {
                    field.set(TodoDatabase.Companion, null)
                }
            }
    }

    private companion object {
        const val DATABASE_NAME = "todo_database"
        const val LIST_ID = "list-1"
    }
}
