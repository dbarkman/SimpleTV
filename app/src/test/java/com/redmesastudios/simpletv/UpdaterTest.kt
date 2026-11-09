package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdaterTest {
    private val sha = "a".repeat(64)

    @Test
    fun readsEachHouseholdsLine() {
        val builds = parseBuilds(
            """
            # household versionCode sha256 asset
            home 29512345 $sha simpletv-home.apk

            mom 29512345 ${"b".repeat(64)} simpletv-mom.apk
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                Build("home", 29512345, sha, "simpletv-home.apk"),
                Build("mom", 29512345, "b".repeat(64), "simpletv-mom.apk"),
            ),
            builds,
        )
    }

    @Test
    fun skipsMalformedLines() {
        val builds = parseBuilds(
            """
            home notanumber $sha simpletv-home.apk
            home 12 tooshort simpletv-home.apk
            home 12 $sha
            mom 12 $sha simpletv-mom.apk
            """.trimIndent(),
        )
        assertEquals(listOf(Build("mom", 12, sha, "simpletv-mom.apk")), builds)
    }

    @Test
    fun updatesOnlyToANewerBuildForThisHousehold() {
        val builds = listOf(Build("home", 200, sha, "simpletv-home.apk"), Build("mom", 300, sha, "simpletv-mom.apk"))
        assertEquals(builds[0], updateFor(builds, "home", 199))
        assertNull(updateFor(builds, "home", 200))
        assertNull(updateFor(builds, "home", 250))
        assertEquals(builds[1], updateFor(builds, "mom", 1))
        assertNull(updateFor(builds, "aunt", 1))
    }
}
