package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.abs
import kotlin.math.sin

// Easter week (the week up to Easter Sunday), in a meadow at sunrise: a wicker basket of
// painted eggs, more eggs hidden in the grass, Easter lilies in pots, and now and then a
// bunny hopping across. The greeting says "Happy Easter!" on the day itself. Good Friday,
// which takes its day from the week, stays quiet: three crosses on a hill at dusk (the
// place) and a few candles, with no greeting. Drawn in design px (1920x1080).

private val EggColors = listOf(Color(0xFFF4A6B8), Color(0xFF7FB7D9), Color(0xFFF2C14E), Color(0xFF9BCB8C), Color(0xFFC99BD9), Color(0xFFE8796B))
private val EggTrim = Color(0xFFFBF6EC)
private val Wicker = Color(0xFFC9A16E)
private val WickerDark = Color(0xFFA9824F)
private val BasketGrass = Color(0xFF9BCB8C)
private val Lily = Color(0xFFFDFCF8)
private val LilyShade = Color(0xFFE6E2D8)
private val LilyLeaf = Color(0xFF6E9C6D)
private val LilyPot = Color(0xFFD9895E)
private val BunnyFur = Color(0xFFF1E9DF)
private val BunnyShade = Color(0xFFD9CFC2)
private val BunnyPink = Color(0xFFF4B8C4)

// The bunny hops across the meadow, right to left, every minute or so.
val Bunny = Walker(cycleSeconds = 64f, walkSeconds = 22f, offsetSeconds = 18f, baseY = 880f, leftToRight = false, strideSeconds = 0.7f)

fun DrawScope.drawEasterFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        listOf(Triple(330f, 878f, 0), Triple(480f, 884f, 1), Triple(1180f, 880f, 2), Triple(1330f, 886f, 3), Triple(1620f, 878f, 4)).forEach { (x, y, i) ->
            drawEgg(Offset(x, y - 22f), 32f, EggColors[i % EggColors.size], i, (i - 2) * 8f)
        }
        drawLilies(640f, 866f, seconds, 0f)
        drawLilies(1290f, 866f, seconds, 2f)
        drawBasket(960f, 866f)
        Bunny.poseAt(seconds)?.let { drawBunny(it) }
    }
}

fun DrawScope.drawGoodFridayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        listOf(700f, 740f, 1180f, 1220f).forEachIndexed { i, x -> drawCandle(x, 880f, seconds, i) }
    }
}

// An egg size wide, painted with a band of zigzag and dots, tipped degrees.
private fun DrawScope.drawEgg(center: Offset, size: Float, color: Color, pattern: Int, degrees: Float) {
    val w = size
    val h = size * 1.3f
    withTransform({ rotate(degrees, pivot = center) }) {
        val egg = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center.x - w / 2, center.y - h / 2, center.x + w / 2, center.y + h / 2)) }
        drawPath(egg, color)
        clipPath(egg) {
            if (pattern % 2 == 0) {
                drawPath(Path().apply {
                    moveTo(center.x - w / 2, center.y)
                    var x = center.x - w / 2
                    var up = true
                    while (x < center.x + w / 2) { x += w / 6; lineTo(x, center.y + if (up) -4f else 4f); up = !up }
                }, EggTrim, style = Stroke(2.5f))
            } else {
                drawRect(EggTrim, Offset(center.x - w / 2, center.y - 3f), Size(w, 6f))
            }
            listOf(-0.25f to -0.3f, 0.2f to -0.28f, 0f to 0.3f).forEach { (dx, dy) -> drawCircle(EggTrim, 2f, Offset(center.x + dx * w, center.y + dy * h)) }
        }
        drawOval(Color.White, Offset(center.x - w * 0.3f, center.y - h * 0.35f), Size(w * 0.2f, h * 0.25f), alpha = 0.35f)
    }
}

