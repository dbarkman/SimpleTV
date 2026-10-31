package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZoneId

class ChannelDataTest {

    @Test
    fun skiesReadFromTheForecastWords() {
        assertEquals(Sky.Storm, skyFor("Chance Showers And Thunderstorms"))
        assertEquals(Sky.Rain, skyFor("Slight Chance Rain Showers then Partly Sunny"))
        assertEquals(Sky.PartlyCloudy, skyFor("Partly Sunny"))
        assertEquals(Sky.PartlyCloudy, skyFor("Mostly Sunny"))
        assertEquals(Sky.Sun, skyFor("Sunny"))
        assertEquals(Sky.Sun, skyFor("Clear"))
        assertEquals(Sky.Cloud, skyFor("Cloudy"))
    }

    @Test
    fun periodNamesReadMidSentence() {
        assertEquals("tonight", whenSaid("Tonight"))
        assertEquals("this afternoon", whenSaid("This Afternoon"))
        assertEquals("today", whenSaid("Today"))
        assertEquals("on Saturday", whenSaid("Saturday"))
    }

    @Test
    fun daysPairEachDaytimeHighWithTheNightAfter() {
        val periods = listOf(
            NwsPeriod("2026-10-30T22:00:00-07:00", false, 62, "Showers", 39),
            NwsPeriod("2026-10-31T06:00:00-07:00", true, 78, "Partly Sunny", 13),
            NwsPeriod("2026-10-31T18:00:00-07:00", false, 64, "Mostly Clear", 5),
            NwsPeriod("2026-11-01T06:00:00-07:00", true, 72, "Thunderstorms", 0),
        )
        val days = foldDays(periods, ZoneId.of("America/Phoenix"))
        assertEquals(listOf("Saturday", "Sunday"), days.map { it.name })
        assertEquals(listOf(78, 72), days.map { it.high })
        assertEquals(listOf(64, null), days.map { it.low })
        assertEquals(periods[2], days[0].night)
        assertEquals(LocalDate.of(2026, 11, 1), days[1].date)
    }

    @Test
    fun eachDayGetsItsOwnHours() {
        val periods = listOf(
            NwsPeriod("2026-10-31T06:00:00-07:00", true, 78, "Partly Sunny", 13),
            NwsPeriod("2026-11-01T06:00:00-07:00", true, 72, "Thunderstorms", 0),
        )
        val hours = listOf(
            HourForecast(LocalDateTime.of(2026, 10, 31, 9, 0), 70, 0),
            HourForecast(LocalDateTime.of(2026, 11, 1, 9, 0), 66, 40),
        )
        val days = foldDays(periods, ZoneId.of("America/Phoenix"), hours)
        assertEquals(listOf(70), days[0].hours.map { it.temperature })
        assertEquals(listOf(66), days[1].hours.map { it.temperature })
    }

    @Test
    fun theDayStripShowsEveryThirdHourFromSixToNine() {
        val hours = (0..23).map { HourForecast(LocalDateTime.of(2026, 11, 1, it, 0), 60 + it, 0) }
        assertEquals(listOf(6, 9, 12, 15, 18, 21), daytimeHours(hours).map { it.time.hour })
        assertEquals(listOf("6 AM", "12 PM", "9 PM"), listOf(6, 12, 21).map { hourLabel(LocalDateTime.of(2026, 11, 1, it, 0)) })
    }

    @Test
    fun storyAgesStayShort() {
        val now = Instant.parse("2026-10-31T12:00:00Z")
        assertEquals("1 min ago", storyAge(now.minusSeconds(20), now))
        assertEquals("25 min ago", storyAge(now.minusSeconds(25 * 60), now))
        assertEquals("1 hour ago", storyAge(now.minusSeconds(90 * 60), now))
        assertEquals("32 hours ago", storyAge(now.minusSeconds(32 * 3600), now))
        assertEquals("3 days ago", storyAge(now.minusSeconds(3 * 86400), now))
    }

    @Test
    fun indexMovesComeFromThePreviousClose() {
        val day = IndexDay("S&P 500", symbol = "%5EGSPC", price = 7580.07, previousClose = 7720.39, prices = emptyList(), asOf = Instant.EPOCH, open = false)
        assertEquals(-140.32, day.change, 0.001)
        assertEquals("−1.8%", percentText(day.percent))
    }

    @Test
    fun symbolsAreCleanedUpAsTyped() {
        assertEquals(listOf("AAPL", "MSFT", "TSLA"), parseSymbols("aapl, msft  tsla"))
        assertEquals(listOf("BRK.B", "^GSPC"), parseSymbols("brk.b ^gspc brk.b"))
        assertEquals(emptyList<String>(), parseSymbols("  "))
        assertEquals(listOf("SPY"), parseSymbols("spy !!!"))
    }

    @Test
    fun performanceLooksBackFromTheLastCloseBeforeEachDate() {
        val zone = ZoneId.of("America/New_York")
        // Daily closes from Nov 1, 2025 through Oct 30, 2026, rising by 1 a day from 100.
        val start = java.time.LocalDate.of(2025, 11, 1)
        val days = (0..363).map { start.plusDays(it.toLong()) }
        val times = days.map { it.atTime(16, 0).atZone(zone).toInstant() }
        val closes = days.indices.map { 100.0 + it }
        val now = java.time.LocalDate.of(2026, 10, 30).atTime(18, 0).atZone(zone).toInstant()
        val price = closes.last()
        val result = performance(times, closes, price, now, zone).toMap()
        fun closeOn(date: java.time.LocalDate) = closes[days.indexOf(date)]
        assertEquals((price - closeOn(java.time.LocalDate.of(2026, 10, 23))) / closeOn(java.time.LocalDate.of(2026, 10, 23)) * 100, result.getValue("1 week")!!, 1e-9)
        assertEquals((price - closeOn(java.time.LocalDate.of(2025, 12, 31))) / closeOn(java.time.LocalDate.of(2025, 12, 31)) * 100, result.getValue("Year to date")!!, 1e-9)
        // A year back is before the history starts, by a day: close enough, it uses the first close.
        assertEquals((price - 100.0) / 100.0 * 100, result.getValue("1 year")!!, 1e-9)
    }

    @Test
    fun shortHistoryLeavesLongLooksBlank() {
        val zone = ZoneId.of("America/New_York")
        val times = listOf(Instant.parse("2026-10-20T20:00:00Z"), Instant.parse("2026-10-29T20:00:00Z"))
        val result = performance(times, listOf(10.0, 11.0), 12.0, Instant.parse("2026-10-30T22:00:00Z"), zone).toMap()
        assertEquals(20.0, result.getValue("1 week")!!, 1e-9)
        assertEquals(null, result.getValue("1 month"))
        assertEquals(null, result.getValue("1 year"))
    }

    @Test
    fun bigNumbersReadInWords() {
        assertEquals("67.8 million", bigNumber(67_842_804))
        assertEquals("4.27 billion", bigNumber(4_268_617_000))
        assertEquals("812,300", bigNumber(812_300))
    }
}
