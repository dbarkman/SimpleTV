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

// Mother's Day weekend (Friday through the second Sunday of May) in a sunroom: a bouquet in
// a vase on the table, a card for Mom standing beside it, a steaming cup of tea, a wrapped
// gift, and flower baskets hanging in the windows. The greeting says "Happy Mother's Day!"
// on the Sunday. Drawn in design px (1920x1080).

private val Petals = listOf(Color(0xFFF4A6B8), Color(0xFFE8796B), Color(0xFFF7C6CF), Color(0xFFC99BD9), Color(0xFFF2C14E))
private val VaseGlass = Color(0xFFBFD8E2)
private val Stem = Color(0xFF6E9C6D)
private val Card = Color(0xFFFBF6EC)
private val CardEdge = Color(0xFFE2D8C8)
private val Cup = Color(0xFFFDFCF8)
private val CupShade = Color(0xFFDCD6CC)
private val Tea = Color(0xFFC98A5A)
private val Steam = Color(0xFFD6DCE2)
private val Gift = Color(0xFFC99BD9)
private val GiftRibbon = Color(0xFFF7C6CF)
private val Basket = Color(0xFFC9A16E)
private val Chain = Color(0xFF9C8C7E)
private val Vine = Color(0xFF7FA270)

private val CardText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xC2, 0x47, 0x7A)
    textSize = 30f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
}

fun DrawScope.drawMothersDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawHangingBasket(420f, seconds, 0f)
        drawHangingBasket(1500f, seconds, 1.4f)
        drawGift(670f, 866f)
        drawBouquet(830f, 866f, seconds)
        drawCard(1010f, 866f)
        drawTeacup(1150f, 866f, seconds)
    }
}

private fun DrawScope.drawBouquet(x: Float, baseY: Float, seconds: Float) {
    val top = baseY - 96f
    val blooms = listOf(-46f to 70f, -20f to 96f, 8f to 106f, 34f to 90f, 54f to 64f, -34f to 40f, 20f to 50f, -6f to 74f)
    blooms.forEachIndexed { i, (dx, up) ->
        val nod = 2f * sin(seconds * 0.8f + i)
        val head = Offset(x + dx + nod, top - up)
        drawLine(Stem, Offset(x + dx * 0.25f, top + 10f), head, strokeWidth = 2.5f)
        val color = Petals[i % Petals.size]
        repeat(6) { k -> drawCircle(color, 8f, Offset(head.x + 9f * cos(k * 1.047f), head.y + 9f * sin(k * 1.047f))) }
        drawCircle(Color(0xFFF2C14E), 5f, head)
    }
    listOf(-1f, 1f).forEach { side -> drawOval(Stem, Offset(x + side * 30f - 14f, top - 30f), Size(28f, 12f)) }
    drawPath(Path().apply {
        moveTo(x - 26f, top); lineTo(x + 26f, top); quadraticTo(x + 40f, baseY - 40f, x + 22f, baseY); lineTo(x - 22f, baseY)
        quadraticTo(x - 40f, baseY - 40f, x - 26f, top); close()
    }, VaseGlass, alpha = 0.85f)
    drawOval(Color.White, Offset(x - 18f, top + 16f), Size(8f, 40f), alpha = 0.35f)
}

private fun DrawScope.drawCard(x: Float, baseY: Float) {
    withTransform({ rotate(-4f, pivot = Offset(x, baseY)) }) {
        drawRect(CardEdge, Offset(x - 60f, baseY - 120f), Size(124f, 120f))
        drawRect(Card, Offset(x - 64f, baseY - 124f), Size(124f, 120f))
        drawHeart(Offset(x - 2f, baseY - 92f), 28f, Color(0xFFE8796B))
        drawIntoCanvas { it.nativeCanvas.drawText("Mom", x - 2f, baseY - 34f, CardText) }
    }
}

private fun DrawScope.drawTeacup(x: Float, baseY: Float, seconds: Float) {
    drawOval(CupShade, Offset(x - 50f, baseY - 12f), Size(100f, 16f))
    drawPath(Path().apply { moveTo(x - 36f, baseY - 58f); lineTo(x + 36f, baseY - 58f); quadraticTo(x + 34f, baseY - 12f, x, baseY - 12f); quadraticTo(x - 34f, baseY - 12f, x - 36f, baseY - 58f); close() }, Cup)
    drawOval(Tea, Offset(x - 32f, baseY - 64f), Size(64f, 12f))
    drawArc(Cup, -70f, 160f, useCenter = false, topLeft = Offset(x + 26f, baseY - 52f), size = Size(26f, 26f), style = Stroke(5f))
    drawLine(Color(0xFFF4A6B8), Offset(x - 30f, baseY - 40f), Offset(x + 30f, baseY - 40f), strokeWidth = 3f)
    repeat(2) { i ->
        val p = (seconds * 0.35f + i / 2f) % 1f
        val sx = x - 10f + i * 20f + 6f * sin(seconds * 1.5f + i)
        val sy = baseY - 72f - 60f * p
        drawPath(Path().apply { moveTo(sx, sy + 14f); quadraticTo(sx + 7f, sy + 7f, sx, sy); quadraticTo(sx - 7f, sy - 7f, sx, sy - 14f) },
            Steam, style = Stroke(3.5f, cap = StrokeCap.Round), alpha = 0.7f * sin(Math.PI.toFloat() * p))
    }
}

private fun DrawScope.drawGift(x: Float, baseY: Float) {
    drawRoundRect(Gift, Offset(x - 40f, baseY - 64f), Size(80f, 64f), CornerRadius(4f))
    drawRect(GiftRibbon, Offset(x - 6f, baseY - 64f), Size(12f, 64f))
    drawRect(GiftRibbon, Offset(x - 40f, baseY - 40f), Size(80f, 10f))
    drawOval(GiftRibbon, Offset(x - 24f, baseY - 78f), Size(24f, 16f))
    drawOval(GiftRibbon, Offset(x, baseY - 78f), Size(24f, 16f))
}

// A basket of flowers on chains, hanging in front of a window, swaying a little.
private fun DrawScope.drawHangingBasket(x: Float, seconds: Float, phase: Float) {
    val hook = Offset(x, 600f)
    withTransform({ rotate(2.5f * sin(seconds * 0.9f + phase), pivot = hook) }) {
        val rim = 690f
        drawLine(Chain, hook, Offset(x - 40f, rim), strokeWidth = 1.5f)
        drawLine(Chain, hook, Offset(x + 40f, rim), strokeWidth = 1.5f)
        drawLine(Chain, hook, Offset(x, rim), strokeWidth = 1.5f)
        listOf(-30f, -6f, 22f, 40f).forEachIndexed { i, dx ->
            drawPath(Path().apply { moveTo(x + dx, rim + 10f); quadraticTo(x + dx + 10f, rim + 50f, x + dx - 4f, rim + 80f + i * 8f) }, Vine, style = Stroke(3f))
        }
        listOf(-30f to -10f, -8f to -16f, 16f to -14f, 36f to -6f).forEachIndexed { i, (dx, dy) ->
            drawCircle(Petals[(i + 1) % Petals.size], 10f, Offset(x + dx, rim + dy))
        }
        drawPath(Path().apply { moveTo(x - 46f, rim); lineTo(x + 46f, rim); quadraticTo(x + 40f, rim + 40f, x, rim + 40f); quadraticTo(x - 40f, rim + 40f, x - 46f, rim); close() }, Basket)
    }
}
