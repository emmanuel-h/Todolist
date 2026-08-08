package fr.mandarine.todolist.data

import android.content.Context
import android.content.Intent
import fr.mandarine.todolist.domain.DailyNotificationWorker
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class DailyNotificationReceiverTest {

    private val context = mockk<Context>(relaxed = true)
    private val mockDb = mockk<TodoDatabase>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(TodoDatabase.Companion)
        every { TodoDatabase.getInstance(any()) } returns mockDb

        mockkConstructor(DailyNotificationWorker::class)
        every { anyConstructed<DailyNotificationWorker>().execute() } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should create DailyNotificationWorker and call execute on receive`() {
        DailyNotificationReceiver().onReceive(context, mockk<Intent>(relaxed = true))
        verify(exactly = 1) { anyConstructed<DailyNotificationWorker>().execute() }
    }

    @Test
    fun `should obtain database instance from context`() {
        DailyNotificationReceiver().onReceive(context, mockk<Intent>(relaxed = true))
        verify { TodoDatabase.getInstance(context) }
    }

    @Test
    fun `should use database todoListDao when wiring worker`() {
        DailyNotificationReceiver().onReceive(context, mockk<Intent>(relaxed = true))
        verify { mockDb.todoListDao() }
    }
}
