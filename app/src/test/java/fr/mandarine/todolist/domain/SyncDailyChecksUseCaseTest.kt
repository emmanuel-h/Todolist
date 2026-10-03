package fr.mandarine.todolist.domain

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalTime
import org.junit.Before
import org.junit.Test

class SyncDailyChecksUseCaseTest {

    private lateinit var repository: TodoListRepository
    private lateinit var scheduler: NotificationScheduler
    private lateinit var useCase: SyncDailyChecksUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        useCase = SyncDailyChecksUseCase(repository, scheduler)
    }

    @Test
    fun `should call ensureDailyChecks with empty set when no list has its own time`() {
        io.mockk.every { repository.getAll() } returns listOf(
            TodoList("1", "Groceries"),
            TodoList("2", "Home")
        )

        useCase()

        verify { scheduler.ensureDailyChecks(emptySet()) }
    }

    @Test
    fun `should call ensureDailyChecks with one own time when one list has its own time`() {
        val ownTime = LocalTime.of(7, 30)
        io.mockk.every { repository.getAll() } returns listOf(
            TodoList("1", "Groceries", reminderTime = ownTime),
            TodoList("2", "Home")
        )

        useCase()

        verify { scheduler.ensureDailyChecks(setOf(ownTime)) }
    }

    @Test
    fun `should call ensureDailyChecks with one time when two lists share the same own time`() {
        val sharedTime = LocalTime.of(7, 30)
        io.mockk.every { repository.getAll() } returns listOf(
            TodoList("1", "Groceries", reminderTime = sharedTime),
            TodoList("2", "Home", reminderTime = sharedTime)
        )

        useCase()

        verify { scheduler.ensureDailyChecks(setOf(sharedTime)) }
    }

    @Test
    fun `should call ensureDailyChecks with all distinct own times`() {
        val time1 = LocalTime.of(7, 30)
        val time2 = LocalTime.of(9, 15)
        io.mockk.every { repository.getAll() } returns listOf(
            TodoList("1", "Groceries", reminderTime = time1),
            TodoList("2", "Home", reminderTime = time2),
            TodoList("3", "Work")
        )

        useCase()

        verify { scheduler.ensureDailyChecks(setOf(time1, time2)) }
    }

    @Test
    fun `should call ensureDailyChecks with empty set when list is empty`() {
        io.mockk.every { repository.getAll() } returns emptyList()

        useCase()

        verify { scheduler.ensureDailyChecks(emptySet()) }
    }
}
