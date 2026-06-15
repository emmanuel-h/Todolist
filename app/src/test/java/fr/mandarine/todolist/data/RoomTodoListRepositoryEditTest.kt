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
    fun `should call dao updateName when updateName is called`() {
        repository.updateName("1", "Supermarket")

        verify { dao.updateName("1", "Supermarket") }
    }

    @Test
    fun `should call dao updateName with another id and name`() {
        repository.updateName("list-42", "Work tasks")

        verify { dao.updateName("list-42", "Work tasks") }
    }
}
