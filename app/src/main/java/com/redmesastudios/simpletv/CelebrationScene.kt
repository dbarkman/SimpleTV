package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.sin

// The birthday or anniversary scene: bunting along the top, balloons rising past, and a cake
// with flickering candles between presents on the hill. Anniversaries get heart balloons.
private val BirthdayBalloons = listOf(
    Color(0xFFE8796B), Color(0xFFF2C14E), Color(0xFF7FB7D9), Color(0xFF9BCB8C), Color(0xFFC99BD9), Color(0xFFF4A6B8),
)
private val AnniversaryBalloons = listOf(Color(0xFFD9534F), Color(0xFFF4A6B8), Color(0xFFE8796B), Color(0xFFC2477A), Color(0xFFF7C6CF))
private val BalloonString = Color(0xFF9C8C7E)
private val Frosting = Color(0xFFFBF1E4)
private val CakeSide = Color(0xFFF3D9C4)
private val Drip = Color(0xFFF4A6B8)
private val Candle = listOf(Color(0xFF7FB7D9), Color(0xFFF2C14E), Color(0xFF9BCB8C), Color(0xFFC99BD9), Color(0xFFE8796B))
private val Flame = Color(0xFFF7C55A)
private val FlameCore = Color(0xFFFFF3D6)
private val Ribbon = Color(0xFFFBF1E4)

private const val CelebrationBuntingY = -8f
private const val CelebrationBuntingDip = 26f
private val CelebrationAnchors = listOf(0f, 480f, 960f, 1440f, 1920f)

fun DrawScope.drawCelebration(unit: Float, seconds: Float, anniversary: Boolean) {
    val colors = if (anniversary) AnniversaryBalloons else BirthdayBalloons
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawCelebrationBunting(seconds, colors)
        drawPresent(820f, 868f, 66f, 54f, colors[2 % colors.size], colors[0])
        drawPresent(1104f, 868f, 54f, 66f, colors[1], colors[3 % colors.size])
        drawCake(960f, 868f, seconds, candles = if (anniversary) 2 else 5)
        RisingBalloons.forEach { balloon ->
            val pose = balloon.poseAt(seconds)
            drawRisingBalloon(pose, balloon.size, colors[balloon.colorIndex % colors.size], heart = anniversary, seconds = seconds, phase = balloon.colorIndex.toFloat())
        }
    }
}

private fun DrawScope.drawCelebrationBunting(seconds: Float, colors: List<Color>) {
    var count = 0
    CelebrationAnchors.zipWithNext().forEach { (from, to) ->
        fun pointAt(t: Float) = Offset(from + (to - from) * t, CelebrationBuntingY + 4f * CelebrationBuntingDip * t * (1f - t))
        drawPath(Path().apply {
            moveTo(from, CelebrationBuntingY)
            quadraticTo((from + to) / 2, CelebrationBuntingY + 2f * CelebrationBuntingDip, to, CelebrationBuntingY)
        }, BalloonString, style = Stroke(2f))
        val pennants = 9
        repeat(pennants) { i ->
            val t = (i + 0.5f) / pennants
            val top = pointAt(t)
            if (top.y < 0f) return@repeat
            val slope = Math.toDegrees(kotlin.math.atan2((4f * CelebrationBuntingDip * (1f - 2f * t)).toDouble(), (to - from).toDouble())).toFloat()
            val sway = 4f * sin(seconds * 1.3f + count * 0.8f)
            withTransform({ rotate(slope + sway, pivot = top) }) {
                drawPath(Path().apply {
                    moveTo(top.x - 13f, top.y); lineTo(top.x + 13f, top.y); lineTo(top.x, top.y + 30f); close()
                }, colors[count % colors.size])
            }
            count++
        }
    }
}

