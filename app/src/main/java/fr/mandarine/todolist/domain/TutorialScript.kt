package fr.mandarine.todolist.domain

data class TutorialScript(val steps: List<TutorialStep>) {
    init {
        require(steps.isNotEmpty()) { "Tutorial script must have at least one step" }
    }

    companion object {
        fun defaultScript(): TutorialScript = TutorialScript(
            listOf(
                TutorialStep.A_DAY_AND_A_NOTE,
                TutorialStep.OPEN_IT,
                TutorialStep.WRITE_ITEMS,
                TutorialStep.TICK_AND_MOVE,
                TutorialStep.EDIT_AND_TEAR
            )
        )
    }
}
