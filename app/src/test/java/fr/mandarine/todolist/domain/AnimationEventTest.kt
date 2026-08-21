package fr.mandarine.todolist.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimationEventTest {

    @Test
    fun `ItemAdded should store and return itemId`() {
        val event = AnimationEvent.ItemAdded("id-1")
        assertEquals("id-1", event.itemId)
    }

    @Test
    fun `ItemCompleted should store and return itemId`() {
        val event = AnimationEvent.ItemCompleted("id-2")
        assertEquals("id-2", event.itemId)
    }

    @Test
    fun `ItemRestored should store and return itemId`() {
        val event = AnimationEvent.ItemRestored("id-3")
        assertEquals("id-3", event.itemId)
    }

    @Test
    fun `ItemDeleted should store and return itemId`() {
        val event = AnimationEvent.ItemDeleted("id-4")
        assertEquals("id-4", event.itemId)
    }

    @Test
    fun `ItemAdded instances with the same id should be equal`() {
        assertEquals(AnimationEvent.ItemAdded("x"), AnimationEvent.ItemAdded("x"))
    }

    @Test
    fun `ItemAdded instances with different ids should not be equal`() {
        val a = AnimationEvent.ItemAdded("a")
        val b = AnimationEvent.ItemAdded("b")
        assert(a != b)
    }

    @Test
    fun `ItemCompleted instances with the same id should be equal`() {
        assertEquals(AnimationEvent.ItemCompleted("x"), AnimationEvent.ItemCompleted("x"))
    }

    @Test
    fun `ItemCompleted instances with different ids should not be equal`() {
        val a = AnimationEvent.ItemCompleted("a")
        val b = AnimationEvent.ItemCompleted("b")
        assert(a != b)
    }

    @Test
    fun `ItemRestored instances with the same id should be equal`() {
        assertEquals(AnimationEvent.ItemRestored("x"), AnimationEvent.ItemRestored("x"))
    }

    @Test
    fun `ItemRestored instances with different ids should not be equal`() {
        val a = AnimationEvent.ItemRestored("a")
        val b = AnimationEvent.ItemRestored("b")
        assert(a != b)
    }

    @Test
    fun `ItemDeleted instances with the same id should be equal`() {
        assertEquals(AnimationEvent.ItemDeleted("x"), AnimationEvent.ItemDeleted("x"))
    }

    @Test
    fun `ItemDeleted instances with different ids should not be equal`() {
        val a = AnimationEvent.ItemDeleted("a")
        val b = AnimationEvent.ItemDeleted("b")
        assert(a != b)
    }

    @Test
    fun `ListAdded should be the singleton object`() {
        assertEquals(AnimationEvent.ListAdded, AnimationEvent.ListAdded)
    }
}
