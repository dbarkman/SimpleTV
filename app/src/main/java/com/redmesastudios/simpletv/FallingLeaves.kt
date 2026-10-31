package com.redmesastudios.simpletv

import kotlin.math.PI
import kotlin.math.cos

// Leaf motion for the fall scene, in the design's 1920x1080 px space.

const val SceneWidth = 1920f
const val SceneHeight = 1080f

// Leaves start this far above the screen and finish this far below it.
private const val Margin = 90f
private const val SwayDistance = 44f
private const val SwayDegrees = 30f

data class FallingLeaf(
    val x: Float,
    val size: Float,
    val fallSeconds: Float,
    val swaySeconds: Float,
    val spinSeconds: Float,
    // How far into its fall the leaf already is at time zero, so they don't all start together.
    val headStartSeconds: Float,
    val colorIndex: Int,
)

data class LeafPose(
    val centerX: Float,
    val centerY: Float,
    val rotationDegrees: Float,
    // Width squash that reads as the leaf turning over in the air.
    val turnScaleX: Float,
)

data class RestingLeaf(val x: Float, val y: Float, val rotationDegrees: Float, val size: Float, val colorIndex: Int)

// Spread like the design: varied speeds and sizes, staggered starts, no clumping.
fun fallingLeaves(count: Int): List<FallingLeaf> = List(count) { i ->
    val fallSeconds = 16f + (i * 7) % 12
    FallingLeaf(
        x = ((i * 137 + 60) % 1880).toFloat(),
        size = 24f + (i * 5) % 18,
        fallSeconds = fallSeconds,
        swaySeconds = 3f + i % 4,
        spinSeconds = 5f + (i % 5) * 2,
        headStartSeconds = (i * 3.7f) % fallSeconds,
        colorIndex = i,
    )
}

// The pile on the ground, kept to the right so it stays clear of the footer hints.
fun restingLeaves(count: Int): List<RestingLeaf> = List(count) { i ->
    RestingLeaf(
        x = if (i < 5) ((i * 23) % 100).toFloat() else (820 + (i * 53) % 1080).toFloat(),
        y = (1000 + (i * 13) % 62).toFloat(),
        rotationDegrees = ((i * 47) % 360).toFloat(),
        size = 18f + (i * 3) % 14,
        colorIndex = i * 5,
    )
}

fun FallingLeaf.poseAt(seconds: Float): LeafPose {
    val t = seconds + headStartSeconds
    val progress = (t % fallSeconds) / fallSeconds
    // Eased back and forth, like a leaf rocking on the air.
    val sway = -cos(PI * t / swaySeconds).toFloat()
    return LeafPose(
        centerX = x + size / 2f + sway * SwayDistance,
        centerY = -Margin + progress * (SceneHeight + 2 * Margin) + size / 2f,
        rotationDegrees = sway * SwayDegrees,
        turnScaleX = cos(2 * PI * t / spinSeconds).toFloat(),
    )
}
