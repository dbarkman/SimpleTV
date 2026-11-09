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

// Christmas on the winter scene (it brings the snow even before winter starts): a string of
// twinkling lights along the top, a decorated tree with a glowing star and presents under it,
// a snowman waving a stick arm, and now and then Santa's sleigh crossing the sky behind his
// reindeer. Drawn in design px (1920x1080).

private val Wire = Color(0xFF5A6B5C)
private val BulbColors = listOf(Color(0xFFD9534F), Color(0xFFF2C14E), Color(0xFF5FA8D3), Color(0xFF6DBE7B), Color(0xFFE48A3E))
private val TreeGreen = listOf(Color(0xFF4F7A53), Color(0xFF5E8C61), Color(0xFF6E9C6D))
private val TreeTrunk = Color(0xFF7A5A40)
private val Star = Color(0xFFF2C14E)
private val Garland = Color(0xFFE9D7A6)
private val Snow = Color(0xFFFDFDFD)
private val SnowShade = Color(0xFFDDE4EA)
private val Coal = Color(0xFF2A211B)
private val Carrot = Color(0xFFE48A3E)
private val Scarf = Color(0xFFC0503A)
private val Stick = Color(0xFF6E5440)
private val SleighRed = Color(0xFFB83A33)
private val SleighGold = Color(0xFFE2B66E)
private val Reindeer = Color(0xFF8A5A3B)
private val SantaRed = Color(0xFFC8423A)
private val Beard = Color(0xFFFBF7F2)
private val Skin = Color(0xFFF1C9A5)
private val PresentColors = listOf(Color(0xFFC0503A), Color(0xFF5E8C61), Color(0xFF5FA8D3), Color(0xFFF2C14E))
private val Ribbon = Color(0xFFFBF1E4)

private const val LightsY = -8f
private const val LightsDip = 30f
private val LightAnchors = listOf(0f, 480f, 960f, 1440f, 1920f)

// Santa crosses high over the apps, once a minute, left to right.
val Sleigh = Bat(cycleSeconds = 60f, flightSeconds = 16f, offsetSeconds = 8f, y = 104f, size = 80f, leftToRight = true)

fun DrawScope.drawChristmasFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawStringLights(seconds)
        Sleigh.poseAt(seconds)?.let { drawSleigh(it, seconds) }
        // A little under full size, so the star and the hat clear the apps above.
        withTransform({ scale(0.9f, 0.9f, pivot = Offset(420f, 872f)) }) { drawSnowman(420f, 872f, seconds) }
        withTransform({ scale(0.9f, 0.9f, pivot = Offset(1490f, 872f)) }) { drawChristmasTree(1490f, 872f, seconds) }
        drawPresent(1380f, 874f, 56f, 44f, 0)
        drawPresent(1594f, 874f, 48f, 56f, 1)
        drawPresent(1640f, 874f, 40f, 32f, 2)
        drawPresent(1336f, 874f, 36f, 30f, 3)
    }
}

// Each bulb twinkles on its own rhythm, never fully dark.
fun twinkle(seconds: Float, index: Int): Float =
    (0.62f + 0.38f * sin(seconds * (1.3f + (index % 4) * 0.35f) + index * 1.7f)).coerceIn(0.25f, 1f)

private fun DrawScope.drawStringLights(seconds: Float) {
    var count = 0
    LightAnchors.zipWithNext().forEach { (from, to) ->
        fun pointAt(t: Float) = Offset(from + (to - from) * t, LightsY + 4f * LightsDip * t * (1f - t))
        drawPath(Path().apply {
            moveTo(from, LightsY)
            quadraticTo((from + to) / 2, LightsY + 2f * LightsDip, to, LightsY)
        }, Wire, style = Stroke(2.2f))
        val bulbs = 10
        repeat(bulbs) { i ->
            val t = (i + 0.5f) / bulbs
            val socket = pointAt(t)
            val color = BulbColors[count % BulbColors.size]
            val glow = twinkle(seconds, count)
            drawCircle(color, 15f, Offset(socket.x, socket.y + 12f), alpha = 0.22f * glow)
            drawRoundRect(Wire, Offset(socket.x - 3f, socket.y - 1f), Size(6f, 6f), CornerRadius(1.5f))
            drawOval(color, Offset(socket.x - 5.5f, socket.y + 4f), Size(11f, 16f), alpha = 0.55f + 0.45f * glow)
            drawOval(Color.White, Offset(socket.x - 2.5f, socket.y + 6f), Size(3f, 5f), alpha = 0.5f * glow)
            count++
        }
    }
}

