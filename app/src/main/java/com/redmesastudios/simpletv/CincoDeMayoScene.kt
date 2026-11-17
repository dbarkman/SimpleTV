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
import kotlin.math.cos
import kotlin.math.sin

// Cinco de Mayo (May 5th) in a plaza of adobe arches between saguaros: papel picado banners
// strung across, a star piñata swinging from the middle, maracas shaking, a guitar leaning on
// a crate and pots of tissue-paper flowers. The greeting says "Happy Cinco de Mayo!". Drawn
// in design px (1920x1080).

private val Picado = listOf(Color(0xFFE8517A), Color(0xFFF2A93B), Color(0xFFF2D24E), Color(0xFF6DBE7B), Color(0xFF5FA8D3), Color(0xFFA77BD0))
private val Twine = Color(0xFF8A7B6A)
private val Cone = listOf(Color(0xFFE8517A), Color(0xFFF2D24E), Color(0xFF5FA8D3), Color(0xFF6DBE7B), Color(0xFFF2A93B))
private val Maraca = listOf(Color(0xFFE8517A), Color(0xFF5FA8D3))
private val MaracaHandle = Color(0xFF9C7A55)
private val GuitarBody = Color(0xFFD9A05E)
private val GuitarDark = Color(0xFF8C5A3A)
private val Crate = Color(0xFFB98A5E)
private val ClayPot = Color(0xFFC9784E)

private const val PicadoY = 648f
private const val PicadoDip = 24f
private val PicadoAnchors = listOf(200f, 580f, 960f, 1340f, 1720f)

fun DrawScope.drawCincoDeMayoFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawPapelPicado(seconds)
        drawPinata(Offset(960f, PicadoY), seconds)
        drawMaracas(600f, 866f, seconds)
        drawGuitar(1330f, 866f)
        drawFlowerPot(760f, 866f, 0)
        drawFlowerPot(1560f, 866f, 1)
    }
}

// Rows of cut-paper flags, each with a scalloped hem and little cut-outs, swaying.
private fun DrawScope.drawPapelPicado(seconds: Float) {
    var count = 0
    PicadoAnchors.zipWithNext().forEach { (from, to) ->
        drawPath(Path().apply { moveTo(from, PicadoY); quadraticTo((from + to) / 2, PicadoY + 2f * PicadoDip, to, PicadoY) }, Twine, style = Stroke(2f))
        val flags = 6
        repeat(flags) { i ->
            val t = (i + 0.5f) / flags
            val top = Offset(from + (to - from) * t, PicadoY + 4f * PicadoDip * t * (1f - t))
            if (kotlin.math.abs(top.x - 960f) < 40f) return@repeat
            val sway = 4f * sin(seconds * 1.4f + count * 0.7f)
            withTransform({ rotate(sway, pivot = top) }) {
                val w = 46f
                val h = 56f
                val color = Picado[count % Picado.size]
                drawPath(Path().apply {
                    moveTo(top.x - w / 2, top.y); lineTo(top.x + w / 2, top.y); lineTo(top.x + w / 2, top.y + h)
                    var x = top.x + w / 2
                    while (x > top.x - w / 2 + 1f) { quadraticTo(x - w / 8, top.y + h + 7f, x - w / 4, top.y + h); x -= w / 4 }
                    close()
                }, color, alpha = 0.92f)
                // Cut-outs: a little flower and a row of diamonds.
                val c = Offset(top.x, top.y + h * 0.42f)
                repeat(6) { k -> drawCircle(Color.White, 2.6f, Offset(c.x + 8f * cos(k * 1.047f), c.y + 8f * sin(k * 1.047f)), alpha = 0.7f) }
                drawCircle(Color.White, 3f, c, alpha = 0.7f)
                listOf(-14f, 0f, 14f).forEach { dx ->
                    drawPath(Path().apply { moveTo(top.x + dx, top.y + 6f); lineTo(top.x + dx + 4f, top.y + 10f); lineTo(top.x + dx, top.y + 14f); lineTo(top.x + dx - 4f, top.y + 10f); close() }, Color.White, alpha = 0.7f)
                }
            }
            count++
        }
    }
}

