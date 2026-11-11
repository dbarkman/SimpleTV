package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.sin

// Veterans Day on the fall scene: a tall flag waving in the middle of the hill, rows of small
// flags planted along it on both sides, a wreath with a red, white and blue ribbon by the
// pole, gold stars twinkling, and now and then a bald eagle gliding across the sky. The
// greeting says "Thank you, veterans". Drawn in design px (1920x1080).

private val EagleBody = Color(0xFF5B3F2C)
private val EagleWing = Color(0xFF4A3324)
private val EagleHead = Color(0xFFFBF7F0)
private val EagleBeak = Color(0xFFE9B949)
private val WreathGreen = listOf(Color(0xFF4F7A53), Color(0xFF5E8C61), Color(0xFF6E9C6D))
private val RibbonRed = Color(0xFFC9483F)
private val RibbonWhite = Color(0xFFFBF6EC)
private val RibbonBlue = Color(0xFF3A5A92)
private val Easel = Color(0xFF9C7A55)

// The eagle crosses high over the apps every 50 seconds, right to left.
val Eagle = Bat(cycleSeconds = 50f, flightSeconds = 20f, offsetSeconds = 12f, y = 112f, size = 120f, leftToRight = false)

// Small flags in rows along the hill, leaving the middle to the big flag and the wreath.
private val FieldFlags = List(16) { i ->
    val left = i < 8
    val k = if (left) i else i - 8
    Triple(if (left) 250f + k * 66f else 1190f + k * 66f, 852f + (k % 3) * 6f, i * 0.8f)
}

private val VeteransTwinkles = listOf(330f to 742f, 640f to 728f, 1290f to 726f, 1580f to 740f, 860f to 760f, 1110f to 752f)

fun DrawScope.drawVeteransDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        VeteransTwinkles.forEachIndexed { i, (x, y) -> drawTwinkle(x, y, seconds, i) }
        FieldFlags.forEach { (x, baseY, phase) -> drawHandFlag(x, baseY, seconds, phase) }
        drawWavingFlag(960f, 866f, 650f, 128f, seconds)
        drawWreath(1060f, 866f, seconds)
        Eagle.poseAt(seconds)?.let { drawEagle(it, seconds) }
    }
}

// A round wreath on a small easel, its ribbon tails stirring.
private fun DrawScope.drawWreath(x: Float, baseY: Float, seconds: Float) {
    val center = Offset(x, baseY - 74f)
    drawLine(Easel, Offset(x - 22f, baseY), Offset(x, center.y - 30f), strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(Easel, Offset(x + 22f, baseY), Offset(x, center.y - 30f), strokeWidth = 4f, cap = StrokeCap.Round)
    // Leaves: small ovals around the ring, turned along it.
    repeat(28) { i ->
        val angle = i * 360f / 28
        val radians = Math.toRadians(angle.toDouble())
        val at = Offset(center.x + 30f * kotlin.math.cos(radians).toFloat(), center.y + 30f * kotlin.math.sin(radians).toFloat())
        withTransform({ rotate(angle + 90f + (i % 2) * 30f, pivot = at) }) {
            drawOval(WreathGreen[i % WreathGreen.size], Offset(at.x - 7f, at.y - 12f), Size(14f, 24f))
        }
    }
    // A bow at the bottom with two tails.
    val bow = Offset(center.x, center.y + 30f)
    val sway = 3f * sin(seconds * 1.4f)
    drawPath(Path().apply { moveTo(bow.x, bow.y); lineTo(bow.x - 10f + sway, bow.y + 34f); lineTo(bow.x - 2f + sway, bow.y + 30f); close() }, RibbonRed)
    drawPath(Path().apply { moveTo(bow.x, bow.y); lineTo(bow.x + 12f + sway, bow.y + 34f); lineTo(bow.x + 4f + sway, bow.y + 30f); close() }, RibbonBlue)
    drawOval(RibbonRed, Offset(bow.x - 20f, bow.y - 8f), Size(20f, 14f))
    drawOval(RibbonBlue, Offset(bow.x, bow.y - 8f), Size(20f, 14f))
    drawCircle(RibbonWhite, 5f, bow)
}

// A bald eagle gliding, seen from the side: its wings rise and fall slowly, the far one a
// little behind and darker.
private fun DrawScope.drawEagle(pose: BatPose, seconds: Float) {
    val lift = 0.6f + 0.4f * sin(seconds * 1.6f)
    val wing = Path().apply {
        moveTo(10f, -4f)
        cubicTo(4f, -30f, -20f, -56f, -46f, -62f)
        lineTo(-40f, -54f)
        lineTo(-52f, -52f)
        lineTo(-41f, -46f)
        lineTo(-48f, -41f)
        cubicTo(-30f, -30f, -14f, -12f, -16f, -2f)
        close()
    }
    withTransform({
        translate(pose.x, pose.y)
        scale(if (pose.leftToRight) 1.4f else -1.4f, 1.4f, pivot = Offset.Zero)
    }) {
        withTransform({ translate(-8f, 2f); scale(0.85f, 0.85f * lift, pivot = Offset.Zero) }) { drawPath(wing, EagleWing, alpha = 0.75f) }
        drawOval(EagleBody, Offset(-28f, -8f), Size(54f, 17f))
        drawPath(Path().apply { moveTo(-24f, -1f); lineTo(-46f, -8f); lineTo(-46f, 7f); close() }, EagleHead)
        drawCircle(EagleHead, 8.5f, Offset(27f, -4f))
        drawPath(Path().apply { moveTo(33f, -6f); quadraticTo(44f, -4f, 40f, 3f); lineTo(33f, 0f); close() }, EagleBeak)
        drawCircle(EagleBody, 1.4f, Offset(29f, -6f))
        withTransform({ scale(1f, lift, pivot = Offset(0f, -4f)) }) { drawPath(wing, EagleBody) }
    }
}

// What the greeting says on a holiday that has its own words, instead of "Good morning".
fun holidayGreeting(holiday: Holiday?): String? = when (holiday) {
    Holiday.VeteransDay -> "Thank you, veterans"
    else -> null
}
