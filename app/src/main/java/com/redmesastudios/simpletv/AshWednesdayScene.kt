package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.sin

// Ash Wednesday, the start of Lent: a quiet scene. A small white chapel on the hill with warm
// light in its windows and its bell swinging slowly in the steeple, a path leading to its
// door, bare shrubs and a few candles flickering on either side. No greeting of its own.
// Drawn in design px (1920x1080).

private val ChapelWall = Color(0xFFF6F1E8)
private val ChapelShade = Color(0xFFE2D9CB)
private val ChapelRoof = Color(0xFF7C6A5C)
private val Glow = Color(0xFFF7D27A)
private val Door = Color(0xFF8C6A4F)
private val Bell = Color(0xFFC9A23A)
private val CrossColor = Color(0xFF8C7A62)
private val PathStone = Color(0xFFD9D2C6)
private val Shrub = Color(0xFF8C7A6A)
private val CandleWax = Color(0xFFFBF6EC)
private val CandleFlame = Color(0xFFF7C55A)

fun DrawScope.drawAshWednesdayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawPath(Path().apply {
            moveTo(940f, 866f); lineTo(980f, 866f); lineTo(1010f, 900f); lineTo(910f, 900f); close()
        }, PathStone, alpha = 0.8f)
        drawChapel(960f, 866f, seconds)
        drawBareShrub(640f, 868f)
        drawBareShrub(1290f, 868f)
        listOf(760f, 800f, 1120f, 1160f).forEachIndexed { i, x -> drawCandle(x, 866f, seconds, i) }
    }
}

private fun DrawScope.drawChapel(x: Float, baseY: Float, seconds: Float) {
    val w = 170f
    val wallTop = baseY - 100f
    // Steeple: tower, belfry with the swinging bell, spire and cross.
    val towerTop = wallTop - 80f
    drawRect(ChapelWall, Offset(x - 26f, towerTop), Size(52f, 90f))
    drawRect(ChapelShade, Offset(x - 18f, towerTop + 10f), Size(36f, 34f))
    val swing = 14f * sin(seconds * 1.6f)
    withTransform({ rotate(swing, pivot = Offset(x, towerTop + 12f)) }) {
        drawPath(Path().apply {
            moveTo(x - 4f, towerTop + 14f); quadraticTo(x - 12f, towerTop + 20f, x - 14f, towerTop + 36f)
            lineTo(x + 14f, towerTop + 36f); quadraticTo(x + 12f, towerTop + 20f, x + 4f, towerTop + 14f); close()
        }, Bell)
        drawCircle(Bell, 3.5f, Offset(x, towerTop + 39f))
    }
    drawPath(Path().apply { moveTo(x - 32f, towerTop); lineTo(x, towerTop - 56f); lineTo(x + 32f, towerTop); close() }, ChapelRoof)
    drawLine(CrossColor, Offset(x, towerTop - 56f), Offset(x, towerTop - 86f), strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(CrossColor, Offset(x - 10f, towerTop - 76f), Offset(x + 10f, towerTop - 76f), strokeWidth = 4f, cap = StrokeCap.Round)
    // The nave.
    drawRect(ChapelWall, Offset(x - w / 2, wallTop), Size(w, 100f))
    drawPath(Path().apply { moveTo(x - w / 2 - 14f, wallTop + 4f); lineTo(x, wallTop - 46f); lineTo(x + w / 2 + 14f, wallTop + 4f); close() }, ChapelRoof)
    drawRect(ChapelShade, Offset(x - w / 2, baseY - 8f), Size(w, 8f))
    // Arched door and two arched windows, lit from inside.
    drawRoundRect(Door, Offset(x - 16f, baseY - 52f), Size(32f, 52f), CornerRadius(16f, 16f))
    val light = 0.75f + 0.25f * sin(seconds * 0.8f)
    listOf(x - 54f, x + 38f).forEach { wx ->
        drawRoundRect(Glow, Offset(wx, wallTop + 26f), Size(16f, 34f), CornerRadius(8f, 8f), alpha = light)
        drawCircle(Glow, 26f, Offset(wx + 8f, wallTop + 44f), alpha = 0.12f * light)
    }
    drawCircle(Glow, 7f, Offset(x, wallTop - 14f), alpha = light)
}

private fun DrawScope.drawBareShrub(x: Float, baseY: Float) {
    listOf(-40f, -18f, 0f, 20f, 42f).forEachIndexed { i, lean ->
        val tip = Offset(x + lean, baseY - 50f - (i % 2) * 14f)
        drawLine(Shrub, Offset(x, baseY), tip, strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(Shrub, Offset(x + lean * 0.6f, baseY - 30f), Offset(tip.x + lean * 0.3f + 6f, tip.y + 10f), strokeWidth = 2f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawCandle(x: Float, baseY: Float, seconds: Float, index: Int) {
    val height = 34f + (index % 2) * 10f
    drawRoundRect(CandleWax, Offset(x - 6f, baseY - height), Size(12f, height), CornerRadius(3f))
    val glow = candleGlow(seconds, index * 1.9f)
    val flick = 1.2f * sin(seconds * 8f + index)
    drawCircle(CandleFlame, 14f * glow, Offset(x, baseY - height - 8f), alpha = 0.2f)
    drawOval(CandleFlame, Offset(x - 4f + flick * 0.3f, baseY - height - 17f), Size(8f, 14f * glow))
    drawOval(Color(0xFFFFF3D6), Offset(x - 1.8f + flick * 0.3f, baseY - height - 10f), Size(3.6f, 6f))
}
