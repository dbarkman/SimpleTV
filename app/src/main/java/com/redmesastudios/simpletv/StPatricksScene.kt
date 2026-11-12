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

// St. Patrick's (March 14th through the 17th), on its own all-green backdrop (green hills and
// trees, nothing drifting down, whatever the season): a rainbow arching down to a pot of gold with
// coins glinting, a garland of shamrocks swagged along the left of the hill, shamrocks
// nodding in the grass, and a green top hat with a gold buckle resting on a stump. The
// greeting says "Happy St. Patrick's Day!" on the 17th. Drawn in design px (1920x1080).

private val RainbowBands = listOf(
    Color(0xFFE57373), Color(0xFFF2A65A), Color(0xFFF2D26B), Color(0xFF8FC98A), Color(0xFF7FB7D9), Color(0xFFA48BD0),
)
private val Cloud = Color(0xFFFDFDFD)
private val CloudShade = Color(0xFFE6EAEE)
private val Shamrock = listOf(Color(0xFF4E9A5B), Color(0xFF5FAE69), Color(0xFF3F8A4E))
private val PotBlack = Color(0xFF2E2A28)
private val PotRim = Color(0xFF4A4440)
private val Gold = Color(0xFFE9C14A)
private val GoldDark = Color(0xFFC9A23A)
private val HatGreen = Color(0xFF3F8A4E)
private val HatBand = Color(0xFF2A211B)
private val Stump = Color(0xFF8C6A4F)
private val StumpTop = Color(0xFFC9A16E)
private val Twine = Color(0xFF8A7B6A)

private val RainbowCenter = Offset(1430f, 872f)
private const val RainbowInner = 128f
private const val BandWidth = 13f

private const val ShamrockLineY = 664f
private const val ShamrockDip = 30f
private val ShamrockAnchors = listOf(180f, 560f, 940f)

private val GrassShamrocks = listOf(250f, 330f, 460f, 700f, 760f, 880f, 1050f, 1120f, 1720f, 1780f)

fun DrawScope.drawStPatricksFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawRainbow()
        drawShamrockGarland(seconds)
        GrassShamrocks.forEachIndexed { i, x -> drawShamrock(Offset(x, 856f + (i % 3) * 4f), 13f + (i % 2) * 3f, 8f * sin(seconds * 1.1f + i), Shamrock[i % Shamrock.size], stem = true) }
        drawHatOnStump(560f, 868f)
        drawPotOfGold(RainbowCenter.x + RainbowInner + BandWidth * 3f, 868f, seconds)
    }
}

private fun DrawScope.drawRainbow() {
    RainbowBands.forEachIndexed { i, color ->
        val r = RainbowInner + BandWidth * (RainbowBands.size - i - 0.5f)
        drawArc(color, 180f, 180f, useCenter = false, topLeft = Offset(RainbowCenter.x - r, RainbowCenter.y - r), size = Size(r * 2, r * 2),
            style = Stroke(BandWidth + 0.8f), alpha = 0.8f)
    }
    // A cloud where the rainbow starts on the left.
    val c = Offset(RainbowCenter.x - RainbowInner - BandWidth * 3f, RainbowCenter.y - 10f)
    listOf(Offset(-34f, 4f) to 24f, Offset(-8f, -10f) to 30f, Offset(24f, -2f) to 26f, Offset(46f, 8f) to 18f).forEach { (d, r) ->
        drawCircle(CloudShade, r, Offset(c.x + d.x, c.y + d.y + 4f))
        drawCircle(Cloud, r, Offset(c.x + d.x, c.y + d.y))
    }
}

// Three heart-shaped leaves around a center, turned degrees.
private fun DrawScope.drawShamrock(center: Offset, size: Float, degrees: Float, color: Color, stem: Boolean = false) {
    withTransform({ rotate(degrees, pivot = center) }) {
        if (stem) drawPath(Path().apply { moveTo(center.x, center.y); quadraticTo(center.x + 3f, center.y + size, center.x - 2f, center.y + size * 1.6f) },
            color, style = Stroke(2f, cap = StrokeCap.Round))
        listOf(-90f, 30f, 150f).forEach { angle ->
            withTransform({ rotate(angle + 90f, pivot = center) }) {
                drawHeart(Offset(center.x, center.y - size * 0.55f), size, color)
            }
        }
        drawCircle(color, size * 0.18f, center)
    }
}

