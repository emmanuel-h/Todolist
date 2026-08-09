package fr.mandarine.todolist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.data.TodoDatabase
import fr.mandarine.todolist.domain.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import org.junit.rules.ExternalResource

class MainThreadDatabaseRule : ExternalResource() {

    lateinit var database: TodoDatabase
        private set
    private lateinit var previousContainer: AppContainer

    override fun before() {
        val application = ApplicationProvider.getApplicationContext<TodoListApplication>()
        database = Room.inMemoryDatabaseBuilder(application, TodoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        previousContainer = application.container
        application.container = AppContainer(
            application,
            databaseFactory = { database },
            schedulerFactory = { NotificationScheduler { } },
            databaseDispatcher = Dispatchers.Unconfined
        )
    }

    override fun after() {
        ApplicationProvider.getApplicationContext<TodoListApplication>().container = previousContainer
        database.close()
    }
}
