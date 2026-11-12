package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HolidaysAndGlanceTest {
    private val OldScenes = setOf(Holiday.Halloween, Holiday.ElectionDay)
    private val zone = ZoneId.of("America/Phoenix")

    @Test
    fun halloweenRunsFromTheTwentyFourthThroughNovemberFirst() {
        assertNull(holidayOn(drawn = OldScenes, date = LocalDate.of(2026, 10, 23)))
        assertEquals(Holiday.Halloween, holidayOn(drawn = OldScenes, date = LocalDate.of(2026, 10, 24)))
        assertEquals(Holiday.Halloween, holidayOn(drawn = OldScenes, date = LocalDate.of(2026, 10, 31)))
        assertEquals(Holiday.Halloween, holidayOn(drawn = OldScenes, date = LocalDate.of(2026, 11, 1)))
        assertEquals(Holiday.Halloween, holidayOn(drawn = OldScenes, date = LocalDate.of(2027, 10, 30)))
    }

    @Test
    fun electionDayRunsFromNovemberSecondThroughTheTuesdayAfterTheFirstMonday() {
        assertEquals(LocalDate.of(2026, 11, 3), electionDay(2026))
        assertEquals(LocalDate.of(2027, 11, 2), electionDay(2027))
        assertEquals(LocalDate.of(2028, 11, 7), electionDay(2028))
        // November 1st is a Tuesday: the first Monday is the 7th, so it's the 8th.
        assertEquals(LocalDate.of(2033, 11, 8), electionDay(2033))
        assertEquals(Holiday.ElectionDay, holidayOn(drawn = OldScenes, date = LocalDate.of(2026, 11, 2)))
        assertEquals(Holiday.ElectionDay, holidayOn(drawn = OldScenes, date = LocalDate.of(2026, 11, 3)))
        assertNull(holidayOn(drawn = OldScenes, date = LocalDate.of(2026, 11, 4)))
        assertEquals(Holiday.ElectionDay, holidayOn(drawn = OldScenes, date = LocalDate.of(2028, 11, 7)))
        assertNull(holidayOn(drawn = OldScenes, date = LocalDate.of(2028, 11, 8)))
    }

    @Test
    fun uncleSamMarchesInStopsToWaveThenMarchesOut() {
        val sam = Marcher(cycleSeconds = 60f, walkSeconds = 10f, pauseSeconds = 4f, stopX = 800f, offsetSeconds = 0f, baseY = 800f, strideSeconds = 1f)
        val start = sam.poseAt(0f)!!
        assertTrue(start.x < 0f)
        assertEquals(0f, start.raise)
        val stopped = sam.poseAt(12f)!!
        assertEquals(800f, stopped.x)
        assertEquals(1f, stopped.raise)
        assertEquals(0f, stopped.stride)
        // The arm eases up and back down rather than snapping.
        assertTrue(sam.poseAt(10.2f)!!.raise in 0.1f..0.9f)
        assertTrue(sam.poseAt(13.8f)!!.raise in 0.1f..0.9f)
        val leaving = sam.poseAt(19f)!!
        assertTrue(leaving.x > 800f)
        assertEquals(0f, leaving.raise)
        assertTrue(sam.poseAt(23.99f)!!.x > SceneWidth)
        assertNull(sam.poseAt(30f))
    }

    @Test
    fun batsCrossTheScreenThenRest() {
        val bat = Bat(cycleSeconds = 40f, flightSeconds = 10f, offsetSeconds = 0f, y = 100f, size = 50f, leftToRight = true)
        val start = assertNotNull(bat.poseAt(0f)).let { bat.poseAt(0f)!! }
        val end = bat.poseAt(9.99f)!!
        assertTrue(start.x < 0f && end.x > SceneWidth)
        assertNull(bat.poseAt(20f))
        assertTrue(bat.poseAt(41f) != null)
        val back = bat.copy(leftToRight = false).poseAt(0f)!!
        assertTrue(back.x > SceneWidth)
    }

    @Test
    fun newYearMlkAndGameDayShowOnTheirDays() {
        fun on(text: String) = holidayOn(LocalDate.parse(text), DefaultPacks, DrawnHolidays, zone)
        assertEquals(Holiday.Christmas, on("2026-12-30"))
        assertEquals(Holiday.NewYear, on("2026-12-31"))
        assertEquals(Holiday.NewYear, on("2027-01-01"))
        assertNull(on("2027-01-02"))
        assertEquals(Holiday.MlkDay, on("2027-01-18"))
        assertEquals(Holiday.MlkDay, on("2028-01-17"))
        assertEquals(Holiday.SuperBowl, on("2027-02-14"))
        assertEquals("Happy New Year!", holidayGreeting(on("2026-12-31")))
        assertEquals("Honoring Dr. King", holidayGreeting(on("2027-01-18")))
        assertEquals("Happy game day!", holidayGreeting(on("2027-02-14")))
        // New Year's Eve already shows the coming year.
        assertEquals(2027, newYearShown(LocalDate.parse("2026-12-31")))
        assertEquals(2027, newYearShown(LocalDate.parse("2027-01-01")))
    }

    @Test
    fun fireworkRisesBurstsAndClears() {
        val firework = Firework(x = 300f, peakY = 700f, radius = 60f, cycleSeconds = 8f, offsetSeconds = 0f, colorIndex = 0)
        val launch = firework.poseAt(0f)!!
        assertTrue(launch.rising)
        assertEquals(880f, launch.y)
        val climbing = firework.poseAt(0.65f)!!
        assertTrue(climbing.rising && climbing.y < 880f && climbing.y > 700f)
        val opening = firework.poseAt(1.5f)!!
        assertTrue(!opening.rising && opening.spread in 0.01f..0.5f && opening.fade > 0.9f)
        val fading = firework.poseAt(3.6f)!!
        assertTrue(fading.spread > 0.95f && fading.fade < 0.3f && fading.y > 700f)
        assertNull(firework.poseAt(5f))
        assertTrue(firework.poseAt(8.1f)!!.rising)
    }

    @Test
    fun fieldGoalClearsTheUprightsAndCountsItsKicks() {
        val kick = Kick(cycleSeconds = 10f, flightSeconds = 3f, gapSeconds = 1f, offsetSeconds = 0f, fromX = 470f, toX = 1820f, groundY = 850f, apexHeight = 206f)
        val start = kick.poseAt(0f)
        assertTrue(start.flying)
        assertEquals(470f, start.x)
        assertEquals(850f, start.y)
        assertEquals(0, start.kicks)
        // Over the posts at x 1560: above the crossbar (792), below the upright tops (652).
        val atPosts = kick.poseAt(3f * (1560f - 470f) / 1350f)
        assertTrue(atPosts.y in 652f..792f)
        assertEquals(1, kick.poseAt(2.9f).kicks)
        // Gone for a second, then back on the tee, fading in.
        assertEquals(0f, kick.poseAt(3.5f).teeAlpha)
        assertTrue(kick.poseAt(4.3f).teeAlpha in 0.1f..0.9f)
        assertEquals(1f, kick.poseAt(9f).teeAlpha)
        assertEquals(2, kick.poseAt(12.9f).kicks)
        assertEquals(14, homeScore(0))
        assertEquals(17, homeScore(1))
        assertEquals(14, homeScore(8))
    }

    @Test
    fun candleNeverGoesDark() {
        (0 until 500).forEach { assertTrue(candleGlow(it * 0.07f, 1f) in 0.6f..1f) }
    }

    @Test
    fun newsCountsTheLastSixHoursOrOvernight() {
        fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant()
        val seen = listOf(at("2026-10-30T23:00"), at("2026-10-31T03:00"), at("2026-10-31T13:00"), at("2026-10-31T14:30"))
        assertEquals("2 new stories in the last 6 hours", newsLine(seen, LocalDateTime.parse("2026-10-31T16:00"), zone))
        assertEquals("1 new story in the last 6 hours", newsLine(seen.take(3), LocalDateTime.parse("2026-10-31T16:00"), zone))
        val morning = listOf(at("2026-10-30T21:00"), at("2026-10-30T23:00"), at("2026-10-31T03:00"))
        assertEquals("2 new stories overnight", newsLine(morning, LocalDateTime.parse("2026-10-31T07:00"), zone))
        assertEquals("No new stories in the last 6 hours", newsLine(emptyList<Instant>(), LocalDateTime.parse("2026-10-31T16:00"), zone))
        assertEquals("Nothing new overnight", newsLine(emptyList<Instant>(), LocalDateTime.parse("2026-10-31T06:00"), zone))
    }

    @Test
    fun percentsUseOneDecimalAndATrueMinus() {
        assertEquals("+0.4%", percentText(0.43))
        assertEquals("−1.8%", percentText(-1.84))
        assertEquals("+0.0%", percentText(-0.01))
    }
}