private fun DrawScope.drawChristmasTree(x: Float, baseY: Float, seconds: Float) {
    drawRoundRect(TreeTrunk, Offset(x - 12f, baseY - 30f), Size(24f, 30f), CornerRadius(4f))
    // Three tiers, widest at the bottom.
    val tiers = listOf(Triple(baseY - 26f, 150f, 92f), Triple(baseY - 86f, 120f, 84f), Triple(baseY - 140f, 88f, 80f))
    tiers.forEachIndexed { i, (bottom, width, height) ->
        drawPath(Path().apply {
            moveTo(x - width / 2, bottom)
            quadraticTo(x - width * 0.18f, bottom - height * 0.45f, x, bottom - height)
            quadraticTo(x + width * 0.18f, bottom - height * 0.45f, x + width / 2, bottom)
            quadraticTo(x, bottom + 8f, x - width / 2, bottom)
            close()
        }, TreeGreen[i % TreeGreen.size])
        // A garland swag across each tier.
        drawPath(Path().apply {
            moveTo(x - width * 0.38f, bottom - height * 0.3f)
            quadraticTo(x, bottom - height * 0.12f, x + width * 0.36f, bottom - height * 0.42f)
        }, Garland, style = Stroke(2.5f, cap = StrokeCap.Round), alpha = 0.85f)
    }
    // Ornaments and lights.
    val ornaments = listOf(
        Offset(x - 46f, baseY - 44f), Offset(x + 18f, baseY - 50f), Offset(x + 52f, baseY - 36f), Offset(x - 12f, baseY - 74f),
        Offset(x - 34f, baseY - 104f), Offset(x + 30f, baseY - 112f), Offset(x - 4f, baseY - 150f), Offset(x + 18f, baseY - 172f),
        Offset(x - 22f, baseY - 168f),
    )
    ornaments.forEachIndexed { i, at ->
        val color = BulbColors[(i * 2) % BulbColors.size]
        if (i % 3 == 0) {
            drawCircle(color, 7.5f, at)
            drawCircle(Color.White, 2.2f, Offset(at.x - 2.5f, at.y - 2.5f), alpha = 0.6f)
        } else {
            val glow = twinkle(seconds, i + 40)
            drawCircle(color, 8f, at, alpha = 0.25f * glow)
            drawCircle(color, 3.6f, at, alpha = 0.5f + 0.5f * glow)
        }
    }
    // The star, glowing.
    val top = Offset(x, baseY - 228f)
    val glow = 0.75f + 0.25f * sin(seconds * 2.2f)
    drawCircle(Star, 26f, top, alpha = 0.18f * glow)
    drawPath(starPath(top, 17f, 7.5f), Star)
}

private fun starPath(center: Offset, outer: Float, inner: Float) = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val angle = Math.toRadians(-90.0 + i * 36.0)
        val point = Offset(center.x + r * kotlin.math.cos(angle).toFloat(), center.y + r * kotlin.math.sin(angle).toFloat())
        if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
    close()
}

private fun DrawScope.drawPresent(x: Float, baseY: Float, width: Float, height: Float, colorIndex: Int) {
    val box = PresentColors[colorIndex % PresentColors.size]
    drawRoundRect(box, Offset(x - width / 2, baseY - height), Size(width, height), CornerRadius(3f))
    drawRect(Ribbon, Offset(x - 4f, baseY - height), Size(8f, height), alpha = 0.9f)
    drawRect(Ribbon, Offset(x - width / 2, baseY - height * 0.62f), Size(width, 7f), alpha = 0.9f)
    drawOval(Ribbon, Offset(x - 16f, baseY - height - 9f), Size(16f, 10f))
    drawOval(Ribbon, Offset(x, baseY - height - 9f), Size(16f, 10f))
}

