package fr.mandarine.todolist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.ui.AppExecutors
import org.junit.rules.ExternalResource
import java.util.concurrent.Executor

class MainThreadDatabaseRule : ExternalResource() {

    private lateinit var database: TodoDatabase
    private lateinit var previousExecutor: Executor

    override fun before() {
        previousExecutor = AppExecutors.database
        AppExecutors.database = Executor { command -> command.run() }
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java
        ).allowMainThreadQueries().build()
        installSingleton(database)
    }

    override fun after() {
        AppExecutors.database = previousExecutor
        installSingleton(null)
        database.close()
    }

    private fun installSingleton(instance: TodoDatabase?) {
        val field = TodoDatabase::class.java.declaredFields.first { it.name == "instance" }
        field.isAccessible = true
        field.set(null, instance)
    }
}
