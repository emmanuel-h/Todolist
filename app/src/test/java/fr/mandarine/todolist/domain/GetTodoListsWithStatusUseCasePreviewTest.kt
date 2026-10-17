package fr.mandarine.todolist.domain

import fr.mandarine.todolist.FakeClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetTodoListsWithStatusUseCasePreviewTest {

    private lateinit var todoListRepository: TodoListRepository
    private lateinit var todoRepository: TodoRepository
    private lateinit var useCase: GetTodoListsWithStatusUseCase

    @Before
    fun setUp() {
        todoListRepository = mockk()
        todoRepository = mockk()
        every { todoRepository.countsByList() } returns emptyList()
        useCase = GetTodoListsWithStatusUseCase(todoListRepository, todoRepository, FakeClock())
    }

    @Test
    fun `should call previewsByList with PREVIEW_ITEMS`() {
        every { todoListRepository.getAll() } returns emptyList()
        every { todoRepository.previewsByList(GetTodoListsWithStatusUseCase.PREVIEW_ITEMS) } returns emptyList()

        useCase()

        verify { todoRepository.previewsByList(GetTodoListsWithStatusUseCase.PREVIEW_ITEMS) }
    }

    @Test
    fun `should return empty preview when no ListPreview exists for a list`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.previewsByList(GetTodoListsWithStatusUseCase.PREVIEW_ITEMS) } returns emptyList()

        val result = useCase()

        assertEquals(emptyList<String>(), result[0].preview)
    }

    @Test
    fun `should fill preview with titles from matching ListPreview`() {
        val list = TodoList("list-1", "Groceries")
        every { todoListRepository.getAll() } returns listOf(list)
        every { todoRepository.previewsByList(GetTodoListsWithStatusUseCase.PREVIEW_ITEMS) } returns listOf(
            ListPreview("list-1", listOf("Milk", "Eggs", "Bread"))
        )

        val result = useCase()

        assertEquals(listOf("Milk", "Eggs", "Bread"), result[0].preview)
    }

    @Test
    fun `should fill preview independently for each list`() {
        val listA = TodoList("list-a", "Groceries")
        val listB = TodoList("list-b", "Work")
        every { todoListRepository.getAll() } returns listOf(listA, listB)
        every { todoRepository.previewsByList(GetTodoListsWithStatusUseCase.PREVIEW_ITEMS) } returns listOf(
            ListPreview("list-a", listOf("Milk")),
            ListPreview("list-b", listOf("Report", "Call"))
        )

        val result = useCase()

        assertEquals(listOf("Milk"), result[0].preview)
        assertEquals(listOf("Report", "Call"), result[1].preview)
    }

    @Test
    fun `should return empty preview for list with no matching preview entry`() {
        val listA = TodoList("list-a", "Groceries")
        val listB = TodoList("list-b", "Work")
        every { todoListRepository.getAll() } returns listOf(listA, listB)
        every { todoRepository.previewsByList(GetTodoListsWithStatusUseCase.PREVIEW_ITEMS) } returns listOf(
            ListPreview("list-b", listOf("Report"))
        )

        val result = useCase()

        assertEquals(emptyList<String>(), result[0].preview)
        assertEquals(listOf("Report"), result[1].preview)
    }
}
