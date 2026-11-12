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
import kotlin.math.atan2
import kotlin.math.sin

// Lunar New Year (three days from the new moon) on the winter scene: a string of red
// lanterns swaying along the hill, a red diamond with the gold character for good fortune,
// plum blossom branches at both sides, and now and then a paper dragon weaving across the
// sky. The greeting says "Happy Lunar New Year!". Drawn in design px (1920x1080).

private val LanternRed = Color(0xFFD2403A)
private val LanternShade = Color(0xFFB0302C)
private val LanternGold = Color(0xFFE9C14A)
private val Cord = Color(0xFF8A5A3B)
private val Blossom = listOf(Color(0xFFF4A6B8), Color(0xFFF7C6CF), Color(0xFFE88AA2))
private val Branch = Color(0xFF6E5440)
private val DragonRed = Color(0xFFD2403A)
private val DragonGold = Color(0xFFE9C14A)
private val DragonBelly = Color(0xFFF7D27A)
private val DragonEye = Color(0xFF2A211B)

private const val LanternLineY = 664f
private const val LanternDip = 30f
private val LanternAnchors = listOf(200f, 585f, 970f, 1355f, 1740f)

private val FortuneText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xE9, 0xC1, 0x4A)
    textSize = 52f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

fun DrawScope.drawLunarNewYearFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawPlumBranch(Offset(150f, 860f), 1f, seconds)
        drawPlumBranch(Offset(1770f, 860f), -1f, seconds)
        drawLanterns(seconds)
        drawFortune(970f, 762f, seconds)
        dragonSegments(seconds)?.let { drawDragon(it, seconds) }
    }
}

private fun DrawScope.drawLanterns(seconds: Float) {
    var count = 0
    LanternAnchors.zipWithNext().forEach { (from, to) ->
        drawPath(Path().apply {
            moveTo(from, LanternLineY)
            quadraticTo((from + to) / 2, LanternLineY + 2f * LanternDip, to, LanternLineY)
        }, Cord, style = Stroke(2.2f))
        listOf(0.25f, 0.5f, 0.75f).forEach { t ->
            val hang = Offset(from + (to - from) * t, LanternLineY + 4f * LanternDip * t * (1f - t))
            drawLantern(hang, seconds, count, if (count % 2 == 0) 1f else 0.82f)
            count++
        }
    }
    LanternAnchors.forEach { drawCircle(LanternGold, 6f, Offset(it, LanternLineY)) }
}

// A round paper lantern on a short cord, swinging gently from where it hangs.
private fun DrawScope.drawLantern(hang: Offset, seconds: Float, index: Int, scale: Float) {
    val swing = 5f * sin(seconds * 1.2f + index * 0.9f)
    withTransform({ rotate(swing, pivot = hang); scale(scale, scale, pivot = hang) }) {
        val top = hang.y + 12f
        drawLine(Cord, hang, Offset(hang.x, top), strokeWidth = 2f)
        drawRoundRect(LanternGold, Offset(hang.x - 12f, top), Size(24f, 7f), CornerRadius(2f))
        drawOval(LanternRed, Offset(hang.x - 26f, top + 5f), Size(52f, 44f))
        // Ribs.
        listOf(-14f, 0f, 14f).forEach { dx ->
            drawOval(LanternShade, Offset(hang.x + dx - 4f - kotlin.math.abs(dx) * 0.3f, top + 6f), Size(8f + kotlin.math.abs(dx) * 0.6f, 42f), style = Stroke(1.6f))
        }
        drawOval(Color.White, Offset(hang.x - 16f, top + 12f), Size(8f, 14f), alpha = 0.25f)
        drawRoundRect(LanternGold, Offset(hang.x - 12f, top + 46f), Size(24f, 7f), CornerRadius(2f))
        // Tassel.
        repeat(5) { i -> drawLine(LanternRed, Offset(hang.x - 4f + i * 2f, top + 53f), Offset(hang.x - 5f + i * 2.5f, top + 76f), strokeWidth = 1.6f) }
        drawCircle(LanternGold, 3.5f, Offset(hang.x, top + 55f))
    }
}

// The character for good fortune on a red diamond, hung upside down as is the custom.
private fun DrawScope.drawFortune(x: Float, y: Float, seconds: Float) {
    val sway = 2.5f * sin(seconds * 0.9f)
    withTransform({ rotate(45f + sway, pivot = Offset(x, y)) }) {
        drawRoundRect(LanternRed, Offset(x - 40f, y - 40f), Size(80f, 80f), CornerRadius(6f))
        drawRoundRect(LanternGold, Offset(x - 34f, y - 34f), Size(68f, 68f), CornerRadius(4f), style = Stroke(2f))
    }
    withTransform({ rotate(180f + sway, pivot = Offset(x, y)) }) {
        drawIntoCanvas { it.nativeCanvas.drawText("福", x, y + 18f, FortuneText) }
    }
    drawLine(Cord, Offset(x, y - 57f), Offset(x, LanternLineY), strokeWidth = 2f)
}

