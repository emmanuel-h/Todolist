package fr.mandarine.todolist.ui

import android.Manifest
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.todolist.MainThreadDatabaseRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
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

    private fun app() = ApplicationProvider.getApplicationContext<android.app.Application>()

    /**
     * The ask is only put when a notification could not otherwise arrive, so the
     * shadow has to say that it cannot. Robolectric leaves notifications enabled
     * by default no matter what the permission says.
     */
    private fun silenceNotifications() {
        Shadows.shadowOf(app().getSystemService(android.app.NotificationManager::class.java))
            .setNotificationsEnabled(false)
    }

    private fun requestedPermissions(activity: TodoListsActivity): List<String> =
        Shadows.shadowOf(activity).lastRequestedPermission
            ?.requestedPermissions
            ?.toList()
            .orEmpty()

    @Test
    fun `should ask for notifications when the first reminder is set`() {
        silenceNotifications()

        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.askForNotifications()

                assertNotNull(
                    "Expected a runtime permission request on API 34",
                    Shadows.shadowOf(activity).lastRequestedPermission
                )
                assertTrue(
                    "POST_NOTIFICATIONS must be in the requested permissions array",
                    requestedPermissions(activity).contains(Manifest.permission.POST_NOTIFICATIONS)
                )
            }
        }
    }

    @Test
    fun `should not ask for notifications a second time once the ask has been spent`() {
        silenceNotifications()
        NotificationAsk(app()).markAsked()

        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.askForNotifications()

                assertNull(
                    "A refusal must be respected without a second prompt",
                    Shadows.shadowOf(activity).lastRequestedPermission
                )
            }
        }
    }

    @Test
    fun `should remember that the ask was spent`() {
        val ask = NotificationAsk(app())

        assertFalse(ask.alreadyAsked())

        ask.markAsked()

        assertTrue(NotificationAsk(app()).alreadyAsked())
    }

    @Test
    fun `should not ask for notifications when they are already granted`() {
        Shadows.shadowOf(app()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ActivityScenario.launch(TodoListsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.askForNotifications()

                assertFalse(
                    "Should not re-request POST_NOTIFICATIONS when already granted",
                    requestedPermissions(activity).contains(Manifest.permission.POST_NOTIFICATIONS)
                )
            }
        }
    }

    @Test
    fun `should ask on Android 13 and later when the permission is neither granted nor spent`() {
        assertTrue(
            shouldAskForNotifications(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                granted = false,
                asked = false
            )
        )
    }

    @Test
    fun `should never ask below Android 13 because notifications need no permission there`() {
        assertFalse(
            shouldAskForNotifications(
                sdkInt = Build.VERSION_CODES.S_V2,
                granted = false,
                asked = false
            )
        )
    }

    @Test
    fun `should not ask when the permission is already granted`() {
        assertFalse(
            shouldAskForNotifications(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                granted = true,
                asked = false
            )
        )
    }

    @Test
    fun `should not ask when the ask has already been spent`() {
        assertFalse(
            shouldAskForNotifications(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                granted = false,
                asked = true
            )
        )
    }
}