// A five-pointed star piñata on a rope, swinging slowly, with streamers off its points.
private fun DrawScope.drawPinata(hang: Offset, seconds: Float) {
    val swing = 10f * sin(seconds * 1.1f)
    withTransform({ rotate(swing, pivot = hang) }) {
        val center = Offset(hang.x, hang.y + 104f)
        drawLine(Twine, hang, Offset(center.x, center.y - 32f), strokeWidth = 2.5f)
        repeat(5) { k ->
            val a = Math.toRadians(-90.0 + k * 72.0)
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            val side = Offset(-dir.y, dir.x)
            val base = Offset(center.x + dir.x * 26f, center.y + dir.y * 26f)
            val tip = Offset(center.x + dir.x * 64f, center.y + dir.y * 64f)
            drawPath(Path().apply {
                moveTo(base.x + side.x * 14f, base.y + side.y * 14f); lineTo(tip.x, tip.y); lineTo(base.x - side.x * 14f, base.y - side.y * 14f); close()
            }, Cone[k % Cone.size])
            val flutter = 4f * sin(seconds * 3f + k)
            drawLine(Cone[(k + 2) % Cone.size], tip, Offset(tip.x + dir.x * 16f + flutter, tip.y + dir.y * 16f + 10f), strokeWidth = 3f, cap = StrokeCap.Round)
        }
        drawCircle(Color(0xFFFBF2EA), 30f, center)
        repeat(4) { r -> drawCircle(Cone[r], 30f - r * 6f, center, style = Stroke(3f)) }
    }
}

private fun DrawScope.drawMaracas(x: Float, baseY: Float, seconds: Float) {
    listOf(-1f, 1f).forEachIndexed { i, side ->
        val pivot = Offset(x + side * 22f, baseY - 6f)
        val shake = 12f * sin(seconds * 9f + i * 3f) * (if (sin(seconds * 0.8f) > 0f) 1f else 0.15f)
        withTransform({ rotate(side * 22f + shake, pivot = pivot) }) {
            drawRoundRect(MaracaHandle, Offset(pivot.x - 4f, pivot.y - 54f), Size(8f, 54f), CornerRadius(4f))
            drawOval(Maraca[i], Offset(pivot.x - 22f, pivot.y - 104f), Size(44f, 54f))
            drawLine(Color(0xFFF2D24E), Offset(pivot.x - 20f, pivot.y - 78f), Offset(pivot.x + 20f, pivot.y - 78f), strokeWidth = 4f)
            drawLine(Color(0xFF6DBE7B), Offset(pivot.x - 17f, pivot.y - 90f), Offset(pivot.x + 17f, pivot.y - 90f), strokeWidth = 3f)
        }
    }
}

// A guitar leaning against a small crate.
private fun DrawScope.drawGuitar(x: Float, baseY: Float) {
    drawRect(Crate, Offset(x + 30f, baseY - 60f), Size(90f, 60f))
    drawLine(Color(0xFF8C6A4F), Offset(x + 30f, baseY - 30f), Offset(x + 120f, baseY - 30f), strokeWidth = 2f)
    withTransform({ rotate(14f, pivot = Offset(x, baseY)) }) {
        drawRoundRect(GuitarDark, Offset(x - 5f, baseY - 200f), Size(10f, 110f), CornerRadius(3f))
        drawRoundRect(GuitarDark, Offset(x - 10f, baseY - 216f), Size(20f, 22f), CornerRadius(4f))
        drawCircle(GuitarBody, 34f, Offset(x, baseY - 40f))
        drawCircle(GuitarBody, 26f, Offset(x, baseY - 86f))
        drawRect(GuitarBody, Offset(x - 20f, baseY - 80f), Size(40f, 30f))
        drawCircle(GuitarDark, 11f, Offset(x, baseY - 66f))
        drawRect(GuitarDark, Offset(x - 14f, baseY - 30f), Size(28f, 5f))
        repeat(4) { k -> drawLine(Color(0xFFFBF2EA), Offset(x - 3f + k * 2f, baseY - 200f), Offset(x - 3f + k * 2f, baseY - 30f), strokeWidth = 0.8f) }
    }
}

// A clay pot of big tissue-paper flowers.
private fun DrawScope.drawFlowerPot(x: Float, baseY: Float, set: Int) {
    listOf(-26f to 70f, 0f to 94f, 26f to 76f).forEachIndexed { i, (dx, h) ->
        val head = Offset(x + dx, baseY - 40f - h)
        drawLine(Color(0xFF6E9C6D), Offset(x + dx * 0.4f, baseY - 40f), head, strokeWidth = 3f)
        val color = Picado[(set * 3 + i) % Picado.size]
        repeat(8) { k ->
            val a = k * 0.785f
            drawCircle(color, 10f, Offset(head.x + 11f * cos(a), head.y + 11f * sin(a)), alpha = 0.9f)
        }
        drawCircle(Color(0xFFFBF2EA), 6f, head, alpha = 0.8f)
    }
    drawPath(Path().apply { moveTo(x - 34f, baseY - 44f); lineTo(x + 34f, baseY - 44f); lineTo(x + 24f, baseY); lineTo(x - 24f, baseY); close() }, ClayPot)
    drawRect(Color(0xFFB0643E), Offset(x - 36f, baseY - 48f), Size(72f, 10f))
}