private fun DrawScope.drawRisingBalloon(pose: BalloonPose, size: Float, color: Color, heart: Boolean, seconds: Float, phase: Float) {
    val w = size
    val h = size * 1.2f
    // The string trails below, waving a little.
    val knot = Offset(pose.x, pose.y + h / 2)
    drawPath(Path().apply {
        moveTo(knot.x, knot.y)
        quadraticTo(knot.x + 12f * sin(seconds * 1.6f + phase), knot.y + 50f, knot.x - 4f, knot.y + 110f)
    }, BalloonString, style = Stroke(1.5f))
    withTransform({ rotate(pose.tilt, pivot = Offset(pose.x, pose.y)) }) {
        if (heart) {
            val heartPath = Path().apply {
                moveTo(pose.x, pose.y + h / 2)
                cubicTo(pose.x - w * 0.9f, pose.y - h * 0.05f, pose.x - w * 0.45f, pose.y - h * 0.7f, pose.x, pose.y - h * 0.22f)
                cubicTo(pose.x + w * 0.45f, pose.y - h * 0.7f, pose.x + w * 0.9f, pose.y - h * 0.05f, pose.x, pose.y + h / 2)
                close()
            }
            drawPath(heartPath, color, alpha = 0.92f)
            drawOval(Color.White, Offset(pose.x - w * 0.32f, pose.y - h * 0.32f), Size(w * 0.14f, h * 0.2f), alpha = 0.4f)
        } else {
            drawOval(color, Offset(pose.x - w / 2, pose.y - h / 2), Size(w, h), alpha = 0.92f)
            drawPath(Path().apply {
                moveTo(knot.x - 5f, knot.y + 6f); lineTo(knot.x + 5f, knot.y + 6f); lineTo(knot.x, knot.y - 1f); close()
            }, color)
            drawOval(Color.White, Offset(pose.x - w * 0.3f, pose.y - h * 0.32f), Size(w * 0.18f, h * 0.26f), alpha = 0.4f)
        }
    }
}

private fun DrawScope.drawPresent(x: Float, baseY: Float, width: Float, height: Float, box: Color, bow: Color) {
    drawRoundRect(box, Offset(x - width / 2, baseY - height), Size(width, height), CornerRadius(4f))
    drawRect(Ribbon, Offset(x - 5f, baseY - height), Size(10f, height), alpha = 0.9f)
    drawRect(Ribbon, Offset(x - width / 2, baseY - height * 0.6f), Size(width, 9f), alpha = 0.9f)
    drawOval(bow, Offset(x - 22f, baseY - height - 13f), Size(22f, 14f))
    drawOval(bow, Offset(x, baseY - height - 13f), Size(22f, 14f))
    drawCircle(bow, 5f, Offset(x, baseY - height - 6f))
}

private fun DrawScope.drawCake(x: Float, baseY: Float, seconds: Float, candles: Int) {
    // Plate, two tiers, frosting drips along each top edge.
    drawOval(Color(0xFFE6D8C6), Offset(x - 96f, baseY - 10f), Size(192f, 18f))
    fun tier(width: Float, height: Float, bottom: Float) {
        drawRoundRect(CakeSide, Offset(x - width / 2, bottom - height), Size(width, height), CornerRadius(8f))
        drawRoundRect(Frosting, Offset(x - width / 2, bottom - height), Size(width, 14f), CornerRadius(7f))
        val drips = (width / 22f).toInt()
        repeat(drips) { i ->
            val dx = x - width / 2 + 10f + i * (width - 20f) / (drips - 1)
            drawRoundRect(Drip, Offset(dx - 4f, bottom - height + 6f), Size(8f, 12f + (i % 3) * 6f), CornerRadius(4f))
        }
        drawRoundRect(Drip, Offset(x - width / 2, bottom - height + 4f), Size(width, 6f), CornerRadius(3f))
    }
    tier(160f, 56f, baseY - 2f)
    tier(110f, 44f, baseY - 58f)
    // Candles with flickering flames.
    val top = baseY - 102f
    repeat(candles) { i ->
        val cx = x - (candles - 1) * 9f + i * 18f
        drawRoundRect(Candle[i % Candle.size], Offset(cx - 3.5f, top - 30f), Size(7f, 30f), CornerRadius(2f))
        val glow = candleGlow(seconds, i * 1.7f)
        val flick = 1.5f * sin(seconds * 9f + i)
        drawCircle(Flame, 10f * glow, Offset(cx, top - 40f), alpha = 0.25f)
        drawOval(Flame, Offset(cx - 4.5f + flick * 0.3f, top - 49f), Size(9f, 15f * glow))
        drawOval(FlameCore, Offset(cx - 2f + flick * 0.3f, top - 42f), Size(4f, 7f))
    }
}
