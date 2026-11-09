package com.redmesastudios.simpletv

import kotlin.math.PI
import kotlin.math.sin

// Motion for the holiday decorations, pure math in design px (1920x1080). Which holiday
// shows when is HolidayCalendar.kt.

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

// Uncle Sam marches along the hill now and then, stopping by the flag to wave: in from
// the left to stopX for walkSeconds, a wave for pauseSeconds, out to the right for
// walkSeconds, then off-screen the rest of the cycle.
data class Marcher(
    val cycleSeconds: Float,
    val walkSeconds: Float,
    val pauseSeconds: Float,
    val stopX: Float,
    val offsetSeconds: Float,
    val baseY: Float,
    val strideSeconds: Float,
)

// raise is how far the waving arm is up: 0 while marching, easing to 1 during the stop.
data class MarcherPose(val x: Float, val y: Float, val stride: Float, val raise: Float)

private const val MarcherMargin = 160f
private const val RaiseSeconds = 0.6f

val UncleSam = Marcher(cycleSeconds = 75f, walkSeconds = 18f, pauseSeconds = 5f, stopX = 850f, offsetSeconds = 3f, baseY = 878f, strideSeconds = 1.25f)

// Null while he's off-screen.
fun Marcher.poseAt(seconds: Float): MarcherPose? {
    val t = (seconds + offsetSeconds) % cycleSeconds
    if (t > 2 * walkSeconds + pauseSeconds) return null
    val inward = stopX + MarcherMargin
    val outward = SceneWidth - stopX + MarcherMargin
    val (x, stride, raise) = when {
        t < walkSeconds -> Triple(-MarcherMargin + t / walkSeconds * inward, sin(2 * PI * t / strideSeconds).toFloat(), 0f)
        t < walkSeconds + pauseSeconds -> {
            val stopped = t - walkSeconds
            val raise = minOf(stopped, pauseSeconds - stopped, RaiseSeconds) / RaiseSeconds
            Triple(stopX, 0f, raise)
        }
        else -> {
            val walked = t - pauseSeconds
            Triple(stopX + (walked - walkSeconds) / walkSeconds * outward, sin(2 * PI * walked / strideSeconds).toFloat(), 0f)
        }
    }
    // A steady march: the body rises only a little as the legs pass under it.
    return MarcherPose(x, baseY - 2f * (1f - kotlin.math.abs(stride)), stride, raise)
}

// A balloon rising past the screen for a family birthday or anniversary: up from below the
// hill to above the top over riseSeconds, swaying as it goes, then around again.
data class RisingBalloon(val x: Float, val riseSeconds: Float, val offsetSeconds: Float, val size: Float, val colorIndex: Int)

data class BalloonPose(val x: Float, val y: Float, val tilt: Float)

private const val BalloonMargin = 160f

val RisingBalloons = List(9) { i ->
    RisingBalloon(
        x = 230f + ((i * 389) % 1460),
        riseSeconds = 22f + (i * 7) % 13,
        offsetSeconds = i * 4.3f,
        size = 54f + (i * 11) % 22,
        colorIndex = i,
    )
}

fun RisingBalloon.poseAt(seconds: Float): BalloonPose {
    val t = (seconds + offsetSeconds) % riseSeconds
    val progress = t / riseSeconds
    val sway = sin(2 * PI * (seconds + offsetSeconds) / 7.0).toFloat()
    return BalloonPose(
        x = x + 30f * sway,
        y = SceneHeight + BalloonMargin - progress * (SceneHeight + 2 * BalloonMargin),
        tilt = 6f * sway,
    )
}
