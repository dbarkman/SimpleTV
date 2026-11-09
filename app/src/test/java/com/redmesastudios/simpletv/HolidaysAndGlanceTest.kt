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
