package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SyncTest {
    private fun show(id: Int) = Title(tmdbId = id, kind = TitleKind.Show, name = "Show $id")
    private fun at(seconds: Long) = Instant.ofEpochSecond(1_800_000_000 + seconds)

    @Test
    fun watchListsMergeNewestFirstWithEachShowOnce() {
        val mine = listOf(Watched(show(1), at(30)), Watched(show(2), at(10)))
        val theirs = listOf(Watched(show(2), at(40)), Watched(show(3), at(20)))

        val merged = mergeWatched(mine, theirs)

        assertEquals(listOf(2, 1, 3), merged.map { it.title.tmdbId })
        assertEquals(at(40), merged.first().at)
    }

    @Test
    fun mergingTheSameListChangesNothing() {
        val list = listOf(Watched(show(1), at(30)), Watched(show(2), at(10)))
        assertEquals(list, mergeWatched(list, list))
    }

    @Test
    fun photosTheOtherStickHasAreFetched() {
        val plan = planPhotos(mine = setOf("a"), myRemoved = emptyMap(), theirs = setOf("a", "b"), theirRemoved = emptyMap())
        assertEquals(listOf("b"), plan.fetch)
        assertEquals(emptyList<String>(), plan.drop)
    }

    @Test
    fun aPhotoRemovedOnEitherStickIsRemovedEverywhereAndNeverFetchedBack() {
        val removedThere = planPhotos(mine = setOf("a", "b"), myRemoved = emptyMap(), theirs = setOf("a"), theirRemoved = mapOf("b" to 5L))
        assertEquals(listOf("b"), removedThere.drop)
        assertEquals(mapOf("b" to 5L), removedThere.removed)

        val removedHere = planPhotos(mine = setOf("a"), myRemoved = mapOf("b" to 5L), theirs = setOf("a", "b"), theirRemoved = emptyMap())
        assertEquals(emptyList<String>(), removedHere.fetch)
    }
}
