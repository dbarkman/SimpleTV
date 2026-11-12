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

// Presidents' Day (the third Monday of February) on the winter scene: a log cabin with smoke
// curling from its chimney, a cherry tree in bloom, a tall waving flag in the middle with
// small flags along the hill, a tricorn hat and a stovepipe hat resting on a bench, and gold
// stars twinkling. The greeting says "Happy Presidents' Day". Drawn in design px (1920x1080).

private val Log = Color(0xFF9C7350)
private val LogDark = Color(0xFF7A5A40)
private val Roof = Color(0xFF6E5440)
private val Chimney = Color(0xFFA0645A)
private val WindowGlow = Color(0xFFF7D27A)
private val Smoke = Color(0xFFC8CDD3)
private val CherryBranch = Color(0xFF6E5440)
private val CherryBlossom = listOf(Color(0xFFF4C3CF), Color(0xFFF7D6DD), Color(0xFFEBA9BA))
private val Cherry = Color(0xFFC0303A)
private val Bench = Color(0xFF8C6A4F)
private val HatBlack = Color(0xFF2A211B)
private val HatBand = Color(0xFF4A3F38)
private val SnowTop = Color(0xFFFAFBFC)

private val PresidentsTwinkles = listOf(240f to 760f, 680f to 742f, 1240f to 744f, 1720f to 760f)
private val SmallFlags = listOf(820f, 870f, 1050f, 1100f)

fun DrawScope.drawPresidentsDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        PresidentsTwinkles.forEachIndexed { i, (x, y) -> drawTwinkle(x, y, seconds, i) }
        drawLogCabin(420f, 868f, seconds)
        drawCherryTree(1500f, 868f, seconds)
        SmallFlags.forEachIndexed { i, x -> drawHandFlag(x, 866f, seconds, i * 1.1f) }
        drawWavingFlag(960f, 866f, 660f, 120f, seconds)
        drawHatBench(1230f, 868f)
    }
}

private fun DrawScope.drawLogCabin(x: Float, baseY: Float, seconds: Float) {
    val w = 200f
    val wallTop = baseY - 92f
    // Smoke puffs drifting up from the chimney, each fading as it rises.
    repeat(4) { i ->
        val p = ((seconds * 0.25f + i * 0.25f) % 1f)
        drawCircle(Smoke, 9f + 12f * p, Offset(x + 54f + 22f * p + 6f * sin(seconds + i), wallTop - 92f - 70f * p), alpha = 0.5f * (1f - p))
    }
    drawRect(Chimney, Offset(x + 42f, wallTop - 80f), Size(24f, 60f))
    // Log walls: stacked rounded logs.
    val logs = 6
    repeat(logs) { i ->
        val y = wallTop + i * (92f / logs)
        drawRoundRect(if (i % 2 == 0) Log else LogDark, Offset(x - w / 2 - 6f, y), Size(w + 12f, 92f / logs + 1f), CornerRadius(8f))
    }
    // Roof with snow on it.
    drawPath(Path().apply {
        moveTo(x - w / 2 - 20f, wallTop + 4f); lineTo(x, wallTop - 66f); lineTo(x + w / 2 + 20f, wallTop + 4f); close()
    }, Roof)
    drawPath(Path().apply {
        moveTo(x - w / 2 - 20f, wallTop + 4f); lineTo(x, wallTop - 66f); lineTo(x + w / 2 + 20f, wallTop + 4f)
        lineTo(x + w / 2 + 6f, wallTop - 4f); lineTo(x, wallTop - 56f); lineTo(x - w / 2 - 6f, wallTop - 4f); close()
    }, SnowTop)
    // Door and a warmly lit window.
    drawRoundRect(LogDark, Offset(x - 22f, baseY - 54f), Size(36f, 54f), CornerRadius(4f))
    drawCircle(WindowGlow, 2.5f, Offset(x + 8f, baseY - 26f))
    val glow = 0.8f + 0.2f * sin(seconds * 1.7f)
    drawRect(WindowGlow, Offset(x + 40f, wallTop + 26f), Size(36f, 30f), alpha = glow)
    drawLine(LogDark, Offset(x + 58f, wallTop + 26f), Offset(x + 58f, wallTop + 56f), strokeWidth = 3f)
    drawLine(LogDark, Offset(x + 40f, wallTop + 41f), Offset(x + 76f, wallTop + 41f), strokeWidth = 3f)
}

