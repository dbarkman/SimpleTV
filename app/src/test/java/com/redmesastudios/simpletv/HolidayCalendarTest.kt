package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId

class HolidayCalendarTest {
    private val phoenix = ZoneId.of("America/Phoenix")
    private fun on(text: String, packs: Set<Pack> = Pack.entries.toSet(), drawn: Set<Holiday> = Holiday.entries.toSet()) =
        holidayOn(LocalDate.parse(text), packs, drawn, phoenix)

    // Reference dates from the macOS Hebrew, Chinese and Umm al-Qura calendars.
    private val roshHashanahs = listOf(
        "2026-09-12", "2027-10-02", "2028-09-21", "2029-09-10", "2030-09-28", "2031-09-18",
        "2032-09-06", "2033-09-24", "2034-09-14", "2035-10-04", "2036-09-22", "2037-09-10",
        "2038-09-30", "2039-09-19", "2040-09-08", "2041-09-26", "2042-09-15", "2043-10-05",
        "2044-09-22", "2045-09-12", "2046-10-01", "2047-09-21", "2048-09-08", "2049-09-27",
        "2050-09-17", "2051-09-07", "2052-09-24", "2053-09-13", "2054-10-03", "2055-09-23",
        "2056-09-11", "2057-09-29", "2058-09-19", "2059-09-08", "2060-09-25",
    )
    private val hanukkahs = listOf(
        "2026-12-05", "2027-12-25", "2028-12-13", "2029-12-02", "2030-12-21", "2031-12-10",
        "2032-11-28", "2033-12-17", "2034-12-07", "2035-12-26", "2036-12-14", "2037-12-03",
        "2038-12-22", "2039-12-12", "2040-11-30", "2041-12-18", "2042-12-08", "2043-12-27",
        "2044-12-15", "2045-12-04", "2046-12-24", "2047-12-13", "2048-11-30", "2049-12-20",
        "2050-12-10", "2051-11-29", "2052-12-16", "2053-12-06", "2054-12-26", "2055-12-15",
        "2056-12-03", "2057-12-22", "2058-12-11", "2059-11-30", "2060-12-18",
    )
    private val passovers = listOf(
        "2026-04-02", "2027-04-22", "2028-04-11", "2029-03-31", "2030-04-18", "2031-04-08",
        "2032-03-27", "2033-04-14", "2034-04-04", "2035-04-24", "2036-04-12", "2037-03-31",
        "2038-04-20", "2039-04-09", "2040-03-29", "2041-04-16", "2042-04-05", "2043-04-25",
        "2044-04-12", "2045-04-02", "2046-04-21", "2047-04-11", "2048-03-29", "2049-04-17",
        "2050-04-07", "2051-03-28", "2052-04-14", "2053-04-03", "2054-04-23", "2055-04-13",
        "2056-04-01", "2057-04-19", "2058-04-09", "2059-03-29", "2060-04-15",
    )
    private val ramadans = listOf(
        "2026-02-18", "2027-02-08", "2028-01-28", "2029-01-16", "2030-01-05", "2030-12-26",
        "2031-12-16", "2032-12-04", "2033-11-23", "2034-11-12", "2035-11-01", "2036-10-21",
        "2037-10-10", "2038-09-30", "2039-09-19", "2040-09-08",
    )
    private val eidAlFitrs = listOf(
        "2026-03-20", "2027-03-09", "2028-02-26", "2029-02-14", "2030-02-04", "2031-01-24",
        "2032-01-14", "2033-01-03", "2033-12-23", "2034-12-12", "2035-12-01", "2036-11-19",
        "2037-11-09", "2038-10-29", "2039-10-19", "2040-10-07",
    )
    private val eidAlAdhas = listOf(
        "2026-05-27", "2027-05-16", "2028-05-05", "2029-04-24", "2030-04-13", "2031-04-02",
        "2032-03-22", "2033-03-12", "2034-03-01", "2035-02-19", "2036-02-08", "2037-01-27",
        "2038-01-16", "2039-01-05", "2039-12-26", "2040-12-15",
    )
    private val lunarNewYears = listOf(
        "2026-02-17", "2027-02-06", "2028-01-26", "2029-02-13", "2030-02-03", "2031-01-23",
        "2032-02-11", "2033-01-31", "2034-02-19", "2035-02-08", "2036-01-28", "2037-02-15",
        "2038-02-04", "2039-01-24", "2040-02-12", "2041-02-01", "2042-01-22",
    )

