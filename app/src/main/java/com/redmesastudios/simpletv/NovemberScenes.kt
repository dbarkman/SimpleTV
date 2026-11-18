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

// Día de los Muertos (November 2nd, in the evening plaza; greeting "Día de los Muertos"): an
// ofrenda of three cloth-covered steps with candles flickering, sugar skulls, pan de muerto,
// marigolds and papel picado overhead.
// First day of winter (the winter backdrop; "Hello, winter!"), which shows only when nothing
// else does (Christmas usually holds the day): a sled with a scarf, mittens on a line and a
// cardinal on a snowy branch.
// Drawn in design px (1920x1080).

private val AltarCloth = Color(0xFFE8517A)
private val AltarCloth2 = Color(0xFFA77BD0)
private val AltarCloth3 = Color(0xFFF2A93B)
private val Lace = Color(0xFFFBF6EC)
private val SkullWhite = Color(0xFFFDFCF8)
private val SkullTrim = listOf(Color(0xFFE8517A), Color(0xFF5FA8D3), Color(0xFF6DBE7B), Color(0xFFF2D24E))
private val Bread = Color(0xFFD9A05E)
private val Marigold = Color(0xFFF2A93B)
private val MarigoldDeep = Color(0xFFE8862E)

fun DrawScope.drawDiaDeLosMuertosFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawPapelPicado(seconds)
        val x = 960f
        val b = 866f
        // Three steps, widest at the bottom, each draped in a different color with a lace edge.
        listOf(Triple(380f, 40f, AltarCloth), Triple(290f, 40f, AltarCloth2), Triple(200f, 40f, AltarCloth3)).forEachIndexed { i, (w, h, c) ->
            val top = b - (i + 1) * h
            drawRect(c, Offset(x - w / 2, top), Size(w, h))
            var lx = x - w / 2
            while (lx < x + w / 2) { drawArc(Lace, 0f, 180f, useCenter = true, topLeft = Offset(lx, top + h - 6f), size = Size(16f, 12f)); lx += 16f }
        }
        // Candles along each step.
        listOf(Triple(-160f, b - 40f, 0), Triple(-120f, b - 40f, 1), Triple(120f, b - 40f, 2), Triple(160f, b - 40f, 3), Triple(-110f, b - 80f, 4), Triple(110f, b - 80f, 5), Triple(-70f, b - 120f, 6), Triple(70f, b - 120f, 7)).forEach { (dx, y, i) ->
            drawCandle(x + dx, y, seconds, i)
        }
        // Sugar skulls on the middle step, pan de muerto on top, marigolds at both ends.
        listOf(-60f, 0f, 60f).forEachIndexed { i, dx -> drawSugarSkull(Offset(x + dx, b - 104f), i) }
        drawOval(Bread, Offset(x - 40f, b - 160f), Size(80f, 44f))
        drawLine(Color(0xFFB8753E), Offset(x - 30f, b - 140f), Offset(x + 30f, b - 140f), strokeWidth = 5f, cap = StrokeCap.Round)
        drawCircle(Color(0xFFB8753E), 7f, Offset(x, b - 156f))
        listOf(-230f, 230f).forEach { dx ->
            drawRoundRect(Color(0xFF5FA8D3), Offset(x + dx - 22f, b - 60f), Size(44f, 60f), CornerRadius(8f))
            repeat(7) { k -> drawCircle(if (k % 2 == 0) Marigold else MarigoldDeep, 13f, Offset(x + dx - 24f + (k % 4) * 16f, b - 74f - (k / 4) * 18f + 2f * sin(seconds + k))) }
        }
        // Marigold petals scattered in a path toward the altar.
        repeat(18) { i -> drawCircle(if (i % 2 == 0) Marigold else MarigoldDeep, 4f, Offset(500f + i * 22f, 870f + 6f * sin(i * 1.3f))) }
    }
}

