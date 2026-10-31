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
    fun packageNamesAreUnique() {
        assertEquals(StreamingApps.size, StreamingApps.map { it.packageName }.toSet().size)
    }
}
