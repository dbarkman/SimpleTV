package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CatalogTest {

    private fun release(id: Int, kind: TitleKind, season: Int?, day: Int) =
        Release(sourceId = 203, tmdbId = id, kind = kind, season = season, date = LocalDate.of(2026, 10, day))

    private val releases = listOf(
        release(1, TitleKind.Movie, null, 3),
        release(2, TitleKind.Show, 1, 20),
        release(3, TitleKind.Show, 4, 12),
        release(4, TitleKind.Show, 2, 25),
        release(2, TitleKind.Show, 1, 21),
    )

    @Test
    fun justAddedHoldsMoviesAndNewShowsNewestFirst() {
        assertEquals(listOf(2, 1), justAdded(releases).map { it.tmdbId })
    }

    @Test
    fun newSeasonsHoldReturningShowsOnly() {
        assertEquals(listOf(4, 3), newSeasons(releases, upcoming = false).map { it.tmdbId })
        assertEquals(listOf(3, 4), newSeasons(releases, upcoming = true).map { it.tmdbId })
    }

    @Test
    fun notesSayWhatIsNew() {
        assertEquals("Added Oct 3", releaseNote(releases[0], upcoming = false))
        assertEquals("Season 4", releaseNote(releases[2], upcoming = false))
        assertEquals("Season 4 · Oct 12", releaseNote(releases[2], upcoming = true))
    }

    @Test
    fun netflixNamesLoseTheirSeasonSuffix() {
        assertEquals("Nobody Wants This", cleanNetflixName("Nobody Wants This: Season 3"))
        assertEquals("Love Is Blind", cleanNetflixName("Love Is Blind: S11: Boston"))
        assertEquals("TYSON", cleanNetflixName("TYSON: Limited Series"))
        assertEquals("Raw", cleanNetflixName("Raw: 2026 - October 19, 2026"))
        assertEquals("Theo Von: I'm Doing Fine", cleanNetflixName("Theo Von: I&#x27;m Doing Fine"))
        assertEquals("Cyberpunk: Edgerunners 2", cleanNetflixName("Cyberpunk: Edgerunners 2"))
    }

    // Shaped like Netflix's Top 10 page: image alt (name), play link (ID), then rank.
    @Test
    fun netflixTop10PairsNamesIdsAndRanks() {
        val html = """
            <h2>Top 10 Shows</h2>
            <img src="a.jpg" alt="From: Season 1"/><a href="https://www.netflix.com/watch/81446288?trackId=1">Play</a><div>#1 in Shows</div>
            <img src="b.jpg" alt="Nobody Wants This: Season 3"/><a href="https://www.netflix.com/watch/81591296?trackId=1">Play</a><div>#2 in Shows</div>
            <img src="c.jpg" alt="Below: Limited Series"/><a href="https://www.netflix.com/watch/81652677?trackId=1">Play</a><div>#3 in Shows</div>
            <table><tr><td><span class="rank">01</span><button>From: Season 1</button></td></tr></table>
            <img src="d.jpg" alt="A man on a pier looking forward"/>
        """.trimIndent()
        assertEquals(
            listOf(
                NetflixRank(1, "From", "81446288"),
                NetflixRank(2, "Nobody Wants This", "81591296"),
                NetflixRank(3, "Below", "81652677"),
            ),
            parseNetflixTop10(html),
        )
    }
}
