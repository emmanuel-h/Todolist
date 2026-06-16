package fr.mandarine.todolist.data

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class RoomTodoListRepositoryShiftTest {

    private lateinit var dao: TodoListDao
    private lateinit var repository: RoomTodoListRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = RoomTodoListRepository(dao)
    }

    @Test
    fun `should call dao incrementAllPositions when shiftAllPositionsUp is called`() {
        repository.shiftAllPositionsUp()
        verify { dao.incrementAllPositions() }
    }
}
