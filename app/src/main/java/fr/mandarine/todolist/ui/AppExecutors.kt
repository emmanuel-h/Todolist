package fr.mandarine.todolist.ui

import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal object AppExecutors {

    @Volatile
    var database: Executor = Executors.newSingleThreadExecutor()
}
