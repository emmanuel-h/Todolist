package fr.mandarine.todolist.domain

import java.time.LocalDate

/**
 * "What time is it" as an injected dependency rather than a static call.
 *
 * Every date comparison in the app — overdue, due today, target elapsed, notify
 * tomorrow — is a function of today's date, so calling `LocalDate.now()` directly
 * would make those behaviours untestable except on the day they happen to be true.
 * Tests pass a fake [Clock] pinned to a fixed date instead.
 *
 * [SystemClock] is the only real implementation, wired once in `AppContainer`.
 */
interface Clock {
    /** Epoch milliseconds; used for `TodoItem.completedAt` and for scheduling. */
    fun now(): Long

    /** Today in the device's default time zone. */
    fun today(): LocalDate
}

class SystemClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now()
}