    @Test
    fun hebrewDatesMatchTheHebrewCalendar() {
        roshHashanahs.map(LocalDate::parse).forEach { assertEquals(it, roshHashanah(it.year)) }
        hanukkahs.map(LocalDate::parse).forEach { assertEquals(it, hanukkah(it.year)) }
        passovers.map(LocalDate::parse).forEach { assertEquals(it, passover(it.year)) }
        // Purim 2026 was March 3rd; Yom Kippur 2026 is September 21st.
        assertEquals(Holiday.Purim, on("2026-03-03", setOf(Pack.Jewish)))
        assertEquals(Holiday.YomKippur, on("2026-09-21", setOf(Pack.Jewish)))
        assertEquals(Holiday.Hanukkah, on("2026-12-12", setOf(Pack.Jewish)))
        assertNull(on("2026-12-13", setOf(Pack.Jewish)))
    }

    @Test
    fun islamicDatesMatchUmmAlQura() {
        fun starts(holiday: Holiday) = (2026..2040).flatMap { holiday.stretchesIn(it, phoenix) }.map { it.start.toString() }
        assertEquals(ramadans, starts(Holiday.Ramadan))
        assertEquals(eidAlFitrs, starts(Holiday.EidAlFitr))
        assertEquals(eidAlAdhas, starts(Holiday.EidAlAdha))
        // Ramadan runs up to the day before Eid al-Fitr.
        Holiday.Ramadan.stretchesIn(2026, phoenix).single().let { assertEquals(LocalDate.parse("2026-03-19"), it.endInclusive) }
    }

    @Test
    fun lunarNewYearMatchesTheChineseCalendar() {
        assertEquals(lunarNewYears, (2026..2042).map { Holiday.LunarNewYear.stretchesIn(it, phoenix).single().start.toString() })
    }

    @Test
    fun easterAndItsDays() {
        assertEquals(LocalDate.of(2026, 4, 5), easter(2026))
        assertEquals(LocalDate.of(2027, 3, 28), easter(2027))
        assertEquals(LocalDate.of(2028, 4, 16), easter(2028))
        assertEquals(LocalDate.of(2038, 4, 25), easter(2038))
        assertEquals(Holiday.AshWednesday, on("2026-02-18", setOf(Pack.Christian)))
        assertEquals(Holiday.Easter, on("2026-03-29", setOf(Pack.Christian)))
        assertEquals(Holiday.GoodFriday, on("2026-04-03", setOf(Pack.Christian)))
        assertEquals(Holiday.Easter, on("2026-04-05", setOf(Pack.Christian)))
        assertEquals(Holiday.Pentecost, on("2026-05-24", setOf(Pack.Christian)))
    }

    @Test
    fun seasonsStartAtTheEquinoxesAndSolstices() {
        assertEquals(LocalDate.of(2026, 3, 20), seasonStart(2026, Season.Spring, phoenix))
        assertEquals(LocalDate.of(2026, 6, 21), seasonStart(2026, Season.Summer, phoenix))
        // 00:05 UTC on the 23rd is still the 22nd in Arizona.
        assertEquals(LocalDate.of(2026, 9, 22), seasonStart(2026, Season.Fall, phoenix))
        assertEquals(LocalDate.of(2026, 9, 23), seasonStart(2026, Season.Fall, ZoneId.of("UTC")))
        assertEquals(LocalDate.of(2026, 12, 21), seasonStart(2026, Season.Winter, phoenix))
        assertEquals(Season.Winter, seasonOn(LocalDate.of(2026, 1, 15), phoenix))
        assertEquals(Season.Spring, seasonOn(LocalDate.of(2026, 3, 20), phoenix))
        assertEquals(Season.Summer, seasonOn(LocalDate.of(2026, 7, 4), phoenix))
        assertEquals(Season.Fall, seasonOn(LocalDate.of(2026, 11, 9), phoenix))
        assertEquals(Season.Winter, seasonOn(LocalDate.of(2026, 12, 21), phoenix))
    }

    @Test
    fun usHolidaysFallWhereTheyShould() {
        val us = setOf(Pack.UsNational)
        assertEquals(Holiday.NewYear, on("2026-12-31", us))
        assertEquals(Holiday.NewYear, on("2027-01-01", us))
        assertNull(on("2027-01-02", us))
        assertEquals(Holiday.MlkDay, on("2027-01-18", us))
        assertEquals(Holiday.SuperBowl, on("2027-02-14", us))
        assertEquals(Holiday.PresidentsDay, on("2027-02-15", us))
        assertEquals(Holiday.MothersDay, on("2027-05-07", us))
        assertEquals(Holiday.MothersDay, on("2027-05-09", us))
        assertEquals(Holiday.MemorialDay, on("2027-05-29", us))
        assertEquals(Holiday.MemorialDay, on("2027-05-31", us))
        // Juneteenth, a single day, wins over the Father's Day weekend around it.
        assertEquals(Holiday.Juneteenth, on("2027-06-19", us))
        assertEquals(Holiday.FathersDay, on("2027-06-20", us))
        assertEquals(Holiday.FourthOfJuly, on("2027-07-04", us))
        assertEquals(Holiday.LaborDay, on("2027-09-06", us))
        assertEquals(Holiday.GrandparentsDay, on("2027-09-12", us))
        assertEquals(Holiday.GrandparentsDay, on("2027-09-10", us))
        assertNull(on("2027-09-13", us))
    }

