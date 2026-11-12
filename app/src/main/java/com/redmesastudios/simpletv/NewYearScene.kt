package com.redmesastudios.simpletv

import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.cos
import kotlin.math.sin

// New Year's Eve and Day on the winter scene: a marquee sign in the middle of the hill with
// the coming year in lights, fireworks bursting over the hill and up between the greeting and
// the clock, bunches of gold, silver and midnight balloons, and stars twinkling. The greeting
// says "Happy New Year!". Drawn in design px (1920x1080).

private val SparkColors = listOf(Color(0xFFF2C14E), Color(0xFFE8796B), Color(0xFF7FB7D9), Color(0xFF9BCB8C), Color(0xFFC99BD9), Color(0xFFF4A6B8))
private val Marquee = Color(0xFF2E3550)
private val MarqueeEdge = Color(0xFF454E70)
private val Bulb = Color(0xFFF7D27A)
private val SignPost = Color(0xFF8C8478)
private val Gold = Color(0xFFE9C46A)
private val Silver = Color(0xFFCBD2DA)
private val Midnight = Color(0xFF3E4A78)
private val BalloonString = Color(0xFF9C8C7E)

private const val SignX = 960f
private const val SignBaseY = 868f
private const val SignWidth = 330f
private const val SignHeight = 140f
private const val SignTop = 686f

// Three over the hill on each side of the sign, and two up top between the greeting and the
// clock, each on its own rhythm so they never all go at once.
val Fireworks = listOf(
    Firework(x = 300f, peakY = 724f, radius = 88f, cycleSeconds = 7.0f, offsetSeconds = 0f, colorIndex = 0),
    Firework(x = 560f, peakY = 716f, radius = 76f, cycleSeconds = 8.5f, offsetSeconds = 3.1f, colorIndex = 1),
    Firework(x = 1370f, peakY = 714f, radius = 80f, cycleSeconds = 7.7f, offsetSeconds = 5.2f, colorIndex = 2),
    Firework(x = 1630f, peakY = 724f, radius = 90f, cycleSeconds = 9.1f, offsetSeconds = 1.6f, colorIndex = 3),
    Firework(x = 780f, peakY = 104f, radius = 66f, cycleSeconds = 10.3f, offsetSeconds = 6.4f, colorIndex = 4, launchY = 236f, riseSeconds = 1f),
    Firework(x = 1060f, peakY = 92f, radius = 60f, cycleSeconds = 11.9f, offsetSeconds = 2.3f, colorIndex = 5, launchY = 236f, riseSeconds = 1f),
)

private val NewYearTwinkles = listOf(170f to 760f, 440f to 790f, 760f to 770f, 1160f to 772f, 1500f to 788f, 1760f to 768f)

private val YearText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xF7, 0xD2, 0x7A)
    textSize = 74f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.06f
}

private val HappyText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xFB, 0xF6, 0xEC)
    textSize = 21f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.22f
}

fun DrawScope.drawNewYearFigures(unit: Float, seconds: Float, year: Int) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        NewYearTwinkles.forEachIndexed { i, (x, y) -> drawTwinkle(x, y, seconds, i) }
        Fireworks.forEach { firework -> firework.poseAt(seconds)?.let { drawFirework(it, firework, seconds) } }
        drawBalloonBunch(760f, 866f, listOf(Gold, Silver, Midnight), seconds, 0f)
        drawBalloonBunch(1170f, 866f, listOf(Midnight, Gold, Silver), seconds, 2f)
        drawMarquee(year, seconds)
    }
}

private fun DrawScope.drawFirework(pose: FireworkPose, firework: Firework, seconds: Float) {
    val color = SparkColors[firework.colorIndex % SparkColors.size]
    if (pose.rising) {
        // The shell and a short fading trail below it.
        drawLine(color, Offset(pose.x, pose.y), Offset(pose.x, pose.y + 34f), strokeWidth = 2.5f, cap = StrokeCap.Round, alpha = 0.35f)
        drawCircle(Color.White, 3.2f, Offset(pose.x, pose.y), alpha = 0.95f)
        drawCircle(color, 6f, Offset(pose.x, pose.y), alpha = 0.4f)
        return
    }
    val center = Offset(pose.x, pose.y)
    // A soft flash as it opens.
    if (pose.spread < 0.5f) drawCircle(color, firework.radius * 0.7f, center, alpha = 0.28f * (1f - pose.spread * 2f))
    val sparks = 22
    val second = SparkColors[(firework.colorIndex + 2) % SparkColors.size]
    repeat(sparks) { i ->
        val angle = (i * 360f / sparks + firework.colorIndex * 11f) * (Math.PI / 180f).toFloat()
        val r = firework.radius * pose.spread * (if (i % 2 == 0) 1f else 0.82f)
        val tip = Offset(center.x + r * cos(angle), center.y + r * sin(angle))
        val tail = Offset(center.x + r * 0.6f * cos(angle), center.y + r * 0.6f * sin(angle))
        val sparkColor = if (i % 3 == 0) second else color
        drawLine(sparkColor, tail, tip, strokeWidth = 3.6f, cap = StrokeCap.Round, alpha = 0.85f * pose.fade)
        // The sparks glitter as they fade.
        val glitter = 0.6f + 0.4f * sin(seconds * 18f + i * 2.1f)
        drawCircle(sparkColor, 4.2f, tip, alpha = pose.fade * glitter)
    }
}

