# Room reads and writes the entities through generated code that R8 rewrites with
# them, so only their shape has to survive: the columns are named after the fields.
# Everything else the app owns is reached by name from Kotlin, or by the keep rules
# Room and WorkManager ship themselves — the database subclass Room loads by name,
# and the worker WorkManager instantiates from the class name it stored.
-keep class fr.mandarine.todolist.data.TodoListEntity { *; }
-keep class fr.mandarine.todolist.data.TodoItemEntity { *; }
-keep class fr.mandarine.todolist.data.TodoCountsRow { *; }
