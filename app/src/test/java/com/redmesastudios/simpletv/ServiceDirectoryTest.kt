package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceDirectoryTest {
    // Names as TMDB and Watchmode spell them.
    private val tmdb = listOf(
        GuideService(1718, "ESPN"),
        GuideService(1768, "ESPN Plus"),
        GuideService(73, "Tubi TV"),
        GuideService(1794, "Starz Amazon Channel"),
        GuideService(43, "Starz"),
        GuideService(192, "YouTube"),
        GuideService(188, "YouTube Premium"),
        GuideService(300, "Pluto TV"),
    )
    private val watchmode = listOf(
        GuideService(521, "ESPN+"),
        GuideService(526, "ESPN"),
        GuideService(296, "Tubi TV"),
        GuideService(234, "STARZ (Via Amazon Prime)"),
        GuideService(232, "STARZ"),
    )

    @Test
    fun coreNamesDropTiersAndPunctuation() {
        assertEquals("espn", serviceCore("ESPN+"))
        assertEquals("espn", serviceCore("ESPN Plus"))
        assertEquals("tubi", serviceCore("Tubi - Movies & TV Shows"))
        assertEquals("tubitv", serviceCore("Tubi TV"))
        assertEquals("peacock", serviceCore("Peacock Premium Plus"))
    }

    @Test
    fun anAppMatchesEveryNameForItsService() {
        assertEquals(setOf(1718, 1768), matchService("ESPN", tmdb))
        assertEquals(setOf(521, 526), matchService("ESPN", watchmode))
        assertEquals(setOf(73), matchService("Tubi", tmdb))
    }

    @Test
    fun servicesResoldInsideAnotherAppDontCount() {
        assertEquals(setOf(43), matchService("STARZ", tmdb))
        assertEquals(setOf(232), matchService("STARZ", watchmode))
    }

    @Test
    fun appsThatArentServicesMatchNothing() {
        assertEquals(emptySet<Int>(), matchService("NASA", tmdb))
        assertEquals(emptySet<Int>(), matchService("TiviMate", tmdb))
        assertEquals(emptySet<Int>(), matchService("YouTube TV", tmdb))
    }

    @Test
    fun aMatchedAppGetsAFolderAndKnownServicesKeepTheirIds() {
        val list = ServiceList(tmdb, watchmode)

        val espn = list.resolve(StreamingApp("ESPN", "com.espn.score_center"))
        assertEquals(true, espn.hasFolder)

        val netflix = StreamingApps.first { it.label == "Netflix" }
        assertEquals(netflix, list.resolve(netflix))
        assertEquals(false, list.resolve(StreamingApp("NASA", "gov.nasa")).hasFolder)
    }
}
