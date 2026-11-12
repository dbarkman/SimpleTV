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

// A firework now and then: the shell climbs from the hill to its peak over riseSeconds,
// bursts, and its sparks spread out and sink as they fade over burstSeconds; the rest of the
// cycle that patch of sky is clear.
data class Firework(
    val x: Float,
    val peakY: Float,
    val radius: Float,
    val cycleSeconds: Float,
    val offsetSeconds: Float,
    val colorIndex: Int,
    val launchY: Float = 880f,
    val riseSeconds: Float = 1.3f,
    val burstSeconds: Float = 2.6f,
)

// Rising: x, y is the shell. Bursting: x, y is the burst's center, spread (0..1) how far the
// sparks have flown and fade (1..0) how bright they still are.
data class FireworkPose(val x: Float, val y: Float, val rising: Boolean, val spread: Float, val fade: Float)

// Null while the sky is clear.
fun Firework.poseAt(seconds: Float): FireworkPose? {
    val t = (seconds + offsetSeconds) % cycleSeconds
    if (t < riseSeconds) {
        // Slowing as it climbs.
        val p = t / riseSeconds
        val eased = 1f - (1f - p) * (1f - p)
        return FireworkPose(x, launchY + (peakY - launchY) * eased, rising = true, spread = 0f, fade = 1f)
    }
    val a = (t - riseSeconds) / burstSeconds
    if (a >= 1f) return null
    return FireworkPose(
        x = x,
        y = peakY + 22f * a * a,
        rising = false,
        spread = 1f - (1f - a) * (1f - a) * (1f - a),
        fade = 1f - a * a,
    )
}

// The year a New Year's scene celebrates: the coming one on New Year's Eve.
fun newYearShown(day: java.time.LocalDate): Int = if (day.monthValue == 12) day.year + 1 else day.year

// A football kicked now and then from its tee along a high arc, end over end, through the
// uprights and away. Between kicks it's gone for gapSeconds, then sits back on the tee.
data class Kick(
    val cycleSeconds: Float,
    val flightSeconds: Float,
    val gapSeconds: Float,
    val offsetSeconds: Float,
    val fromX: Float,
    val toX: Float,
    val groundY: Float,
    val apexHeight: Float,
)

// flying: the ball is in the air at x, y turned spinDegrees; otherwise it's on the tee, shown
// at teeAlpha (0 while it's gone). kicks counts the kicks that have cleared the posts.
data class KickPose(val flying: Boolean, val x: Float, val y: Float, val spinDegrees: Float, val teeAlpha: Float, val kicks: Int)

private const val TeeFadeSeconds = 0.6f

// The part of the flight where the ball passes the posts, for counting the score.
private const val KickClearsAt = 0.85f

fun Kick.poseAt(seconds: Float): KickPose {
    val total = seconds + offsetSeconds
    val cycle = kotlin.math.floor(total / cycleSeconds).toInt()
    val t = total - cycle * cycleSeconds
    val cleared = cycle + if (t >= flightSeconds * KickClearsAt) 1 else 0
    if (t < flightSeconds) {
        val p = t / flightSeconds
        return KickPose(
            flying = true,
            x = fromX + (toX - fromX) * p,
            y = groundY - 4f * apexHeight * p * (1f - p),
            spinDegrees = 540f * p,
            teeAlpha = 0f,
            kicks = cleared,
        )
    }
    val waited = t - flightSeconds - gapSeconds
    val alpha = if (waited < 0f) 0f else minOf(1f, waited / TeeFadeSeconds)
    return KickPose(flying = false, x = fromX, y = groundY, spinDegrees = 0f, teeAlpha = alpha, kicks = cleared)
}

// The home side starts at 14 and gets 3 for every kick, the guests sit at 17.
fun homeScore(kicks: Int): Int = 14 + 3 * (kicks % 8)

// The Epiphany caravan: three wise men on camels walking right to left, one behind another,
// each its own walker a few seconds apart so each one enters and leaves the screen on its own.
val Caravan = listOf(10f, 6.1f, 2.2f).map { offset ->
    Walker(cycleSeconds = 72f, walkSeconds = 46f, offsetSeconds = offset, baseY = 872f, leftToRight = false, strideSeconds = 1.6f)
}

