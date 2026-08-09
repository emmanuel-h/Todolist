package fr.mandarine.todolist.data

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class RoomTodoListRepositoryEditTest {

    private lateinit var dao: TodoListDao
    private lateinit var repository: RoomTodoListRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = RoomTodoListRepository(dao)
    }

    @Test
    fun `should call dao update when update is called`() {
        repository.update("1", "Supermarket", null, null)

        verify { dao.update("1", "Supermarket", null, null) }
    }

    @Test
    fun `should call dao update with another id and name`() {
        repository.update("list-42", "Work tasks", null, null)

        verify { dao.update("list-42", "Work tasks", null, null) }
    }
}
