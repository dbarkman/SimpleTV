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
    private val zone = ZoneId.of("America/Phoenix")

    @Test
    fun halloweenRunsFromTheTwentyFourthThroughNovemberFirst() {
        assertNull(holidayOn(LocalDate.of(2026, 10, 23)))
        assertEquals(Holiday.Halloween, holidayOn(LocalDate.of(2026, 10, 24)))
        assertEquals(Holiday.Halloween, holidayOn(LocalDate.of(2026, 10, 31)))
        assertEquals(Holiday.Halloween, holidayOn(LocalDate.of(2026, 11, 1)))
        assertNull(holidayOn(LocalDate.of(2026, 11, 2)))
        assertEquals(Holiday.Halloween, holidayOn(LocalDate.of(2027, 10, 30)))
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
