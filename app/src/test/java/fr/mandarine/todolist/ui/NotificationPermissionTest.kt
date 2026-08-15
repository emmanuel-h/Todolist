package fr.mandarine.todolist.ui

import fr.mandarine.todolist.MainThreadDatabaseRule
import org.junit.Rule
import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.data.SharedPreferencesTutorialStateRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationPermissionTest {

    @get:Rule
    val databaseRule = MainThreadDatabaseRule()

    private fun markTutorialSeen() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        SharedPreferencesTutorialStateRepository(app).markTutorialSeen()
    }

    @Test
    fun `should request POST_NOTIFICATIONS permission on Android 13+ when not yet granted`() {
        markTutorialSeen()

        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val shadow = Shadows.shadowOf(activity)
                val permRequest = shadow.lastRequestedPermission
                assertNotNull("Expected a runtime permission request on API 34", permRequest)
                assertTrue(
                    "POST_NOTIFICATIONS must be in the requested permissions array",
                    permRequest!!.requestedPermissions.contains(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }
        }
    }

    @Test
    fun `should not request POST_NOTIFICATIONS permission when already granted`() {
        markTutorialSeen()
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val shadow = Shadows.shadowOf(activity)
                val permRequest = shadow.lastRequestedPermission
                val includedPostNotifications = permRequest
                    ?.requestedPermissions
                    ?.contains(Manifest.permission.POST_NOTIFICATIONS)
                    ?: false
                assertFalse(
                    "Should not re-request POST_NOTIFICATIONS when already granted",
                    includedPostNotifications
                )
            }
        }
    }

    @Test
    fun `should defer POST_NOTIFICATIONS request while first-launch tutorial is running`() {
        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val shadow = Shadows.shadowOf(activity)
                assertNull(
                    "Permission request must wait until the tutorial is dismissed",
                    shadow.lastRequestedPermission
                )
            }
        }
    }
}