// A popcorn kernel popping out of the bucket: up and out along an arc, turning, then
// dropping back below the rim. dx, dy are from the middle of the bucket's mouth.
data class KernelPose(val dx: Float, val dy: Float, val spinDegrees: Float)

private const val KernelFlightSeconds = 1.1f

// Null while that kernel is resting in the bucket.
fun poppedKernel(seconds: Float, index: Int): KernelPose? {
    val cycle = 1.9f + (index % 5) * 0.27f
    val t = (seconds + index * 0.61f) % cycle
    if (t > KernelFlightSeconds) return null
    val p = t / KernelFlightSeconds
    val reach = listOf(-58f, 34f, -22f, 64f, -40f, 12f, 48f, -66f)[index % 8]
    val height = 70f + (index * 37) % 50
    return KernelPose(dx = reach * p, dy = -4f * height * p * (1f - p) + 30f * p * p, spinDegrees = 400f * p * if (index % 2 == 0) 1f else -1f)
}

// The groundhog's day, over and over: hidden in its burrow, up it comes, looks left and right,
// sees its shadow, gives a start, and ducks back down. rise is how far out it is (0 to 1),
// look which way it's turned (-1 left to 1 right).
data class GroundhogPose(val rise: Float, val look: Float, val startled: Boolean)

const val GroundhogCycleSeconds = 14f

fun groundhogAt(seconds: Float): GroundhogPose {
    val t = seconds % GroundhogCycleSeconds
    fun ease(p: Float) = p * p * (3f - 2f * p)
    return when {
        t < 3f -> GroundhogPose(0f, 0f, false)
        t < 4f -> GroundhogPose(ease(t - 3f), 0f, false)
        t < 9f -> GroundhogPose(1f, sin(2 * PI * (t - 4f) / 5f).toFloat(), false)
        t < 9.6f -> GroundhogPose(1f, 0f, true)
        t < 10f -> GroundhogPose(1f - ease((t - 9.6f) / 0.4f), 0f, true)
        else -> GroundhogPose(0f, 0f, false)
    }
}

// A paper dragon weaving across the sky for Lunar New Year: its head crosses like a bat and
// each body segment follows a step behind on the same wave. Null while it's all off-screen.
val Dragon = Bat(cycleSeconds = 42f, flightSeconds = 22f, offsetSeconds = 6f, y = 118f, size = 30f, leftToRight = false)

const val DragonSegments = 12
const val DragonSpacing = 30f

fun dragonSegments(seconds: Float): List<Pair<Float, Float>>? {
    val t = (seconds + Dragon.offsetSeconds) % Dragon.cycleSeconds
    if (t > Dragon.flightSeconds) return null
    val travel = SceneWidth + 2 * BatMargin + DragonSegments * DragonSpacing
    val head = -BatMargin + t / Dragon.flightSeconds * travel
    val headX = if (Dragon.leftToRight) head else SceneWidth - head
    val behind = if (Dragon.leftToRight) -DragonSpacing else DragonSpacing
    return List(DragonSegments) { k ->
        (headX + k * behind) to (Dragon.y + 20f * sin(t * 2.4f - k * 0.55f))
    }
}

// A small heart rising off the hill for Valentine's and fading before it reaches the apps.
data class RisingHeart(val x: Float, val riseSeconds: Float, val offsetSeconds: Float, val size: Float, val colorIndex: Int)

data class HeartPose(val x: Float, val y: Float, val alpha: Float)

const val HeartStartY = 880f
const val HeartRise = 250f

val RisingHearts = List(10) { i ->
    RisingHeart(x = 200f + ((i * 433) % 1520), riseSeconds = 9f + (i * 5) % 6, offsetSeconds = i * 1.9f, size = 18f + (i * 7) % 14, colorIndex = i)
}

fun RisingHeart.poseAt(seconds: Float): HeartPose {
    val p = ((seconds + offsetSeconds) % riseSeconds) / riseSeconds
    val alpha = when {
        p < 0.15f -> p / 0.15f
        p > 0.65f -> (1f - p) / 0.35f
        else -> 1f
    }
    return HeartPose(x + 14f * sin(2 * PI * p * 2 + colorIndex).toFloat(), HeartStartY - p * HeartRise, alpha)
}
