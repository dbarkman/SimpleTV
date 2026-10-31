package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

class InfoPanelTest {
    private val now = LocalDateTime.parse("2026-10-31T20:15")
    private val nowInstant = Instant.parse("2026-11-01T03:15:00Z")
    private val movie = Title(115, TitleKind.Movie, "Black Mass", overview = "Whitey Bulger's story.", year = "2015", rating = 6.8)
    private val weather = WeatherNow("Springfield", 65, "F", "Mostly Clear")

    @Test
    fun aShowStartedFromSimpleTVGetsItsDetails() {
        val started = NowPlaying.Started("com.netflix.ninja", movie, nowInstant.minusSeconds(3600))
        val info = panelInfo("com.netflix.ninja", "Netflix", started, weather, now, nowInstant)
        assertEquals("Black Mass", info.heading)
        assertEquals("2015 · Movie · ★ 6.8", info.meta)
        assertEquals("Whitey Bulger's story.", info.overview)
        assertEquals("8:15 PM", info.time)
        assertEquals("Saturday, October 31", info.date)
        assertEquals("65° · Mostly Clear", info.weather)
    }

    @Test
    fun anythingElseGetsTheAppName() {
        val elsewhere = NowPlaying.Started("com.hulu.livingroomplus", movie, nowInstant.minusSeconds(60))
        assertEquals("Watching Netflix", panelInfo("com.netflix.ninja", "Netflix", elsewhere, weather, now, nowInstant).heading)
        val longAgo = NowPlaying.Started("com.netflix.ninja", movie, nowInstant.minusSeconds(5 * 3600))
        val info = panelInfo("com.netflix.ninja", "Netflix", longAgo, null, now, nowInstant)
        assertEquals("Watching Netflix", info.heading)
        assertEquals("", info.meta)
        assertEquals("", info.weather)
    }

    @Test
    fun showsCountTheirSeasons() {
        assertEquals("2019 · 4 seasons", titleMeta(Title(1, TitleKind.Show, "x", year = "2019", seasons = 4)))
        assertEquals("1 season · ★ 7.5", titleMeta(Title(1, TitleKind.Show, "x", seasons = 1, rating = 7.5)))
    }
}