private fun DrawScope.drawSugarSkull(c: Offset, index: Int) {
    drawCircle(SkullWhite, 22f, c)
    drawRoundRect(SkullWhite, Offset(c.x - 13f, c.y + 10f), Size(26f, 16f), CornerRadius(5f))
    val trim = SkullTrim[index % SkullTrim.size]
    drawCircle(trim, 7f, Offset(c.x - 9f, c.y - 2f), style = Stroke(2.5f))
    drawCircle(trim, 7f, Offset(c.x + 9f, c.y - 2f), style = Stroke(2.5f))
    drawCircle(Color(0xFF2A211B), 3.5f, Offset(c.x - 9f, c.y - 2f))
    drawCircle(Color(0xFF2A211B), 3.5f, Offset(c.x + 9f, c.y - 2f))
    drawPath(Path().apply { moveTo(c.x - 3f, c.y + 8f); lineTo(c.x, c.y + 3f); lineTo(c.x + 3f, c.y + 8f); close() }, Color(0xFF2A211B))
    repeat(4) { k -> drawLine(Color(0xFF2A211B), Offset(c.x - 9f + k * 6f, c.y + 16f), Offset(c.x - 9f + k * 6f, c.y + 22f), strokeWidth = 1.5f) }
    repeat(5) { k ->
        val a = Math.toRadians(200.0 + k * 35.0)
        drawCircle(SkullTrim[(index + k + 1) % SkullTrim.size], 3f, Offset(c.x + 16f * cos(a).toFloat(), c.y - 4f + 16f * sin(a).toFloat()))
    }
}

// ---- First day of winter ----

private val SledWood = Color(0xFFC0503A)
private val Runner = Color(0xFF8C9AA6)
private val Scarf = Color(0xFF5E8C61)
private val MittenColors = listOf(Color(0xFFC9483F), Color(0xFF3A5A92))
private val Cardinal = Color(0xFFC9302E)
private val Branch = Color(0xFF7A5A40)

fun DrawScope.drawWinterDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        // A sled with a scarf trailing off it.
        drawPath(Path().apply { moveTo(780f, 860f); lineTo(1000f, 860f); quadraticTo(1030f, 860f, 1030f, 836f) }, Runner, style = Stroke(5f, cap = StrokeCap.Round))
        listOf(810f, 960f).forEach { x -> drawLine(Runner, Offset(x, 860f), Offset(x, 838f), strokeWidth = 4f) }
        drawRoundRect(SledWood, Offset(790f, 818f), Size(220f, 22f), CornerRadius(6f))
        drawPath(Path().apply { moveTo(860f, 818f); quadraticTo(900f, 790f, 950f, 816f) }, Scarf, style = Stroke(12f, cap = StrokeCap.Round))
        drawLine(Scarf, Offset(950f, 816f), Offset(966f + 4f * sin(seconds * 2f), 846f), strokeWidth = 10f, cap = StrokeCap.Round)
        // Mittens pegged on a line between two posts.
        listOf(1200f, 1460f).forEach { x -> drawLine(Branch, Offset(x, 866f), Offset(x, 740f), strokeWidth = 6f) }
        drawPath(Path().apply { moveTo(1200f, 748f); quadraticTo(1330f, 770f, 1460f, 748f) }, Color(0xFF9C8C7E), style = Stroke(2f))
        listOf(1280f, 1380f).forEachIndexed { i, x ->
            val sway = 5f * sin(seconds * 1.3f + i)
            withTransform({ rotate(sway, pivot = Offset(x, 760f)) }) {
                drawRoundRect(MittenColors[i], Offset(x - 16f, 762f), Size(32f, 44f), CornerRadius(14f))
                drawRoundRect(MittenColors[i], Offset(x + 12f, 772f), Size(12f, 20f), CornerRadius(6f))
                drawRect(Color(0xFFFBF6EC), Offset(x - 16f, 798f), Size(32f, 10f))
            }
        }
        // A cardinal on a snowy branch.
        drawLine(Branch, Offset(480f, 760f), Offset(680f, 740f), strokeWidth = 6f, cap = StrokeCap.Round)
        drawRoundRect(Color(0xFFFAFBFC), Offset(500f, 748f), Size(170f, 8f), CornerRadius(4f))
        val bird = Offset(600f, 724f + 2f * sin(seconds * 2f))
        drawOval(Cardinal, Offset(bird.x - 18f, bird.y - 10f), Size(36f, 26f))
        drawCircle(Cardinal, 11f, Offset(bird.x + 14f, bird.y - 10f))
        drawPath(Path().apply { moveTo(bird.x + 10f, bird.y - 20f); lineTo(bird.x + 14f, bird.y - 32f); lineTo(bird.x + 20f, bird.y - 18f); close() }, Cardinal)
        drawPath(Path().apply { moveTo(bird.x + 22f, bird.y - 12f); lineTo(bird.x + 32f, bird.y - 8f); lineTo(bird.x + 22f, bird.y - 5f); close() }, Color(0xFFF2A93B))
        drawCircle(Color(0xFF2A211B), 2f, Offset(bird.x + 17f, bird.y - 12f))
        drawPath(Path().apply { moveTo(bird.x - 16f, bird.y); lineTo(bird.x - 34f, bird.y + 10f); lineTo(bird.x - 14f, bird.y + 8f); close() }, Color(0xFFA82624))
    }
}
