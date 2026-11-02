package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedRecordsTest {
    @Test
    fun theNewestCopyOfEachRecordWins() {
        val mine = listOf(SharedRecord("a", 10, "mine-a"), SharedRecord("b", 30, "mine-b"))
        val theirs = listOf(SharedRecord("a", 20, "their-a"), SharedRecord("b", 5, "their-b"), SharedRecord("c", 1, "their-c"))
        assertEquals(listOf("their-a", "mine-b", "their-c"), mergeShared(mine, theirs).map { it.json })
    }

    @Test
    fun onATieMineStays() {
        assertEquals("mine", mergeShared(listOf(SharedRecord("a", 10, "mine")), listOf(SharedRecord("a", 10, "theirs"))).single().json)
    }

    @Test
    fun collectionNamesAreSafeFileNames() {
        assertTrue(isCollectionName("trivia-games"))
        assertFalse(isCollectionName("../photos"))
        assertFalse(isCollectionName(""))
    }
}
