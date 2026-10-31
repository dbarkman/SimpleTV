package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class YouTubeTest {
    private val zone = ZoneId.of("America/Phoenix")
    private val now = Instant.parse("2026-10-30T22:00:00Z") // 3 pm in Phoenix

    @Test
    fun agesReadNaturally() {
        assertEquals("Just now", videoAge(Instant.parse("2026-10-30T21:30:00Z"), now, zone))
        assertEquals("1 hour ago", videoAge(Instant.parse("2026-10-30T20:30:00Z"), now, zone))
        assertEquals("5 hours ago", videoAge(Instant.parse("2026-10-30T17:00:00Z"), now, zone))
        // 11 pm the night before is yesterday, even though it's under 24 hours.
        assertEquals("Yesterday", videoAge(Instant.parse("2026-10-30T06:00:00Z"), now, zone))
        assertEquals("3 days ago", videoAge(Instant.parse("2026-10-27T18:00:00Z"), now, zone))
        assertEquals("Oct 12", videoAge(Instant.parse("2026-10-12T18:00:00Z"), now, zone))
    }

    @Test
    fun newestComeFirstWithoutRepeats() {
        fun video(id: String) = Title(0, TitleKind.Video, id, videoId = id)
        val published = mapOf("a" to Instant.parse("2026-10-28T00:00:00Z"), "b" to Instant.parse("2026-10-30T00:00:00Z"), "c" to Instant.parse("2026-10-29T00:00:00Z"))
        val newest = newestVideos(listOf(video("a"), video("b"), video("c"), video("b")), published, 2)
        assertEquals(listOf("b", "c"), newest.map { it.videoId })
    }

    @Test
    fun topChannelsGiveTheirNewestFullLengthVideoInChannelOrder() {
        fun video(id: String) = Title(0, TitleKind.Video, id, videoId = id)
        val day = { d: Int -> Instant.parse("2026-10-%02dT00:00:00Z".format(d)) }
        val channels = listOf(
            listOf(video("a-old") to day(20), video("a-short") to day(29), video("a-new") to day(28)),
            listOf(video("b-short") to day(30)),
            listOf(video("c") to day(10)),
        )
        val picks = topChannelPicks(channels, fullLength = setOf("a-old", "a-new", "c"), count = 5)
        assertEquals(listOf("a-new", "c"), picks.map { it.videoId })
        assertEquals(listOf("a-new"), topChannelPicks(channels, setOf("a-old", "a-new", "c"), count = 1).map { it.videoId })
    }

    @Test
    fun videosAreKeyedAndPicturedByVideo() {
        val video = Title(0, TitleKind.Video, "A video", videoId = "abc123")
        assertEquals("abc123", video.key)
        assertEquals("https://i.ytimg.com/vi/abc123/mqdefault.jpg", video.imageUrl(large = false))
        assertEquals("https://i.ytimg.com/vi/abc123/hqdefault.jpg", video.imageUrl(large = true))
        val show = Title(1399, TitleKind.Show, "A show", backdropPath = "/x.jpg")
        assertEquals("Show:1399", show.key)
        assertEquals("https://image.tmdb.org/t/p/w300/x.jpg", show.imageUrl(large = false))
    }
}
