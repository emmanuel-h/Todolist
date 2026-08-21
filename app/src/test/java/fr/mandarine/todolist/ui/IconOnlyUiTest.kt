package fr.mandarine.todolist.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.google.android.material.appbar.MaterialToolbar
import fr.mandarine.todolist.R
import fr.mandarine.todolist.presentation.TodoListState
import fr.mandarine.todolist.ui.paper.PaperTheme
import fr.mandarine.todolist.ui.todolist.SectionDivider
import fr.mandarine.todolist.ui.todolist.TodoListScreen
import fr.mandarine.todolist.ui.todolist.TodoListScreenState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconOnlyUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var themedContext: Context

    @Before
    fun setUp() {
        themedContext = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_ToDoList
        )
    }

    @Test
    fun `should show only numeric count in divider label without the word Completed`() {
        composeRule.setContent { PaperTheme { SectionDivider(completedCount = 2) } }

        assertEquals(listOf("2"), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    @Test
    fun `should show count of one in divider label when exactly one completed item exists`() {
        composeRule.setContent { PaperTheme { SectionDivider(completedCount = 1) } }

        assertEquals(listOf("1"), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    @Test
    fun `should not contain the word Completed in divider label text`() {
        composeRule.setContent { PaperTheme { SectionDivider(completedCount = 12) } }

        val labels = composeRule.onRoot().fetchSemanticsNode().staticText().map { it.lowercase() }

        assert(labels.none { it.contains("completed") }) {
            "Expected divider label to not contain 'completed' but was: $labels"
        }
    }

    @Test
    fun `should not contain any static text in the empty state of todo list`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        assertEquals(listOf(GHOST_HINT), composeRule.onRoot().fetchSemanticsNode().staticText())
    }

    @Test
    fun `should expose only the back affordance in the empty state of todo list`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        assertEquals(listOf(BACK_DESCRIPTION), composeRule.onRoot().fetchSemanticsNode().contentDescriptions())
    }

    @Test
    fun `should show the ghost row as the only row when the list has no items`() {
        composeRule.setContent { PaperTheme { EmptyItemsScreen() } }

        composeRule.onNodeWithText(GHOST_HINT).assertIsDisplayed()
    }

    @Test
    fun `should not contain any static text in the empty state of lists screen`() {
        val parent = FrameLayout(themedContext)
        val contentView = LayoutInflater.from(themedContext)
            .inflate(R.layout.activity_todo_lists, parent, false)

        assertEquals(emptyList<String>(), staticTextIn(contentView))
    }

    @Test
    fun `should not contain a background illustration in the lists screen`() {
        val parent = FrameLayout(themedContext)
        val listsView = LayoutInflater.from(themedContext)
            .inflate(R.layout.activity_todo_lists, parent, false)

        assertEquals(emptyList<String>(), decorativeImagesIn(listsView))
    }

    @Test
    fun `should not have a toolbar in the lists screen`() {
        val parent = FrameLayout(themedContext)
        val listsView = LayoutInflater.from(themedContext)
            .inflate(R.layout.activity_todo_lists, parent, false)

        assertNull("Expected no toolbar in the lists screen", toolbarIn(listsView))
    }

    @Test
    fun `should have confirm icon button in create list dialog layout`() {
        val parent = FrameLayout(themedContext)
        val dialogView = LayoutInflater.from(themedContext)
            .inflate(R.layout.dialog_create_list, parent, false)

        val confirmBtn = dialogView.findViewById<android.view.View>(R.id.btnDialogConfirm)
        assertNotNull("Expected btnDialogConfirm to exist in dialog_create_list layout", confirmBtn)
    }

    @Test
    fun `should have cancel icon button in create list dialog layout`() {
        val parent = FrameLayout(themedContext)
        val dialogView = LayoutInflater.from(themedContext)
            .inflate(R.layout.dialog_create_list, parent, false)

        val cancelBtn = dialogView.findViewById<android.view.View>(R.id.btnDialogCancel)
        assertNotNull("Expected btnDialogCancel to exist in dialog_create_list layout", cancelBtn)
    }

    @Test
    fun `should have icon-only confirm button in list row delete confirm strip`() {
        val parent = FrameLayout(themedContext)
        val rowView = LayoutInflater.from(themedContext)
            .inflate(R.layout.item_todo_list, parent, false)

        val confirmBtn = rowView.findViewById<android.widget.ImageButton>(R.id.btnDeleteConfirm)
        assertNotNull("Expected btnDeleteConfirm to exist in item_todo_list layout", confirmBtn)
    }

    @Test
    fun `should have icon-only cancel button in list row delete confirm strip`() {
        val parent = FrameLayout(themedContext)
        val rowView = LayoutInflater.from(themedContext)
            .inflate(R.layout.item_todo_list, parent, false)

        val cancelBtn = rowView.findViewById<android.widget.ImageButton>(R.id.btnDeleteCancel)
        assertNotNull("Expected btnDeleteCancel to exist in item_todo_list layout", cancelBtn)
    }

    private fun staticTextIn(view: android.view.View): List<String> = when {
        view is android.widget.TextView && view.text.isNotBlank() -> listOf(view.text.toString())
        view is android.view.ViewGroup ->
            (0 until view.childCount).flatMap { staticTextIn(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun decorativeImagesIn(view: android.view.View): List<String> = when {
        view is android.widget.ImageView && view.contentDescription.isNullOrBlank() ->
            listOf(view.javaClass.simpleName)
        view is android.view.ViewGroup ->
            (0 until view.childCount).flatMap { decorativeImagesIn(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun toolbarIn(view: android.view.View): MaterialToolbar? = when {
        view is MaterialToolbar -> view
        view is android.view.ViewGroup ->
            (0 until view.childCount).firstNotNullOfOrNull { toolbarIn(view.getChildAt(it)) }
        else -> null
    }

    private companion object {
        const val GHOST_HINT = "…"
        const val BACK_DESCRIPTION = "Navigate up"
    }
}

@Composable
private fun EmptyItemsScreen() {
    TodoListScreen(
        listName = "",
        state = TodoListState.Empty,
        screenState = remember { TodoListScreenState() },
        onBack = {},
        onToggle = {},
        onEdit = { _, _ -> },
        onDelete = {},
        onSubmitInline = {},
        onReorder = { _, _ -> }
    )
}

internal fun SemanticsNode.staticText(): List<String> =
    config.getOrNull(SemanticsProperties.Text).orEmpty()
        .map { it.text }
        .filter { it.isNotBlank() } + children.flatMap { it.staticText() }

internal fun SemanticsNode.contentDescriptions(): List<String> =
    config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        .filter { it.isNotBlank() } + children.flatMap { it.contentDescriptions() }
