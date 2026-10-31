package com.redmesastudios.simpletv

import java.time.LocalDate
import java.time.Month
import kotlin.math.PI
import kotlin.math.sin

// Holiday decorations for the scene, each shown over a stretch of days around the holiday
// and gone again afterwards on its own. Motion is pure math in design px (1920x1080).
enum class Holiday { Halloween }

fun holidayOn(date: LocalDate): Holiday? = when {
    date.month == Month.OCTOBER && date.dayOfMonth >= 24 -> Holiday.Halloween
    date.month == Month.NOVEMBER && date.dayOfMonth == 1 -> Holiday.Halloween
    else -> null
}

// A bat crosses the sky now and then: each one flies for part of its cycle and is
// off-screen the rest, so the sky is often empty.
data class Bat(
    val cycleSeconds: Float,
    val flightSeconds: Float,
    val offsetSeconds: Float,
    val y: Float,
    val size: Float,
    val leftToRight: Boolean,
)

data class BatPose(val x: Float, val y: Float, val size: Float, val flap: Float, val leftToRight: Boolean)

private const val BatMargin = 140f

val Bats = listOf(
    Bat(cycleSeconds = 38f, flightSeconds = 15f, offsetSeconds = 4f, y = 60f, size = 72f, leftToRight = true),
    Bat(cycleSeconds = 38f, flightSeconds = 17f, offsetSeconds = 7f, y = 95f, size = 54f, leftToRight = true),
    Bat(cycleSeconds = 52f, flightSeconds = 19f, offsetSeconds = 30f, y = 45f, size = 62f, leftToRight = false),
)

// Null while the bat is off-screen.
fun Bat.poseAt(seconds: Float): BatPose? {
    val t = (seconds + offsetSeconds) % cycleSeconds
    if (t > flightSeconds) return null
    val progress = t / flightSeconds
    val across = -BatMargin + progress * (SceneWidth + 2 * BatMargin)
    return BatPose(
        x = if (leftToRight) across else SceneWidth - across,
        // A loose, swooping line rather than a straight one.
        y = y + 26f * sin(2 * PI * progress * 1.5).toFloat(),
        size = size,
        // Wings beat about four times a second: 1 is up, -1 is down.
        flap = sin(2 * PI * t * 4).toFloat(),
        leftToRight = leftToRight,
    )
}

// Candlelight in a jack-o'-lantern: a slow flicker, never fully dim.
fun candleGlow(seconds: Float, phase: Float): Float =
    (0.82f + 0.1f * sin(seconds * 7.3f + phase) + 0.08f * sin(seconds * 13.1f + phase * 2f)).coerceIn(0f, 1f)

// A figure walking along the hill below the apps, now and then: across for part of its
// cycle, off-screen the rest.
data class Walker(
    val cycleSeconds: Float,
    val walkSeconds: Float,
    val offsetSeconds: Float,
    val baseY: Float,
    val leftToRight: Boolean,
    // One full step with each foot.
    val strideSeconds: Float,
)

data class WalkerPose(val x: Float, val y: Float, val stride: Float, val leftToRight: Boolean)

private const val WalkerMargin = 160f

val Skeleton = Walker(cycleSeconds = 52f, walkSeconds = 32f, offsetSeconds = 6f, baseY = 878f, leftToRight = true, strideSeconds = 1.1f)
val Mummy = Walker(cycleSeconds = 80f, walkSeconds = 58f, offsetSeconds = 40f, baseY = 880f, leftToRight = false, strideSeconds = 1.8f)

// Null while the walker is off-screen. stride swings -1..1 between the two feet forward.
fun Walker.poseAt(seconds: Float): WalkerPose? {
    val t = (seconds + offsetSeconds) % cycleSeconds
    if (t > walkSeconds) return null
    val across = -WalkerMargin + t / walkSeconds * (SceneWidth + 2 * WalkerMargin)
    val stride = sin(2 * PI * t / strideSeconds).toFloat()
    return WalkerPose(
        x = if (leftToRight) across else SceneWidth - across,
        // The body rises a little as the legs pass under it.
        y = baseY - 3f * (1f - kotlin.math.abs(stride)),
        stride = stride,
        leftToRight = leftToRight,
    )
}