// A small cherry tree in blossom with a few cherries, its branches swaying a little.
private fun DrawScope.drawCherryTree(x: Float, baseY: Float, seconds: Float) {
    val sway = 2f * sin(seconds * 0.7f)
    drawLine(CherryBranch, Offset(x, baseY), Offset(x, baseY - 90f), strokeWidth = 14f, cap = StrokeCap.Round)
    withTransform({ rotate(sway, pivot = Offset(x, baseY - 80f)) }) {
        val forks = listOf(Offset(x - 70f, baseY - 160f), Offset(x + 8f, baseY - 190f), Offset(x + 76f, baseY - 150f))
        forks.forEach { drawLine(CherryBranch, Offset(x, baseY - 86f), it, strokeWidth = 7f, cap = StrokeCap.Round) }
        val clumps = listOf(
            Offset(x - 80f, baseY - 160f), Offset(x - 40f, baseY - 186f), Offset(x + 6f, baseY - 200f), Offset(x + 50f, baseY - 180f),
            Offset(x + 86f, baseY - 150f), Offset(x - 50f, baseY - 140f), Offset(x + 30f, baseY - 150f), Offset(x - 8f, baseY - 166f),
        )
        clumps.forEachIndexed { i, at -> drawCircle(CherryBlossom[i % CherryBlossom.size], 30f, at) }
        listOf(Offset(x - 56f, baseY - 128f), Offset(x + 40f, baseY - 132f), Offset(x + 70f, baseY - 128f)).forEach { cherry ->
            drawLine(CherryBranch, Offset(cherry.x - 2f, cherry.y - 14f), cherry, strokeWidth = 1.5f)
            drawCircle(Cherry, 6f, cherry)
        }
    }
}

// A little bench with a tricorn hat and a stovepipe hat resting on it.
private fun DrawScope.drawHatBench(x: Float, baseY: Float) {
    val seat = baseY - 34f
    drawRoundRect(Bench, Offset(x - 80f, seat), Size(160f, 10f), CornerRadius(3f))
    drawRect(Bench, Offset(x - 70f, seat + 8f), Size(8f, baseY - seat - 8f))
    drawRect(Bench, Offset(x + 62f, seat + 8f), Size(8f, baseY - seat - 8f))
    // Tricorn: a low crown with three upturned brims.
    val t = Offset(x - 38f, seat)
    drawOval(HatBlack, Offset(t.x - 24f, t.y - 26f), Size(48f, 28f))
    drawPath(Path().apply {
        moveTo(t.x - 40f, t.y - 4f); quadraticTo(t.x - 30f, t.y - 30f, t.x - 10f, t.y - 18f)
        lineTo(t.x + 10f, t.y - 18f); quadraticTo(t.x + 30f, t.y - 30f, t.x + 40f, t.y - 4f); close()
    }, HatBlack)
    drawCircle(Color(0xFFE9C14A), 3.5f, Offset(t.x + 24f, t.y - 14f))
    // Stovepipe: a tall crown on a narrow brim.
    val s = Offset(x + 34f, seat)
    drawRoundRect(HatBlack, Offset(s.x - 30f, s.y - 6f), Size(60f, 7f), CornerRadius(3f))
    drawRoundRect(HatBlack, Offset(s.x - 19f, s.y - 76f), Size(38f, 72f), CornerRadius(4f))
    drawRect(HatBand, Offset(s.x - 19f, s.y - 20f), Size(38f, 9f))
}
