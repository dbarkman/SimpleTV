package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Test

class HomePagesTest {
    @Test
    fun tenOrFewerFitOnOnePage() {
        assertEquals(listOf((1..10).toList()), homePages((1..10).toList()))
        assertEquals(listOf(emptyList<Int>()), homePages(emptyList<Int>()))
    }

    @Test
    fun pagesWithMoreAfterThemHoldNine() {
        val pages = homePages((1..13).toList())
        assertEquals(listOf((1..9).toList(), (10..13).toList()), pages)
        val many = homePages((1..30).toList())
        assertEquals(listOf(9, 9, 9, 3), many.map { it.size })
        assertEquals((1..30).toList(), many.flatten())
    }
}
