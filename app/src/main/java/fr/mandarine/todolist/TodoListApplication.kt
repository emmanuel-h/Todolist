package fr.mandarine.todolist

import android.app.Application

class TodoListApplication : Application() {
    var container: AppContainer = AppContainer(this)
}