private fun DrawScope.drawShamrockGarland(seconds: Float) {
    var count = 0
    ShamrockAnchors.zipWithNext().forEach { (from, to) ->
        drawPath(Path().apply { moveTo(from, ShamrockLineY); quadraticTo((from + to) / 2, ShamrockLineY + 2f * ShamrockDip, to, ShamrockLineY) },
            Twine, style = Stroke(2f))
        val leaves = 7
        repeat(leaves) { i ->
            val t = (i + 0.5f) / leaves
            val at = Offset(from + (to - from) * t, ShamrockLineY + 4f * ShamrockDip * t * (1f - t))
            val sway = 6f * sin(seconds * 1.3f + count * 0.8f)
            drawLine(Twine, at, Offset(at.x, at.y + 6f), strokeWidth = 1.5f)
            drawShamrock(Offset(at.x, at.y + 20f), 16f, sway, Shamrock[count % Shamrock.size])
            count++
        }
    }
    ShamrockAnchors.forEach { drawCircle(Gold, 5f, Offset(it, ShamrockLineY)) }
}

private fun DrawScope.drawPotOfGold(x: Float, baseY: Float, seconds: Float) {
    // Coins heaped above the rim, glinting in turn.
    val coins = listOf(-34f to -8f, -16f to -16f, 4f to -20f, 22f to -14f, 38f to -6f, -24f to -2f, 12f to -4f, -4f to -10f)
    val rim = baseY - 66f
    coins.forEachIndexed { i, (dx, dy) ->
        val at = Offset(x + dx, rim + dy)
        drawOval(GoldDark, Offset(at.x - 11f, at.y - 5f), Size(22f, 11f))
        drawOval(Gold, Offset(at.x - 10f, at.y - 6f), Size(20f, 9f))
        val glint = (sin(seconds * 2.2f + i * 1.3f) - 0.75f).coerceAtLeast(0f) * 4f
        if (glint > 0f) {
            drawLine(Color.White, Offset(at.x - 6f * glint, at.y - 2f), Offset(at.x + 6f * glint, at.y - 2f), strokeWidth = 1.6f, alpha = glint)
            drawLine(Color.White, Offset(at.x, at.y - 2f - 6f * glint), Offset(at.x, at.y - 2f + 6f * glint), strokeWidth = 1.6f, alpha = glint)
        }
    }
    // The pot: a round black belly, a thick rim and little feet.
    drawCircle(PotBlack, 8f, Offset(x - 30f, baseY - 4f))
    drawCircle(PotBlack, 8f, Offset(x + 30f, baseY - 4f))
    drawOval(PotBlack, Offset(x - 52f, rim - 2f), Size(104f, 70f))
    drawRoundRect(PotRim, Offset(x - 54f, rim - 6f), Size(108f, 14f), CornerRadius(7f))
    drawOval(Color.White, Offset(x - 36f, rim + 18f), Size(12f, 22f), alpha = 0.12f)
}

private fun DrawScope.drawHatOnStump(x: Float, baseY: Float) {
    drawRect(Stump, Offset(x - 40f, baseY - 40f), Size(80f, 40f))
    drawOval(StumpTop, Offset(x - 40f, baseY - 50f), Size(80f, 20f))
    drawOval(Stump, Offset(x - 22f, baseY - 45f), Size(44f, 10f), style = Stroke(1.5f))
    val brim = baseY - 46f
    drawOval(HatGreen, Offset(x - 50f, brim - 8f), Size(100f, 18f))
    drawPath(Path().apply { moveTo(x - 32f, brim); lineTo(x - 28f, brim - 66f); lineTo(x + 28f, brim - 66f); lineTo(x + 32f, brim); close() }, HatGreen)
    drawOval(Color(0xFF4E9A5B), Offset(x - 28f, brim - 72f), Size(56f, 12f))
    drawRect(HatBand, Offset(x - 31f, brim - 20f), Size(62f, 14f))
    drawRoundRect(Gold, Offset(x - 11f, brim - 22f), Size(22f, 18f), CornerRadius(2f), style = Stroke(3.5f))
}
