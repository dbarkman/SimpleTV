package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId

class SceneChoiceTest {
    private val zone = ZoneId.of("America/Phoenix")

    @Test
    fun holidaysWithoutARoomKeepTheSeason() {
        assertEquals(BackdropKind.Outdoors(Season.Winter), backdropFor(null, LocalDate.of(2027, 1, 27), zone = zone))
        assertEquals(BackdropKind.Outdoors(Season.Fall), backdropFor(Holiday.Thanksgiving, LocalDate.of(2026, 11, 26), zone = zone))
        // Christmas is winter even before winter starts.
        assertEquals(BackdropKind.Outdoors(Season.Winter), backdropFor(Holiday.Christmas, LocalDate.of(2026, 12, 5), zone = zone))
        assertEquals(BackdropKind.Emerald, backdropFor(Holiday.StPatricks, LocalDate.of(2027, 3, 15), zone = zone))
        assertEquals(BackdropKind.Indoors(Room.Pizzeria), backdropFor(Holiday.PizzaDay, LocalDate.of(2027, 2, 9), zone = zone))
    }

    @Test
    fun piDayHasThreeRoomsToChooseFrom() {
        val day = LocalDate.of(2027, 3, 14)
        assertEquals(BackdropKind.Indoors(Room.Classroom), backdropFor(Holiday.PiDay, day, 0, zone))
        assertEquals(BackdropKind.Indoors(Room.Bakery), backdropFor(Holiday.PiDay, day, 1, zone))
        assertEquals(BackdropKind.Indoors(Room.ComputerRoom), backdropFor(Holiday.PiDay, day, 2, zone))
        // Up from the first wraps to the last.
        assertEquals(BackdropKind.Indoors(Room.ComputerRoom), backdropFor(Holiday.PiDay, day, -1, zone))
    }

    @Test
    fun previewListsTheNextYearSoonestFirst() {
        val family = listOf(FamilyDay(MonthDay.of(3, 2), FamilyDayKind.Birthday, "Sam"), FamilyDay(MonthDay.of(6, 1), FamilyDayKind.Anniversary, "Pat & Lee"))
        val entries = previewEntries(LocalDate.of(2026, 11, 12), DefaultPacks, family, zone)
        assertEquals(entries.sortedBy { it.day }, entries)
        assertTrue(entries.first().day >= LocalDate.of(2026, 11, 12))
        assertTrue(entries.last().day < LocalDate.of(2027, 11, 12))
        fun dayOf(holiday: Holiday) = entries.single { it.holiday == holiday }.day
        assertEquals(LocalDate.of(2026, 11, 26), dayOf(Holiday.Thanksgiving))
        assertEquals(LocalDate.of(2026, 12, 25), dayOf(Holiday.Christmas))
        assertEquals(LocalDate.of(2026, 12, 31), dayOf(Holiday.NewYear))
        assertEquals(LocalDate.of(2027, 2, 14), dayOf(Holiday.Valentines))
        assertEquals(LocalDate.of(2027, 3, 17), dayOf(Holiday.StPatricks))
        assertEquals(LocalDate.of(2027, 10, 31), dayOf(Holiday.Halloween))
        // Veterans Day 2026 has passed, so it's next year's.
        assertEquals(LocalDate.of(2027, 11, 11), dayOf(Holiday.VeteransDay))
        assertEquals(FamilyDayKind.Birthday, entries.single { it.label == "A birthday" }.family)
        assertEquals(LocalDate.of(2027, 3, 2), entries.single { it.label == "A birthday" }.day)
        assertEquals(4, entries.count { it.holiday == null && it.family == null })
        // Every drawn holiday in the household's packs is there once.
        assertEquals(DrawnHolidays.count { it.pack in DefaultPacks }, entries.count { it.holiday != null })
    }

    @Test
    fun previewStepsAroundAndTurnsBackdrops() {
        val entries = listOf(
            PreviewEntry("Pi Day", LocalDate.of(2027, 3, 14), Holiday.PiDay),
            PreviewEntry("St. Patrick's Day", LocalDate.of(2027, 3, 17), Holiday.StPatricks),
        )
        val start = PreviewState(entries)
        assertTrue(previewCaption(start).contains("Backdrop 1 of 3"))
        assertTrue(previewCaption(start.turn(1)).contains("Backdrop 2 of 3"))
        assertEquals(1, start.step(-1).index)
        assertEquals(0, start.step(2).index)
        // Stepping to another holiday starts on its chosen backdrop.
        assertEquals(0, start.turn(2).step(1).variant)
        assertTrue(!previewCaption(start.step(1)).contains("Backdrop"))
    }

    @Test
    fun bannerSpellsTheBirthdaysElseTheAnniversaries() {
        val ryan = FamilyDay(MonthDay.of(11, 17), FamilyDayKind.Birthday, "Alex")
        val sam = FamilyDay(MonthDay.of(11, 17), FamilyDayKind.Birthday, "Sam")
        val couple = FamilyDay(MonthDay.of(11, 17), FamilyDayKind.Anniversary, "Pat & Lee")
        assertEquals("Alex", celebrationNames(listOf(ryan)))
        assertEquals("Alex & Sam", celebrationNames(listOf(ryan, sam)))
        assertEquals("Alex", celebrationNames(listOf(ryan, couple)))
        assertEquals("Pat & Lee", celebrationNames(listOf(couple)))
    }
}
