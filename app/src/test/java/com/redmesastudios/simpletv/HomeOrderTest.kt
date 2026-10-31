package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeOrderTest {

    private val youtube = StreamingApp("YouTube", "yt")
    private val netflix = StreamingApp("Netflix", "nf")
    private val hulu = StreamingApp("Hulu", "hu")
    private val pbs = StreamingApp("PBS", "pbs")
    private val curated = listOf(youtube, netflix, hulu, pbs)

    @Test
    fun mostWatchedComesFirst() {
        val ranked = rankByWatchTime(curated, mapOf("hu" to 9_000L, "yt" to 50_000L, "nf" to 2_000L))
        assertEquals(listOf(youtube, hulu, netflix, pbs), ranked)
    }

    @Test
    fun appsWithNoWatchTimeKeepTheCuratedOrder() {
        assertEquals(curated, rankByWatchTime(curated, emptyMap()))
        assertEquals(listOf(pbs, youtube, netflix, hulu), rankByWatchTime(curated, mapOf("pbs" to 1L)))
    }

    @Test
    fun savedOrderPutsNewAppsLastAndDropsRemovedOnes() {
        val saved = listOf("hu", "gone", "yt")
        assertEquals(listOf(hulu, youtube, netflix, pbs), inSavedOrder(curated, saved))
    }
}
