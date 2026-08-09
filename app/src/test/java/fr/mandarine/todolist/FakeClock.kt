package fr.mandarine.todolist

import fr.mandarine.todolist.domain.Clock
import java.time.LocalDate

class FakeClock(
    var nowMillis: Long = 0L,
    var todayDate: LocalDate = LocalDate.ofEpochDay(0)
) : Clock {
    override fun now(): Long = nowMillis
    override fun today(): LocalDate = todayDate
}
