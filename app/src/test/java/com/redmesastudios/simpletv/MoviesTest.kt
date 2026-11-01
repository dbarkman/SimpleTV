package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MoviesTest {
    private val netflix = StreamingApp("Netflix", "com.netflix.ninja", tmdbProviders = setOf(8))
    private val prime = StreamingApp("Prime Video", "com.amazon.amazonvideo.livingroom", tmdbProviders = setOf(9))
    private val fandango = StreamingApp("Fandango at Home", FandangoPackage, tmdbProviders = setOf(7, 332))
    private val today = LocalDate.of(2026, 10, 31)

    private fun movie(homeDate: LocalDate?) = HomeMovie(Title(1, TitleKind.Movie, "Verity", year = "2026"), homeDate)

    @Test
    fun yourOwnServiceComesFirst() {
        val offers = listOf(Offer(7, "Fandango At Home", rentOrBuy = true), Offer(8, "Netflix", rentOrBuy = false))
        assertEquals(MovieWatch.Stream(netflix), movieWatch(offers, listOf(fandango, netflix)))
    }

    @Test
    fun anythingForRentGoesToFandango() {
        // Fandango rents it even when the guide only lists Apple TV's store.
        val offers = listOf(Offer(2, "Apple TV Store", rentOrBuy = true))
        assertEquals(MovieWatch.Rent(fandango), movieWatch(offers, listOf(netflix, fandango)))
    }

    @Test
    fun aMovieTheGuideDoesntPlaceYetStillTriesFandango() {
        assertEquals(MovieWatch.Rent(fandango), movieWatch(emptyList(), listOf(netflix, fandango)))
    }

    @Test
    fun withoutFandangoAnotherStoreOnTheTvRents() {
        val offers = listOf(Offer(9, "Amazon Video", rentOrBuy = true))
        assertEquals(MovieWatch.Rent(prime), movieWatch(offers, listOf(netflix, prime)))
    }

    @Test
    fun aStreamingOnlyMovieOnAnotherServiceSaysWhere() {
        val offers = listOf(Offer(1899, "HBO Max", rentOrBuy = false))
        assertEquals(MovieWatch.Elsewhere("HBO Max"), movieWatch(offers, listOf(netflix, fandango)))
        assertEquals(MovieWatch.Nowhere, movieWatch(emptyList(), listOf(netflix)))
    }

    @Test
    fun cardNoteShowsDateScoreAndWhere() {
        val info = MovieInfo(
            MovieFacts("2026", LocalDate.of(2026, 10, 27), 112, listOf("Thriller"), "R", "tt1", emptyList()),
            MovieScores(rottenTomatoes = 81, imdb = "6.9", metacritic = null),
        )
        assertEquals("Oct 27 · 🍅 81% · Netflix", movieNote(movie(LocalDate.of(2026, 10, 27)), info, MovieWatch.Stream(netflix), today))
        assertEquals("2019 · Rent", movieNote(movie(LocalDate.of(2019, 3, 1)), null, MovieWatch.Rent(fandango), today))
        assertEquals("", movieNote(movie(null), null, null, today))
    }

    @Test
    fun heroLinesReadNaturally() {
        val facts = MovieFacts("2025", null, 112, listOf("Drama", "Thriller", "Mystery", "Crime"), "PG-13", null, emptyList())
        assertEquals("2025 · 1h 52m · PG-13 · Drama, Thriller, Mystery", movieMeta(MovieInfo(facts, null), movie(null)))
        assertEquals("2026", movieMeta(null, movie(null)))
        assertEquals("🍅 92%  ·  IMDb 7.8  ·  Metacritic 71", MovieScores(92, "7.8", 71).line)
        assertEquals("IMDb 7.8", MovieScores(null, "7.8", null).line)
        assertEquals("Rent or buy on Fandango at Home", MovieWatch.Rent(fandango).sentence())
    }
}
