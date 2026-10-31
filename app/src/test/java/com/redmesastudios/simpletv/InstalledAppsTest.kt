package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Test

class InstalledAppsTest {
    @Test
    fun keepsOnlyInstalledAppsInHomeScreenOrder() {
        val installed = setOf("com.netflix.ninja", "com.google.android.youtube.tv", "com.pbs.video")

        val result = installedApps(StreamingApps) { it in installed }

        assertEquals(listOf("YouTube", "Netflix", "PBS"), result.map { it.label })
    }

    @Test
    fun nothingInstalledMeansAnEmptyGrid() {
        assertEquals(emptyList<StreamingApp>(), installedApps(StreamingApps) { false })
    }

    @Test
    fun everyInstalledAppGetsATileWithKnownServicesFirst() {
        val installed = mapOf(
            "com.tubitv" to "Tubi",
            "com.netflix.ninja" to "Netflix",
            "com.haystack.android" to "Haystack News",
            "com.google.android.youtube.tv" to "YouTube",
            "air.com.vudu.air.DownloaderTablet" to "Fandango at Home",
        )

        val result = homeApps(installed)

        assertEquals(listOf("YouTube", "Netflix", "Fandango at Home", "Haystack News", "Tubi"), result.map { it.label })
    }

    @Test
    fun appsWithTheirOwnButtonAndSimpleTvItselfGetNoTile() {
        val installed = mapOf(
            "com.android.vending" to "Google Play Store",
            "com.android.tv.settings" to "Settings",
            BuildConfig.APPLICATION_ID to "SimpleTV",
            "com.tubitv" to "Tubi",
        )

        assertEquals(listOf("com.tubitv"), homeApps(installed).map { it.packageName })
    }

    @Test
    fun packageNamesAreUnique() {
        assertEquals(StreamingApps.size, StreamingApps.map { it.packageName }.toSet().size)
    }
}
