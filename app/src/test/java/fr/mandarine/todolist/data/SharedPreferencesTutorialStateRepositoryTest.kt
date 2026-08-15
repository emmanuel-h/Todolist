package fr.mandarine.todolist.data

import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.TodoListApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedPreferencesTutorialStateRepositoryTest {

    private lateinit var repository: SharedPreferencesTutorialStateRepository

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<TodoListApplication>()
        application.getSharedPreferences("tutorial_state", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        repository = SharedPreferencesTutorialStateRepository(application)
    }

    @Test
    fun `should return false for isTutorialSeen by default`() {
        assertFalse(repository.isTutorialSeen())
    }

    @Test
    fun `should return true after markTutorialSeen is called`() {
        repository.markTutorialSeen()

        assertTrue(repository.isTutorialSeen())
    }

    @Test
    fun `should persist seen flag across repository instances`() {
        val application = ApplicationProvider.getApplicationContext<TodoListApplication>()
        repository.markTutorialSeen()

        val anotherInstance = SharedPreferencesTutorialStateRepository(application)

        assertTrue(anotherInstance.isTutorialSeen())
    }

    @Test
    fun `should return null for getPendingDemoListId by default`() {
        assertNull(repository.getPendingDemoListId())
    }

    @Test
    fun `should return saved id after savePendingDemoListId is called`() {
        repository.savePendingDemoListId("list-123")

        assertEquals("list-123", repository.getPendingDemoListId())
    }

    @Test
    fun `should overwrite id when savePendingDemoListId is called again`() {
        repository.savePendingDemoListId("list-first")
        repository.savePendingDemoListId("list-second")

        assertEquals("list-second", repository.getPendingDemoListId())
    }

    @Test
    fun `should return null after clearPendingDemoListId is called`() {
        repository.savePendingDemoListId("list-456")
        repository.clearPendingDemoListId()

        assertNull(repository.getPendingDemoListId())
    }

    @Test
    fun `should persist demo list id across repository instances`() {
        val application = ApplicationProvider.getApplicationContext<TodoListApplication>()
        repository.savePendingDemoListId("list-persist")

        val anotherInstance = SharedPreferencesTutorialStateRepository(application)

        assertEquals("list-persist", anotherInstance.getPendingDemoListId())
    }
}