private fun DrawScope.drawBasket(x: Float, baseY: Float) {
    val top = baseY - 64f
    // The handle arching over, eggs heaped on grass, then the woven front.
    drawArc(WickerDark, 180f, 180f, useCenter = false, topLeft = Offset(x - 74f, top - 80f), size = Size(148f, 160f), style = Stroke(9f))
    drawArc(Wicker, 180f, 180f, useCenter = false, topLeft = Offset(x - 74f, top - 80f), size = Size(148f, 160f), style = Stroke(5f))
    repeat(9) { i -> drawLine(BasketGrass, Offset(x - 80f + i * 20f, top + 4f), Offset(x - 84f + i * 20f + (i % 3) * 4f, top - 14f), strokeWidth = 3f, cap = StrokeCap.Round) }
    listOf(-50f to -10f, -22f to -18f, 8f to -20f, 36f to -14f, 60f to -6f, -36f to 0f, 22f to -2f).forEachIndexed { i, (dx, dy) ->
        drawEgg(Offset(x + dx, top + dy), 26f, EggColors[(i + 2) % EggColors.size], i, (i - 3) * 9f)
    }
    drawPath(Path().apply {
        moveTo(x - 90f, top); lineTo(x + 90f, top); lineTo(x + 72f, baseY); lineTo(x - 72f, baseY); close()
    }, Wicker)
    var y = top + 8f
    while (y < baseY) { drawLine(WickerDark, Offset(x - 86f + (y - top) * 0.25f, y), Offset(x + 86f - (y - top) * 0.25f, y), strokeWidth = 2f, alpha = 0.6f); y += 12f }
    var wx = x - 80f
    while (wx < x + 80f) { drawLine(WickerDark, Offset(wx, top), Offset(wx + (x - wx) * 0.2f, baseY), strokeWidth = 1.5f, alpha = 0.5f); wx += 18f }
    drawRoundRect(WickerDark, Offset(x - 94f, top - 6f), Size(188f, 12f), CornerRadius(6f))
}

// Three Easter lilies in a pot, nodding.
private fun DrawScope.drawLilies(x: Float, baseY: Float, seconds: Float, phase: Float) {
    drawPath(Path().apply { moveTo(x - 30f, baseY - 44f); lineTo(x + 30f, baseY - 44f); lineTo(x + 22f, baseY); lineTo(x - 22f, baseY); close() }, LilyPot)
    listOf(-34f to 120f, 0f to 140f, 34f to 116f).forEachIndexed { i, (dx, h) ->
        val nod = 3f * sin(seconds * 0.9f + phase + i)
        val head = Offset(x + dx + nod, baseY - 44f - h)
        drawLine(LilyLeaf, Offset(x + dx * 0.3f, baseY - 44f), head, strokeWidth = 3f)
        drawOval(LilyLeaf, Offset(x + dx * 0.5f - 18f, baseY - 90f - i * 10f), Size(36f, 10f))
        withTransform({ rotate(if (dx < 0) -30f else if (dx > 0) 30f else 0f, pivot = head) }) {
            // A trumpet flaring into six petals.
            drawPath(Path().apply {
                moveTo(head.x - 5f, head.y); lineTo(head.x - 18f, head.y - 30f); quadraticTo(head.x, head.y - 38f, head.x + 18f, head.y - 30f); lineTo(head.x + 5f, head.y); close()
            }, Lily)
            drawPath(Path().apply { moveTo(head.x - 5f, head.y); lineTo(head.x, head.y - 30f); lineTo(head.x + 5f, head.y) }, LilyShade, style = Stroke(1.5f))
            drawCircle(Color(0xFFF2C14E), 2.2f, Offset(head.x, head.y - 30f))
        }
    }
}

// A white bunny mid-hop: up off the grass and stretched out on each bound.
private fun DrawScope.drawBunny(pose: WalkerPose) {
    val hop = abs(pose.stride)
    withTransform({
        translate(pose.x, pose.y - 34f * hop)
        scale(if (pose.leftToRight) 1.1f else -1.1f, 1.1f, pivot = Offset.Zero)
    }) {
        val stretch = 1f + 0.18f * hop
        drawOval(BunnyShade, Offset(-30f * stretch, -40f), Size(56f * stretch, 38f))
        drawOval(BunnyFur, Offset(-28f * stretch, -42f), Size(54f * stretch, 36f))
        drawCircle(Color.White, 9f, Offset(-28f * stretch, -28f))
        val head = Offset(26f * stretch, -48f)
        drawOval(BunnyFur, Offset(head.x - 6f, head.y - 50f), Size(12f, 42f))
        drawOval(BunnyPink, Offset(head.x - 3f, head.y - 44f), Size(6f, 30f))
        drawOval(BunnyFur, Offset(head.x + 6f, head.y - 46f), Size(12f, 40f))
        drawCircle(BunnyFur, 17f, head)
        drawCircle(Color(0xFF2A211B), 2.4f, Offset(head.x + 7f, head.y - 4f))
        drawCircle(BunnyPink, 2.6f, Offset(head.x + 16f, head.y + 3f))
        // Back feet pushing off.
        drawOval(BunnyShade, Offset(-20f * stretch - 12f * hop, -8f), Size(26f, 10f))
    }
}