    @Test
    fun thanksgivingThenChristmas() {
        val us = setOf(Pack.UsNational)
        assertEquals(LocalDate.of(2026, 11, 26), thanksgiving(2026))
        assertEquals(Holiday.ElectionDay, on("2026-11-03", us))
        assertEquals(Holiday.Thanksgiving, on("2026-11-04", us))
        assertEquals(Holiday.Thanksgiving, on("2026-11-09", us))
        assertEquals(Holiday.VeteransDay, on("2026-11-11", us))
        assertEquals(Holiday.Thanksgiving, on("2026-11-26", us))
        assertNull(on("2026-11-27", us))
        assertEquals(Holiday.Christmas, on("2026-11-28", us))
        assertEquals(Holiday.Christmas, on("2026-12-30", us))
        assertEquals(Holiday.NewYear, on("2026-12-31", us))
    }

    @Test
    fun aOneDayHolidayWinsItsDayInsideALongerOne() {
        assertEquals(Holiday.CatDay, on("2026-10-29"))
        assertEquals(Holiday.Halloween, on("2026-10-30"))
        assertEquals(Holiday.PiDay, on("2027-03-14"))
        assertEquals(Holiday.StPatricks, on("2027-03-15"))
        assertEquals(Holiday.BitcoinPizzaDay, on("2027-05-22"))
        assertEquals(Holiday.DonutDay, on("2027-06-04"))
        assertEquals(Holiday.IceCreamDay, on("2027-07-18"))
    }

    @Test
    fun aHolidayWithoutASceneLeavesTheDayToTheOthers() {
        val drawn = setOf(Holiday.Halloween, Holiday.ElectionDay)
        assertEquals(Holiday.Halloween, on("2026-10-29", drawn = drawn))
        assertEquals(Holiday.ElectionDay, on("2026-11-02", drawn = drawn))
    }

    @Test
    fun theFirstDayOfASeasonShowsOnlyWhenNothingElseDoes() {
        assertEquals(Holiday.FirstDayOfSpring, on("2026-03-20", DefaultPacks))
        assertEquals(Holiday.FirstDayOfSummer, on("2027-06-21", DefaultPacks))
        // The winter solstice falls inside Christmas, which keeps the day.
        assertEquals(Holiday.Christmas, on("2026-12-21", DefaultPacks))
        assertEquals(Holiday.FirstDayOfWinter, on("2026-12-21", setOf(Pack.Seasons)))
    }

    @Test
    fun packsComeFromHomeTxt() {
        assertEquals(DefaultPacks, parsePacks(""))
        assertEquals(setOf(Pack.UsNational, Pack.FunDays), parsePacks(" US, fun ,nonsense"))
        assertNull(on("2026-12-05", setOf(Pack.FunDays)))
        assertEquals(Holiday.Hanukkah, on("2026-12-05", setOf(Pack.Jewish, Pack.FunDays)))
    }

    @Test
    fun familyDaysAndTheirGreeting() {
        val days = parseFamilyDays(
            """
            # comment
            01-05 birthday Alex
            01-05 birthday Sam
            07-15 anniversary Pat & Jo
            02-29 birthday Leap
            nonsense line
            13-40 birthday Nobody
            """.trimIndent(),
        )
        assertEquals(4, days.size)
        assertEquals("Happy birthday, Alex and Sam!", celebrationGreeting(familyDaysOn(LocalDate.of(2027, 1, 5), days)))
        assertEquals("Happy anniversary, Pat & Jo!", celebrationGreeting(familyDaysOn(LocalDate.of(2027, 7, 15), days)))
        assertNull(celebrationGreeting(familyDaysOn(LocalDate.of(2027, 7, 16), days)))
        assertEquals(MonthDay.of(2, 29), familyDaysOn(LocalDate.of(2027, 2, 28), days).single().day)
        assertTrue(familyDaysOn(LocalDate.of(2028, 2, 28), days).isEmpty())
        assertEquals(1, familyDaysOn(LocalDate.of(2028, 2, 29), days).size)
    }

    @Test
    fun balloonsRiseFromBelowThePageToAboveIt() {
        val balloon = RisingBalloon(x = 500f, riseSeconds = 20f, offsetSeconds = 0f, size = 60f, colorIndex = 0)
        assertTrue(balloon.poseAt(0f).y > SceneHeight)
        assertTrue(balloon.poseAt(10f).y in 0f..SceneHeight)
        assertTrue(balloon.poseAt(19.9f).y < 0f)
        // And around again.
        assertTrue(balloon.poseAt(20.1f).y > SceneHeight)
        assertTrue(RisingBalloons.all { it.x in 150f..1770f })
    }
}
