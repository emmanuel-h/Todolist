package fr.mandarine.todolist

import android.app.Application
import com.google.android.material.color.DynamicColors

class TodoListApplication : Application() {
    var container: AppContainer = AppContainer(this)

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
