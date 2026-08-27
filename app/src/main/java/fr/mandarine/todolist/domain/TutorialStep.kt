package fr.mandarine.todolist.domain

/**
 * The scenes the tour is driven through after its opening one, named for what
 * each actually does on the page.
 *
 * They used to be named for what the scene *before* them had done — the step
 * called `SET_DUE_DATE` opened a list, the one called `OPEN_LIST` wrote items into
 * it — which made every reference here a small lie and the script hard to follow
 * against the screen it drives.
 */
enum class TutorialStep {
    A_DAY_AND_A_NOTE,
    OPEN_IT,
    WRITE_ITEMS,
    TICK_AND_MOVE,
    EDIT_AND_TEAR
}
