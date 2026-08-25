package fr.mandarine.todolist.ui

/**
 * The page of items no longer has a window of its own: it is a sheet laid over the
 * page of lists inside [TodoListsActivity], reached by putting `LIST_ID` on that
 * activity's intent. The old name stays pointed at the window that answers to it so
 * a notification built against it still opens the right page.
 */
typealias TodoListActivity = TodoListsActivity
