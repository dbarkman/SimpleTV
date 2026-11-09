package com.redmesastudios.simpletv

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.MonthDay

class HouseSettingsTest {
    private val baked = parseFamilyDays(
        """
        01-05 birthday Alex
        07-15 anniversary Pat & Jo
        """.trimIndent(),
    )

    @Test
    fun theBuildsListSeedsTheSettings() {
        val state = houseState(seedRecords(baked), DefaultPacks)
        assertEquals(listOf("Alex", "Pat & Jo"), state.familyDays.map { it.day.name })
        assertTrue(state.hiddenCards.isEmpty())
        assertEquals(DefaultPacks, state.packs)
        // Every stick makes the same id for the same line.
        assertEquals(seedId(baked[0]), seedRecords(baked)[0].id)
        assertNotEquals(seedId(baked[0]), seedId(baked[1]))
    }

    @Test
    fun aRemovalOutlastsTheSeedComingBack() {
        val seeds = seedRecords(baked)
        val removed = mergeShared(seeds, listOf(removedRecord(seedId(baked[0]), at = 1_000)))
        // The next start seeds again; the removal is newer, so it stays.
        val reseeded = mergeShared(removed, seedRecords(baked))
        assertEquals(listOf("Pat & Jo"), houseState(reseeded, DefaultPacks).familyDays.map { it.day.name })
    }

    @Test
    fun aLineAddedToTheFileLaterStillArrives() {
        val before = seedRecords(baked)
        val later = baked + parseFamilyDays("03-10 birthday Sam")
        val merged = mergeShared(before, seedRecords(later))
        assertEquals(listOf("Alex", "Sam", "Pat & Jo"), houseState(merged, DefaultPacks).familyDays.map { it.day.name })
    }

    @Test
    fun cardsAndPacksAreTheNewestChoice() {
        val records = mergeShared(
            listOf(cardsRecord(setOf(Channel.News), at = 1), packsRecord(setOf(Pack.FunDays), at = 1)),
            listOf(cardsRecord(setOf(Channel.Markets), at = 2), packsRecord(setOf(Pack.UsNational, Pack.Christian), at = 2)),
        )
        val state = houseState(records, DefaultPacks)
        assertEquals(setOf(Channel.Markets), state.hiddenCards)
        assertEquals(setOf(Pack.UsNational, Pack.Christian), state.packs)
        // Turning every pack off is a choice too, not "use the defaults".
        assertTrue(houseState(listOf(packsRecord(emptySet(), at = 3)), DefaultPacks).packs.isEmpty())
    }

    @Test
    fun theListIsInCalendarOrder() {
        val records = listOf(
            familyRecord("family:b", FamilyDay(MonthDay.of(12, 1), FamilyDayKind.Birthday, "Zed"), at = 5),
            familyRecord("family:a", FamilyDay(MonthDay.of(2, 3), FamilyDayKind.Birthday, "Amy"), at = 5),
        )
        assertEquals(listOf("Amy", "Zed"), houseState(records, DefaultPacks).familyDays.map { it.day.name })
    }

    @Test
    fun aPhoneEntryIsCheckedBeforeSaving() {
        fun entry(name: String, month: Int, day: Int, kind: String = "birthday") =
            familyDayFrom(JSONObject().put("name", name).put("month", month).put("day", day).put("kind", kind))
        assertEquals(FamilyDay(MonthDay.of(3, 10), FamilyDayKind.Birthday, "Sam Jones"), entry("  Sam   Jones ", 3, 10))
        assertEquals(FamilyDayKind.Anniversary, entry("Pat & Jo", 7, 15, "anniversary")!!.kind)
        assertEquals(MonthDay.of(2, 29), entry("Leap", 2, 29)!!.day)
        assertNull(entry("", 3, 10))
        assertNull(entry("Sam", 2, 30))
        assertNull(entry("Sam", 13, 1))
        assertNull(entry("Sam", 3, 10, "party"))
        assertNull(entry("x".repeat(41), 3, 10))
    }
}
