package fr.mandarine.todolist.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TutorialScriptTest {

    @Test
    fun `should create script when steps list is not empty`() {
        val script = TutorialScript(listOf(TutorialStep.A_DAY_AND_A_NOTE))
        assertEquals(1, script.steps.size)
    }

    @Test
    fun `should throw when steps list is empty`() {
        assertThrows(IllegalArgumentException::class.java) {
            TutorialScript(emptyList())
        }
    }

    @Test
    fun `should expose all provided steps`() {
        val steps = listOf(TutorialStep.A_DAY_AND_A_NOTE, TutorialStep.OPEN_IT)
        val script = TutorialScript(steps)
        assertEquals(steps, script.steps)
    }

    @Test
    fun `should have exactly five steps in default script`() {
        assertEquals(5, TutorialScript.defaultScript().steps.size)
    }

    @Test
    fun `should have CREATE_LIST as first step in default script`() {
        assertEquals(TutorialStep.A_DAY_AND_A_NOTE, TutorialScript.defaultScript().steps[0])
    }

    @Test
    fun `should have SET_DUE_DATE as second step in default script`() {
        assertEquals(TutorialStep.OPEN_IT, TutorialScript.defaultScript().steps[1])
    }

    @Test
    fun `should have OPEN_LIST as third step in default script`() {
        assertEquals(TutorialStep.WRITE_ITEMS, TutorialScript.defaultScript().steps[2])
    }

    @Test
    fun `should have COMPLETE_AND_REORDER as fourth step in default script`() {
        assertEquals(TutorialStep.TICK_AND_MOVE, TutorialScript.defaultScript().steps[3])
    }

    @Test
    fun `should have DELETE_LIST as last step in default script`() {
        assertEquals(TutorialStep.EDIT_AND_TEAR, TutorialScript.defaultScript().steps[4])
    }
}
