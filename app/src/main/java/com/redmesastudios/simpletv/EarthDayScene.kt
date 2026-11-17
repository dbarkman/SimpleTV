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

// Earth Day (April 22nd) in a green valley with wind turbines turning on the far hills: a
// sapling just planted in the middle, a watering can tipping over it with drops falling, two
// young staked trees, and a recycling bin. The greeting says "Happy Earth Day!". Drawn in
// design px (1920x1080).

private val Soil = Color(0xFF9C7A55)
private val SoilDark = Color(0xFF7A5A40)
private val Leaf = listOf(Color(0xFF6DBE7B), Color(0xFF5FAE69), Color(0xFF86C98A))
private val Stem = Color(0xFF8C6A4F)
private val CanGreen = Color(0xFF5E8C61)
private val CanDark = Color(0xFF4A7350)
private val Drop = Color(0xFF7FB7D9)
private val BinBlue = Color(0xFF3A6EA8)
private val BinDark = Color(0xFF2C5684)
private val Stake = Color(0xFFB98A5E)

fun DrawScope.drawEarthDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawRecyclingBin(620f, 866f)
        drawYoungTree(1340f, 868f, seconds, 0f)
        drawYoungTree(1500f, 868f, seconds, 1.7f)
        drawSapling(960f, 868f, seconds)
        drawWateringCan(1050f, seconds)
    }
}

private fun DrawScope.drawSapling(x: Float, baseY: Float, seconds: Float) {
    drawOval(SoilDark, Offset(x - 70f, baseY - 16f), Size(140f, 26f))
    drawOval(Soil, Offset(x - 64f, baseY - 20f), Size(128f, 22f))
    val sway = 2f * sin(seconds * 1.1f)
    withTransform({ rotate(sway, pivot = Offset(x, baseY - 10f)) }) {
        drawLine(Stem, Offset(x, baseY - 10f), Offset(x, baseY - 110f), strokeWidth = 5f, cap = StrokeCap.Round)
        listOf(Triple(-1f, 40f, 0), Triple(1f, 60f, 1), Triple(-1f, 82f, 2), Triple(1f, 100f, 0)).forEach { (side, up, c) ->
            val at = Offset(x, baseY - 10f - up)
            withTransform({ rotate(side * 50f, pivot = at) }) {
                drawOval(Leaf[c], Offset(at.x - 9f, at.y - 34f), Size(18f, 34f))
            }
        }
        drawOval(Leaf[2], Offset(x - 9f, baseY - 146f), Size(18f, 36f))
    }
}

// A watering can held tipped over the sapling, a stream of drops falling from its spout.
private fun DrawScope.drawWateringCan(x: Float, seconds: Float) {
    val tip = Offset(x - 66f, 726f)
    withTransform({ rotate(-24f, pivot = Offset(x, 740f)) }) {
        drawRoundRect(CanGreen, Offset(x - 30f, 710f), Size(76f, 60f), CornerRadius(10f))
        drawRoundRect(CanDark, Offset(x - 34f, 706f), Size(84f, 10f), CornerRadius(5f))
        drawArc(CanDark, 200f, 140f, useCenter = false, topLeft = Offset(x - 14f, 680f), size = Size(60f, 60f), style = Stroke(6f))
        drawLine(CanGreen, Offset(x - 28f, 750f), Offset(x - 74f, 712f), strokeWidth = 9f, cap = StrokeCap.Round)
        drawRoundRect(CanDark, Offset(x - 84f, 702f), Size(16f, 16f), CornerRadius(4f))
    }
    repeat(6) { i ->
        val p = (seconds * 1.4f + i / 6f) % 1f
        val at = Offset(tip.x - 14f * p + (i % 3 - 1) * 5f, tip.y + 120f * p)
        drawOval(Drop, Offset(at.x - 3f, at.y - 5f), Size(6f, 10f), alpha = 0.9f * (1f - p * 0.6f))
    }
}

// A young tree tied to a stake, its small crown stirring.
private fun DrawScope.drawYoungTree(x: Float, baseY: Float, seconds: Float, phase: Float) {
    drawLine(Stake, Offset(x + 14f, baseY), Offset(x + 14f, baseY - 120f), strokeWidth = 5f)
    drawLine(Stem, Offset(x, baseY), Offset(x, baseY - 130f), strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(Stake, Offset(x, baseY - 80f), Offset(x + 14f, baseY - 80f), strokeWidth = 3f)
    val sway = 3f * sin(seconds * 1f + phase)
    listOf(Offset(0f, -150f) to 34f, Offset(-24f, -136f) to 24f, Offset(24f, -138f) to 26f).forEachIndexed { i, (d, r) ->
        drawCircle(Leaf[i], r, Offset(x + d.x + sway, baseY + d.y))
    }
}

// A blue bin with the three chasing arrows of recycling on its front.
private fun DrawScope.drawRecyclingBin(x: Float, baseY: Float) {
    drawPath(Path().apply { moveTo(x - 44f, baseY - 110f); lineTo(x + 44f, baseY - 110f); lineTo(x + 36f, baseY); lineTo(x - 36f, baseY); close() }, BinBlue)
    drawRoundRect(BinDark, Offset(x - 50f, baseY - 122f), Size(100f, 16f), CornerRadius(5f))
    val c = Offset(x, baseY - 56f)
    repeat(3) { k ->
        withTransform({ rotate(k * 120f, pivot = c) }) {
            drawLine(Color.White, Offset(c.x - 16f, c.y + 12f), Offset(c.x + 12f, c.y + 12f), strokeWidth = 5f, cap = StrokeCap.Round)
            drawPath(Path().apply { moveTo(c.x + 10f, c.y + 4f); lineTo(c.x + 22f, c.y + 12f); lineTo(c.x + 10f, c.y + 20f); close() }, Color.White)
        }
    }
}