// A plum branch reaching in from the side (dir 1 from the left, -1 from the right), dotted
// with pink blossoms that sway with it.
private fun DrawScope.drawPlumBranch(root: Offset, dir: Float, seconds: Float) {
    val sway = 2f * sin(seconds * 0.8f + dir)
    withTransform({ rotate(sway * dir, pivot = root) }) {
        val tip = Offset(root.x + 230f * dir, root.y - 150f)
        val mid = Offset(root.x + 110f * dir, root.y - 90f)
        drawPath(Path().apply {
            moveTo(root.x, root.y); quadraticTo(root.x + 40f * dir, root.y - 70f, mid.x, mid.y); quadraticTo(mid.x + 70f * dir, mid.y - 20f, tip.x, tip.y)
        }, Branch, style = Stroke(6f, cap = StrokeCap.Round))
        val twig = Offset(mid.x + 30f * dir, mid.y - 90f)
        drawLine(Branch, mid, twig, strokeWidth = 4f, cap = StrokeCap.Round)
        val blossoms = listOf(
            Offset(root.x + 50f * dir, root.y - 66f), Offset(mid.x, mid.y - 6f), Offset(mid.x + 46f * dir, mid.y - 22f),
            Offset(tip.x - 40f * dir, tip.y + 16f), tip, Offset(twig.x, twig.y), Offset(mid.x + 16f * dir, mid.y - 52f),
            Offset(tip.x - 80f * dir, tip.y + 34f),
        )
        blossoms.forEachIndexed { i, at -> drawBlossom(at, 9f + (i % 3) * 2f, Blossom[i % Blossom.size]) }
    }
}

private fun DrawScope.drawBlossom(center: Offset, radius: Float, color: Color) {
    repeat(5) { k ->
        val angle = Math.toRadians(k * 72.0 - 90.0)
        drawCircle(color, radius * 0.55f, Offset(center.x + radius * 0.55f * kotlin.math.cos(angle).toFloat(), center.y + radius * 0.55f * kotlin.math.sin(angle).toFloat()))
    }
    drawCircle(LanternGold, radius * 0.28f, center)
}

// The paper dragon: tail segments first so the head draws on top, each striped red and gold,
// with spines along its back, then a head with whiskers, horns and an eye.
private fun DrawScope.drawDragon(segments: List<Pair<Float, Float>>, seconds: Float) {
    for (k in segments.indices.reversed()) {
        val (x, y) = segments[k]
        val r = 17f - k * 0.7f
        if (k > 0) {
            val (px, py) = segments[k - 1]
            drawLine(DragonRed, Offset(px, py), Offset(x, y), strokeWidth = r * 1.7f, cap = StrokeCap.Round)
        }
        drawCircle(if (k % 2 == 0) DragonRed else DragonGold, r, Offset(x, y))
        drawCircle(DragonBelly, r * 0.45f, Offset(x, y + r * 0.35f), alpha = 0.8f)
        // A spine on top.
        drawPath(Path().apply { moveTo(x - r * 0.4f, y - r * 0.8f); lineTo(x, y - r * 1.5f); lineTo(x + r * 0.4f, y - r * 0.8f); close() }, DragonGold)
    }
    val (hx, hy) = segments[0]
    val (nx, ny) = segments[1]
    val heading = Math.toDegrees(atan2((hy - ny).toDouble(), (hx - nx).toDouble())).toFloat()
    val facingLeft = hx < nx
    withTransform({
        translate(hx, hy)
        rotate(if (facingLeft) heading - 180f else heading, pivot = Offset.Zero)
        scale(if (facingLeft) -1f else 1f, 1f, pivot = Offset.Zero)
    }) {
        // Facing right in this frame.
        drawOval(DragonRed, Offset(-14f, -18f), Size(48f, 34f))
        drawRoundRect(DragonGold, Offset(14f, -2f), Size(26f, 14f), CornerRadius(6f))
        drawCircle(Color.White, 6f, Offset(12f, -6f))
        drawCircle(DragonEye, 3f, Offset(13.5f, -6f))
        drawLine(DragonGold, Offset(0f, -16f), Offset(-14f, -36f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(DragonGold, Offset(8f, -16f), Offset(0f, -38f), strokeWidth = 4f, cap = StrokeCap.Round)
        val whisk = 6f * sin(seconds * 3f)
        drawPath(Path().apply { moveTo(36f, 4f); quadraticTo(54f, 12f + whisk, 66f, -2f + whisk) }, DragonGold, style = Stroke(2f, cap = StrokeCap.Round))
        drawPath(Path().apply { moveTo(34f, 10f); quadraticTo(48f, 24f - whisk, 62f, 22f - whisk) }, DragonGold, style = Stroke(2f, cap = StrokeCap.Round))
        drawCircle(DragonEye, 1.8f, Offset(36f, 2f))
    }
}
