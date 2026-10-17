package fr.mandarine.todolist.data

import fr.mandarine.todolist.domain.ListPreview
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomTodoRepositoryPreviewsUnitTest {

    private lateinit var dao: TodoItemDao
    private lateinit var repository: RoomTodoRepository

    @Before
    fun setUp() {
        dao = mockk()
        repository = RoomTodoRepository(dao)
    }

    @Test
    fun `should return empty list when no items in dao`() {
        every { dao.previewsQuery(3) } returns emptyList()

        assertTrue(repository.previewsByList(3).isEmpty())
    }

    @Test
    fun `should delegate to dao previewsQuery with the given limit`() {
        every { dao.previewsQuery(5) } returns emptyList()

        repository.previewsByList(5)

        verify { dao.previewsQuery(5) }
    }

    @Test
    fun `should group items by listId into ListPreview`() {
        every { dao.previewsQuery(3) } returns listOf(
            TodoItemEntity("a", "Alpha", "list-1", completed = false, position = 0),
            TodoItemEntity("b", "Beta", "list-1", completed = false, position = 1)
        )

        val result = repository.previewsByList(3)

        assertEquals(1, result.size)
        assertEquals("list-1", result[0].listId)
        assertEquals(listOf("Alpha", "Beta"), result[0].titles)
    }

    @Test
    fun `should produce separate ListPreview for each list`() {
        every { dao.previewsQuery(3) } returns listOf(
            TodoItemEntity("a", "Alpha", "list-1", completed = false, position = 0),
            TodoItemEntity("b", "Beta", "list-2", completed = false, position = 0)
        )

        val result = repository.previewsByList(3).associateBy { it.listId }

        assertEquals(2, result.size)
        assertEquals(listOf("Alpha"), result["list-1"]?.titles)
        assertEquals(listOf("Beta"), result["list-2"]?.titles)
    }

    @Test
    fun `should take at most limit titles per list defensively`() {
        every { dao.previewsQuery(2) } returns listOf(
            TodoItemEntity("a", "Alpha", "list-1", completed = false, position = 0),
            TodoItemEntity("b", "Beta", "list-1", completed = false, position = 1),
            TodoItemEntity("c", "Gamma", "list-1", completed = false, position = 2)
        )

        val result = repository.previewsByList(2)

        assertEquals(listOf("Alpha", "Beta"), result[0].titles)
    }

    @Test
    fun `should throw when limit is zero`() {
        try {
            repository.previewsByList(0)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun `should throw when limit is negative`() {
        try {
            repository.previewsByList(-1)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun `should map listId property of each preview correctly`() {
        every { dao.previewsQuery(1) } returns listOf(
            TodoItemEntity("x", "Only", "list-99", completed = false, position = 0)
        )

        val result = repository.previewsByList(1)

        assertEquals("list-99", result[0].listId)
    }

    @Test
    fun `should map title property of each item in preview`() {
        every { dao.previewsQuery(2) } returns listOf(
            TodoItemEntity("a", "First Title", "list-1", completed = false, position = 0),
            TodoItemEntity("b", "Second Title", "list-1", completed = false, position = 1)
        )

        val result = repository.previewsByList(2)

        assertEquals("First Title", result[0].titles[0])
        assertEquals("Second Title", result[0].titles[1])
    }
}
