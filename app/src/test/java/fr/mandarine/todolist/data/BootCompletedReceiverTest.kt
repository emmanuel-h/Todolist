package fr.mandarine.todolist.data

import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class BootCompletedReceiverTest {

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkConstructor(AndroidNotificationScheduler::class)
        every { anyConstructed<AndroidNotificationScheduler>().scheduleNextDailyCheck() } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should schedule alarm when action is BOOT_COMPLETED`() {
        val intent = mockk<Intent> { every { action } returns "android.intent.action.BOOT_COMPLETED" }
        BootCompletedReceiver().onReceive(context, intent)
        verify(exactly = 1) { anyConstructed<AndroidNotificationScheduler>().scheduleNextDailyCheck() }
    }

    @Test
    fun `should not schedule alarm when action is not BOOT_COMPLETED`() {
        val intent = mockk<Intent> { every { action } returns "android.intent.action.PACKAGE_ADDED" }
        BootCompletedReceiver().onReceive(context, intent)
        verify(exactly = 0) { anyConstructed<AndroidNotificationScheduler>().scheduleNextDailyCheck() }
    }

    @Test
    fun `should not schedule alarm when action is null`() {
        val intent = mockk<Intent> { every { action } returns null }
        BootCompletedReceiver().onReceive(context, intent)
        verify(exactly = 0) { anyConstructed<AndroidNotificationScheduler>().scheduleNextDailyCheck() }
    }
}
