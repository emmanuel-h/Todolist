package fr.mandarine.todolist.domain

data class TutorialScript(val steps: List<TutorialStep>) {
    init {
        require(steps.isNotEmpty()) { "Tutorial script must have at least one step" }
    }

    companion object {
        fun defaultScript(): TutorialScript = TutorialScript(
            listOf(
                TutorialStep.CREATE_LIST,
                TutorialStep.SET_DUE_DATE,
                TutorialStep.OPEN_LIST,
                TutorialStep.COMPLETE_AND_REORDER,
                TutorialStep.DELETE_LIST
            )
        )
    }
}
