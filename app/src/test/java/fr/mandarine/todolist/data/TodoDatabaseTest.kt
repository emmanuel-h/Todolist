package fr.mandarine.todolist.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoDatabaseTest {

    private var openedDb: TodoDatabase? = null

    @After
    fun tearDown() {
        openedDb?.close()
        resetSingleton()
    }

    private fun resetSingleton() {
        val companionClass = TodoDatabase.Companion::class.java
        companionClass.declaredFields.firstOrNull { it.name == "instance" }?.let { field ->
            field.isAccessible = true
            field.set(TodoDatabase.Companion, null)
        }
    }

    @Test
    fun `should return a non-null database instance`() {
        val db = TodoDatabase.getInstance(ApplicationProvider.getApplicationContext())
        openedDb = db
        assertNotNull(db)
    }

    @Test
    fun `should return the same instance on subsequent calls`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db1 = TodoDatabase.getInstance(context)
        val db2 = TodoDatabase.getInstance(context)
        openedDb = db1
        assertSame(db1, db2)
    }
}