private fun DrawScope.drawSnowman(x: Float, baseY: Float, seconds: Float) {
    drawOval(SnowShade, Offset(x - 62f, baseY - 10f), Size(124f, 16f), alpha = 0.7f)
    // Stick arms behind the middle ball; the right one waves.
    drawLine(Stick, Offset(x - 26f, baseY - 104f), Offset(x - 78f, baseY - 128f), strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(Stick, Offset(x - 62f, baseY - 121f), Offset(x - 70f, baseY - 138f), strokeWidth = 3f, cap = StrokeCap.Round)
    val wave = 14f * sin(seconds * 2.4f)
    val hand = Offset(x + 76f, baseY - 140f - wave)
    drawLine(Stick, Offset(x + 26f, baseY - 104f), hand, strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(Stick, Offset(x + 26f + (hand.x - x - 26f) * 0.7f, baseY - 104f + (hand.y - baseY + 104f) * 0.7f), Offset(hand.x + 4f, hand.y - 14f), strokeWidth = 3f, cap = StrokeCap.Round)
    // Three balls, each with a little shading underneath.
    listOf(Triple(baseY - 46f, 50f, 0f), Triple(baseY - 112f, 38f, 0f), Triple(baseY - 164f, 28f, 0f)).forEach { (cy, r, _) ->
        drawCircle(Snow, r, Offset(x, cy))
        drawArc(SnowShade, 20f, 140f, useCenter = false, topLeft = Offset(x - r, cy - r), size = Size(2 * r, 2 * r), style = Stroke(5f), alpha = 0.8f)
    }
    // Buttons, face, carrot nose.
    listOf(baseY - 124f, baseY - 108f, baseY - 92f).forEach { drawCircle(Coal, 3.6f, Offset(x, it)) }
    drawCircle(Coal, 3.4f, Offset(x - 10f, baseY - 172f))
    drawCircle(Coal, 3.4f, Offset(x + 10f, baseY - 172f))
    drawPath(Path().apply { moveTo(x, baseY - 166f); lineTo(x + 26f, baseY - 161f); lineTo(x, baseY - 159f); close() }, Carrot)
    listOf(-12f, -6f, 0f, 6f, 12f).forEachIndexed { i, dx -> drawCircle(Coal, 2f, Offset(x + dx, baseY - 152f + if (i == 0 || i == 4) -3f else 0f)) }
    // Scarf, its tail swaying.
    drawRoundRect(Scarf, Offset(x - 30f, baseY - 144f), Size(60f, 11f), CornerRadius(5f))
    val sway = 4f * sin(seconds * 1.7f)
    drawRoundRect(Scarf, Offset(x + 10f + sway * 0.3f, baseY - 138f), Size(12f, 34f), CornerRadius(4f))
    // Top hat.
    drawRoundRect(Coal, Offset(x - 32f, baseY - 194f), Size(64f, 8f), CornerRadius(3f))
    drawRoundRect(Coal, Offset(x - 21f, baseY - 236f), Size(42f, 44f), CornerRadius(4f))
    drawRect(Scarf, Offset(x - 21f, baseY - 204f), Size(42f, 7f))
}

// Santa in his sleigh behind two reindeer, all on the move; flap gallops their legs.
private fun DrawScope.drawSleigh(pose: BatPose, seconds: Float) {
    withTransform({
        translate(pose.x, pose.y)
        scale(if (pose.leftToRight) 1f else -1f, 1f, pivot = Offset.Zero)
    }) {
        // The reins.
        drawLine(SleighGold, Offset(30f, -8f), Offset(150f, -14f), strokeWidth = 1.5f)
        listOf(150f, 104f).forEachIndexed { i, dx -> drawReindeer(Offset(dx, -6f + 3f * sin(seconds * 6f + i)), pose.flap * (if (i == 0) 1f else -1f)) }
        // Sleigh body with a curled runner.
        drawPath(Path().apply {
            moveTo(-46f, -24f)
            lineTo(-46f, 8f)
            quadraticTo(-40f, 18f, -20f, 18f)
            lineTo(26f, 18f)
            quadraticTo(40f, 16f, 40f, 2f)
            lineTo(30f, 2f)
            lineTo(22f, -10f)
            lineTo(-30f, -10f)
            lineTo(-34f, -24f)
            close()
        }, SleighRed)
        drawPath(Path().apply {
            moveTo(-52f, 26f); lineTo(36f, 26f); quadraticTo(52f, 26f, 50f, 12f)
        }, SleighGold, style = Stroke(3f, cap = StrokeCap.Round))
        drawLine(SleighGold, Offset(-30f, 18f), Offset(-30f, 26f), strokeWidth = 2.5f)
        drawLine(SleighGold, Offset(20f, 18f), Offset(20f, 26f), strokeWidth = 2.5f)
        // Santa.
        drawOval(SantaRed, Offset(-30f, -40f), Size(30f, 34f))
        drawCircle(Skin, 9f, Offset(-12f, -46f))
        drawOval(Beard, Offset(-16f, -44f), Size(14f, 14f))
        drawPath(Path().apply { moveTo(-21f, -50f); lineTo(-4f, -52f); lineTo(-18f, -66f); close() }, SantaRed)
        drawCircle(Beard, 3.5f, Offset(-19f, -66f))
        // A sack of toys behind him.
        drawOval(Color(0xFF9C7A54), Offset(-48f, -40f), Size(22f, 24f))
    }
}

private fun DrawScope.drawReindeer(at: Offset, gallop: Float) {
    val legSwing = 8f * gallop
    // Legs, front and back pairs swinging opposite ways.
    drawLine(Reindeer, Offset(at.x - 14f, at.y + 6f), Offset(at.x - 18f - legSwing, at.y + 22f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(Reindeer, Offset(at.x + 12f, at.y + 6f), Offset(at.x + 16f + legSwing, at.y + 22f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawOval(Reindeer, Offset(at.x - 22f, at.y - 6f), Size(44f, 18f))
    // Neck, head, nose and antlers.
    drawLine(Reindeer, Offset(at.x + 16f, at.y - 2f), Offset(at.x + 26f, at.y - 16f), strokeWidth = 7f, cap = StrokeCap.Round)
    drawOval(Reindeer, Offset(at.x + 22f, at.y - 24f), Size(18f, 11f))
    drawCircle(Color(0xFF2A211B), 2.4f, Offset(at.x + 40f, at.y - 19f))
    drawPath(Path().apply {
        moveTo(at.x + 26f, at.y - 23f); lineTo(at.x + 22f, at.y - 36f); lineTo(at.x + 16f, at.y - 40f)
        moveTo(at.x + 23f, at.y - 32f); lineTo(at.x + 30f, at.y - 38f)
    }, Stick, style = Stroke(2f, cap = StrokeCap.Round))
    drawLine(Reindeer, Offset(at.x - 22f, at.y), Offset(at.x - 28f, at.y - 6f), strokeWidth = 3f, cap = StrokeCap.Round)
}