// Three balloons on strings tied to one weight, bobbing out of step.
private fun DrawScope.drawBalloonBunch(x: Float, baseY: Float, colors: List<Color>, seconds: Float, phase: Float) {
    val knot = Offset(x, baseY - 8f)
    listOf(Offset(x - 30f, 768f), Offset(x + 4f, 752f), Offset(x + 34f, 772f)).forEachIndexed { i, center ->
        val p = phase + i * 2.1f
        val c = Offset(center.x + 3f * sin(seconds * 0.8f + p), center.y + 5f * sin(seconds * 1.2f + p))
        val color = colors[i % colors.size]
        drawPath(Path().apply {
            moveTo(c.x, c.y + 30f)
            quadraticTo((c.x + knot.x) / 2 + 6f * sin(seconds * 1.5f + p), (c.y + knot.y) / 2, knot.x, knot.y)
        }, BalloonString, style = Stroke(1.5f))
        drawOval(color, Offset(c.x - 24f, c.y - 29f), Size(48f, 58f), alpha = 0.95f)
        drawPath(Path().apply { moveTo(c.x - 5f, c.y + 34f); lineTo(c.x + 5f, c.y + 34f); lineTo(c.x, c.y + 28f); close() }, color)
        drawOval(Color.White, Offset(c.x - 14f, c.y - 19f), Size(9f, 15f), alpha = 0.45f)
    }
    drawRoundRect(SignPost, Offset(knot.x - 7f, knot.y), Size(14f, 9f), CornerRadius(3f))
}

// The coming year in lights on a marquee sign, its border bulbs chasing round.
private fun DrawScope.drawMarquee(year: Int, seconds: Float) {
    val left = SignX - SignWidth / 2
    val bottom = SignTop + SignHeight
    drawLine(SignPost, Offset(left + 50f, bottom), Offset(left + 50f, SignBaseY), strokeWidth = 8f, cap = StrokeCap.Round)
    drawLine(SignPost, Offset(left + SignWidth - 50f, bottom), Offset(left + SignWidth - 50f, SignBaseY), strokeWidth = 8f, cap = StrokeCap.Round)
    drawRoundRect(Marquee, Offset(left, SignTop), Size(SignWidth, SignHeight), CornerRadius(14f))
    drawRoundRect(MarqueeEdge, Offset(left + 6f, SignTop + 6f), Size(SignWidth - 12f, SignHeight - 12f), CornerRadius(10f), style = Stroke(2f))
    // Bulbs every 22 px around the border; a bright spot runs around them.
    val inset = 14f
    val across = ((SignWidth - 2 * inset) / 22f).toInt()
    val down = ((SignHeight - 2 * inset) / 22f).toInt()
    val bulbs = buildList {
        for (i in 0..across) add(Offset(left + inset + i * (SignWidth - 2 * inset) / across, SignTop + inset))
        for (i in 1..down) add(Offset(left + SignWidth - inset, SignTop + inset + i * (SignHeight - 2 * inset) / down))
        for (i in across - 1 downTo 0) add(Offset(left + inset + i * (SignWidth - 2 * inset) / across, bottom - inset))
        for (i in down - 1 downTo 1) add(Offset(left + inset, SignTop + inset + i * (SignHeight - 2 * inset) / down))
    }
    val chase = (seconds * 9f) % bulbs.size
    bulbs.forEachIndexed { i, at ->
        val distance = minOf(kotlin.math.abs(i - chase), bulbs.size - kotlin.math.abs(i - chase))
        val lit = 0.45f + 0.55f * (1f - minOf(distance, 4f) / 4f)
        drawCircle(Bulb, 7f, at, alpha = 0.18f * lit)
        drawCircle(Bulb, 3.6f, at, alpha = lit)
    }
    drawIntoCanvas {
        it.nativeCanvas.drawText("HAPPY NEW YEAR", SignX, SignTop + 50f, HappyText)
        it.nativeCanvas.drawText(year.toString(), SignX, SignTop + 116f, YearText)
    }
}
