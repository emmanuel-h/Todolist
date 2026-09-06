package fr.mandarine.todolist

import android.app.Application

/**
 * The process-wide object Android creates before any activity, registered in
 * `AndroidManifest.xml` under `android:name`.
 *
 * Its only job is to own the [AppContainer]. Anything that needs a repository —
 * an activity, or the WorkManager worker running with no activity at all — reaches
 * it as `(applicationContext as TodoListApplication).container`.
 *
 * [container] is a `var` rather than a `val` purely so instrumentation tests can
 * swap in a container built over an in-memory database before the activity starts.
 */
class TodoListApplication : Application() {
    var container: AppContainer = AppContainer(this)
}
