package fr.mandarine.todolist.domain

import java.time.LocalDate

interface Clock {
    fun now(): Long
    fun today(): LocalDate
}

class SystemClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now()
}
