package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallingLeavesTest {

    private val leaves = fallingLeaves(16)

    @Test
    fun leavesStayWithinTheirFallAndSwayBounds() {
        for (leaf in leaves) {
            var seconds = 0f
            while (seconds < 120f) {
                val pose = leaf.poseAt(seconds)
                assertTrue(pose.centerY >= -90f && pose.centerY <= SceneHeight + 90f + leaf.size)
                assertTrue(pose.centerX >= leaf.x + leaf.size / 2 - 44.01f && pose.centerX <= leaf.x + leaf.size / 2 + 44.01f)
                assertTrue(pose.rotationDegrees in -30.01f..30.01f)
                seconds += 0.25f
            }
        }
    }

    @Test
    fun aLeafReturnsToTheTopAfterOneFall() {
        val leaf = leaves.first()
        val start = leaf.poseAt(0f).centerY
        assertEquals(start, leaf.poseAt(leaf.fallSeconds).centerY, 0.5f)
    }

    @Test
    fun leavesAreStaggeredAcrossTheScreen() {
        val startingHeights = leaves.map { it.poseAt(0f).centerY.toInt() / 100 }.toSet()
        assertTrue("leaves should not all start at the same height", startingHeights.size > 4)
        assertTrue(leaves.all { it.x in 0f..SceneWidth })
    }

    @Test
    fun restingLeavesSitOnTheGroundClearOfTheFooterHints() {
        val pile = restingLeaves(30)
        assertTrue(pile.all { it.y in 990f..SceneHeight })
        assertTrue(pile.none { it.x in 120f..800f })
    }
}
