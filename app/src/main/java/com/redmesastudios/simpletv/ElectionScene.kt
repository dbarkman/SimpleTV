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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Election Day on the fall scene, on the hillside between the apps and the glance cards:
// red, white and blue bunting swagged across the whole space and tied to a waving flag in
// the middle, a ballot box with ballots dropping in now and then, a VOTE yard sign, a bunch
// of balloons, a few little hand flags, some twinkling stars, and now and then Uncle Sam
// marching by, stopping beside the flag to wave. Drawn in design px (1920x1080).

private val FlagRed = Color(0xFFC9483F)
private val FlagBlue = Color(0xFF3A5A92)
private val FlagNavy = Color(0xFF2C4372)
private val FlagWhite = Color(0xFFFBF6EC)
private val WhiteEdge = Color(0xFFD9CDBB)
private val StarGold = Color(0xFFE9B949)
private val Twine = Color(0xFF8A7B6A)
private val PoleColor = Color(0xFFB3A99C)
private val StakeColor = Color(0xFF9C7A55)
private val CheckColor = Color(0xFF3A5A92)

private val Pennants = listOf(FlagRed, FlagWhite, FlagBlue)

// The bunting hangs from these points; the middle one is the top of the flagpole.
private val BuntingAnchors = listOf(200f, 585f, 970f, 1355f, 1740f)
private const val BuntingY = 662f
private const val BuntingDip = 34f
private const val PennantsPerSwag = 8

private const val FlagX = 970f
private const val FlagBaseY = 838f
private const val FlagWidth = 96f

// The moving parts, redrawn with the falling leaves.
fun DrawScope.drawElectionFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        Twinkles.forEachIndexed { i, (x, y) -> drawTwinkle(x, y, seconds, i) }
        drawBallotBox(400f, 850f, seconds)
        drawYardSign(620f, 850f)
        drawBalloons(1355f, 810f, seconds)
        listOf(1585f to 0f, 1625f to 1.3f, 1665f to 2.6f).forEach { (x, phase) -> drawHandFlag(x, 802f, seconds, phase) }
        drawBunting(seconds)
        drawFlagpole(seconds)
        UncleSam.poseAt(seconds)?.let { drawUncleSam(it, seconds) }
    }
}

private val Twinkles = listOf(290f to 790f, 800f to 772f, 1150f to 768f, 1480f to 772f, 1722f to 770f)

private fun starPath(center: Offset, radius: Float, rotation: Float = 0f) = Path().apply {
    repeat(10) { i ->
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = rotation - PI.toFloat() / 2 + i * PI.toFloat() / 5
        val point = Offset(center.x + r * cos(angle), center.y + r * sin(angle))
        if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
    close()
}

// A small gold star that slowly swells and fades.
private fun DrawScope.drawTwinkle(x: Float, y: Float, seconds: Float, index: Int) {
    val pulse = 0.5f + 0.5f * sin(seconds * 1.6f + index * 1.7f)
    drawPath(starPath(Offset(x, y), 7f + 3f * pulse, rotation = 0.15f * sin(seconds * 0.7f + index)), StarGold, alpha = 0.45f + 0.5f * pulse)
}

private fun DrawScope.drawBunting(seconds: Float) {
    var count = 0
    BuntingAnchors.zipWithNext().forEach { (from, to) ->
        fun pointAt(t: Float) = Offset(from + (to - from) * t, BuntingY + 4f * BuntingDip * t * (1f - t))
        drawPath(Path().apply {
            moveTo(from, BuntingY)
            quadraticTo((from + to) / 2, BuntingY + 2f * BuntingDip, to, BuntingY)
        }, Twine, style = Stroke(2.5f))
        repeat(PennantsPerSwag) { i ->
            val t = (i + 0.5f) / PennantsPerSwag
            val top = pointAt(t)
            // The flag flies where these would hang.
            if (top.x > FlagX && top.x < FlagX + FlagWidth + 12f) return@repeat
            // Each pennant follows the string's slope and sways a little on its own.
            val slope = Math.toDegrees(kotlin.math.atan2((4f * BuntingDip * (1f - 2f * t)).toDouble(), (to - from).toDouble())).toFloat()
            val sway = 5f * sin(seconds * 1.3f + count * 0.8f)
            val color = Pennants[count % Pennants.size]
            withTransform({ rotate(slope + sway, pivot = top) }) {
                val pennant = Path().apply {
                    moveTo(top.x - 15f, top.y)
                    lineTo(top.x + 15f, top.y)
                    lineTo(top.x, top.y + 38f)
                    close()
                }
                drawPath(pennant, color)
                if (color == FlagWhite) drawPath(pennant, WhiteEdge, style = Stroke(1.5f))
            }
            count++
        }
    }
    // Rosettes where the string is tied; the flagpole's ball covers the middle knot.
    BuntingAnchors.filter { it != FlagX }.forEach { drawRosette(Offset(it, BuntingY)) }
}

private fun DrawScope.drawRosette(center: Offset) {
    drawPath(Path().apply {
        moveTo(center.x - 3f, center.y); lineTo(center.x - 9f, center.y + 26f); lineTo(center.x - 4f, center.y + 22f); lineTo(center.x + 1f, center.y + 26f); close()
    }, FlagRed)
    drawPath(Path().apply {
        moveTo(center.x + 3f, center.y); lineTo(center.x + 10f, center.y + 24f); lineTo(center.x + 5f, center.y + 21f); lineTo(center.x + 1f, center.y + 25f); close()
    }, FlagBlue)
    drawCircle(FlagBlue, 13f, center)
    drawCircle(FlagWhite, 9f, center)
    drawCircle(FlagRed, 5f, center)
}

// A flag on a pole, rippling: drawn in narrow strips, each lifted by a wave that grows
// toward the loose end, with a little shading in the folds.
private fun DrawScope.drawFlagpole(seconds: Float) {
    val top = 664f
    drawLine(PoleColor, Offset(FlagX, FlagBaseY), Offset(FlagX, top), strokeWidth = 5f, cap = StrokeCap.Round)
    drawOval(PoleColor, Offset(FlagX - 12f, FlagBaseY - 4f), Size(24f, 8f), alpha = 0.6f)

    val left = FlagX + 3f
    val flagTop = top + 10f
    val width = FlagWidth
    val height = 58f
    val strips = 24
    val stripWidth = width / strips
    val stripes = 13
    val stripeHeight = height / stripes
    repeat(strips) { k ->
        val along = (k + 0.5f) / strips
        val phase = along * 5f - seconds * 3.2f
        val lift = 5f * along * sin(phase)
        val x = left + k * stripWidth
        repeat(stripes) { s ->
            val inCanton = k < strips * 0.42f && s < 7
            val color = when {
                inCanton -> FlagNavy
                s % 2 == 0 -> FlagRed
                else -> FlagWhite
            }
            drawRect(color, Offset(x, flagTop + s * stripeHeight + lift), Size(stripWidth + 0.6f, stripeHeight + 0.6f))
        }
        drawRect(Color.Black, Offset(x, flagTop + lift), Size(stripWidth + 0.6f, height), alpha = 0.07f * (1f + cos(phase)))
    }
    // Stars in the canton, riding the same wave.
    repeat(3) { row ->
        repeat(4) { col ->
            val x = left + 5f + col * 9.5f
            val along = (x - left) / width
            val lift = 5f * along * sin(along * 5f - seconds * 3.2f)
            drawCircle(FlagWhite, 1.6f, Offset(x, flagTop + 5f + row * 9.5f + lift))
        }
    }
    drawCircle(StarGold, 7f, Offset(FlagX, top - 2f))
}

// A ballot box; every so often a ballot with a check mark slides down into the slot.
private fun DrawScope.drawBallotBox(x: Float, baseY: Float, seconds: Float) {
    val w = 104f
    val h = 74f
    val lidY = baseY - h - 12f
    drawOval(Color.Black, Offset(x - w * 0.55f, baseY - 5f), Size(w * 1.1f, 10f), alpha = 0.08f)

    val cycle = (seconds + 2f) % 7f
    if (cycle < 1.8f) {
        val drop = (cycle / 1.8f).let { it * it }
        val ballotY = lidY - 58f + drop * 66f
        clipRect(top = 0f, bottom = lidY + 2f) {
            withTransform({ rotate(-4f + 4f * drop, pivot = Offset(x, ballotY + 22f)) }) {
                drawRoundRect(FlagWhite, Offset(x - 16f, ballotY), Size(32f, 44f), CornerRadius(2f))
                drawRoundRect(WhiteEdge, Offset(x - 16f, ballotY), Size(32f, 44f), CornerRadius(2f), style = Stroke(1.5f))
                drawPath(Path().apply {
                    moveTo(x - 7f, ballotY + 16f); lineTo(x - 2f, ballotY + 22f); lineTo(x + 8f, ballotY + 9f)
                }, CheckColor, style = Stroke(3f, cap = StrokeCap.Round))
                drawLine(WhiteEdge, Offset(x - 9f, ballotY + 31f), Offset(x + 9f, ballotY + 31f), strokeWidth = 2f)
                drawLine(WhiteEdge, Offset(x - 9f, ballotY + 37f), Offset(x + 5f, ballotY + 37f), strokeWidth = 2f)
            }
        }
    }

    drawRoundRect(FlagBlue, Offset(x - w / 2, baseY - h), Size(w, h), CornerRadius(6f))
    drawRect(FlagRed, Offset(x - w / 2, baseY - h * 0.32f), Size(w, 9f))
    drawPath(starPath(Offset(x, baseY - h * 0.62f), 15f), FlagWhite)
    drawRoundRect(FlagNavy, Offset(x - w / 2 - 5f, lidY), Size(w + 10f, 14f), CornerRadius(4f))
    drawRoundRect(Color(0xFF1C2B4A), Offset(x - 22f, lidY + 4f), Size(44f, 6f), CornerRadius(3f))
}

private val SignText = android.graphics.Paint().apply {
    isAntiAlias = true
    color = android.graphics.Color.rgb(0x2C, 0x43, 0x72)
    textSize = 44f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.08f
}

private fun DrawScope.drawYardSign(x: Float, baseY: Float) {
    val w = 184f
    val h = 84f
    val signBottom = baseY - 40f
    val signTop = signBottom - h
    withTransform({ rotate(-3f, pivot = Offset(x, baseY)) }) {
        drawRect(StakeColor, Offset(x - 3f, signBottom - 6f), Size(6f, baseY - signBottom + 6f))
        drawRoundRect(FlagWhite, Offset(x - w / 2, signTop), Size(w, h), CornerRadius(6f))
        drawRoundRect(FlagNavy, Offset(x - w / 2, signTop), Size(w, h), CornerRadius(6f), style = Stroke(3f))
        drawRect(FlagRed, Offset(x - w / 2 + 3f, signBottom - 15f), Size(w - 6f, 6f))
        drawRect(FlagBlue, Offset(x - w / 2 + 3f, signBottom - 9f), Size(w - 6f, 4f))
        drawPath(starPath(Offset(x - 74f, signTop + 34f), 8f), FlagRed)
        drawPath(starPath(Offset(x + 74f, signTop + 34f), 8f), FlagRed)
        drawIntoCanvas { it.nativeCanvas.drawText("VOTE", x, signTop + 50f, SignText) }
    }
}

// Three balloons on strings tied to one weight, bobbing out of step.
private fun DrawScope.drawBalloons(x: Float, baseY: Float, seconds: Float) {
    val balloons = listOf(
        Triple(Offset(x - 30f, 760f), FlagRed, 0f),
        Triple(Offset(x + 4f, 746f), FlagWhite, 2.1f),
        Triple(Offset(x + 34f, 766f), FlagBlue, 4.2f),
    )
    val knot = Offset(x, baseY - 8f)
    balloons.forEach { (center, color, phase) ->
        val bob = 5f * sin(seconds * 1.2f + phase)
        val sway = 3f * sin(seconds * 0.8f + phase)
        val c = Offset(center.x + sway, center.y + bob)
        drawPath(Path().apply {
            moveTo(c.x, c.y + 28f)
            quadraticTo((c.x + knot.x) / 2 + 6f * sin(seconds + phase), (c.y + knot.y) / 2, knot.x, knot.y)
        }, Twine, style = Stroke(1.5f))
        drawOval(color, Offset(c.x - 21f, c.y - 26f), Size(42f, 52f))
        if (color == FlagWhite) drawOval(WhiteEdge, Offset(c.x - 21f, c.y - 26f), Size(42f, 52f), style = Stroke(1.5f))
        drawPath(Path().apply { moveTo(c.x - 4f, c.y + 30f); lineTo(c.x + 4f, c.y + 30f); lineTo(c.x, c.y + 24f); close() }, color)
        drawOval(Color.White, Offset(c.x - 12f, c.y - 18f), Size(8f, 13f), alpha = 0.45f)
    }
    drawRoundRect(FlagNavy, Offset(knot.x - 8f, knot.y), Size(16f, 10f), CornerRadius(3f))
}

// A little hand flag stuck in the grass.
private fun DrawScope.drawHandFlag(x: Float, baseY: Float, seconds: Float, phase: Float) {
    val top = baseY - 62f
    drawLine(StakeColor, Offset(x, baseY), Offset(x, top), strokeWidth = 3f, cap = StrokeCap.Round)
    val w = 34f
    val h = 22f
    val strips = 10
    repeat(strips) { k ->
        val along = (k + 0.5f) / strips
        val lift = 2.5f * along * sin(along * 4f - seconds * 3f + phase)
        repeat(5) { s ->
            val color = if (k < 4 && s < 3) FlagNavy else if (s % 2 == 0) FlagRed else FlagWhite
            drawRect(color, Offset(x + 1.5f + k * w / strips, top + 2f + s * h / 5 + lift), Size(w / strips + 0.5f, h / 5 + 0.5f))
        }
    }
    drawCircle(StarGold, 2.5f, Offset(x, top))
}

private const val SamScale = 1.35f
private val SamCoat = Color(0xFF34508A)
private val SamCoatBack = Color(0xFF2A4170)
private val SamSkin = Color(0xFFF1C9A5)
private val SamHair = Color(0xFFF7F4EE)
private val SamShoe = Color(0xFF2A211B)

// Upright and unhurried, facing the way he marches. Feet at the pose's x, y.
private fun DrawScope.drawUncleSam(pose: MarcherPose, seconds: Float) {
    withTransform({
        translate(pose.x, pose.y)
        scale(SamScale, SamScale, pivot = Offset.Zero)
    }) {
        val hip = Offset(0f, -56f)
        val shoulder = Offset(2f, -94f)
        val legSwing = pose.stride * 0.3f
        val armSwing = -pose.stride * 0.3f
        drawOval(Color.Black, Offset(-16f, -3f), Size(32f, 6f), alpha = 0.08f)

        // Back arm and leg first.
        sleeve(shoulder, limb(shoulder, 36f, -armSwing), SamCoatBack)
        trouserLeg(hip, limb(hip, 54f, -legSwing))

        // The coat's tails, then the coat.
        drawPath(Path().apply {
            moveTo(-11f, -66f); lineTo(-19f, -34f); lineTo(-6f, -40f); lineTo(2f, -60f); close()
        }, SamCoatBack)
        drawRoundRect(SamCoat, Offset(-12f, -100f), Size(25f, 46f), CornerRadius(8f))
        drawPath(Path().apply { moveTo(3f, -100f); lineTo(13f, -100f); lineTo(9f, -80f); close() }, FlagWhite)
        drawCircle(StarGold, 1.6f, Offset(9f, -74f))
        drawCircle(StarGold, 1.6f, Offset(9f, -66f))

        trouserLeg(hip, limb(hip, 54f, legSwing))

        // Head: white hair at the back, a goatee, and a bow tie under the chin.
        drawCircle(SamHair, 7f, Offset(-5f, -110f))
        drawCircle(SamSkin, 10.5f, Offset(4f, -112f))
        drawCircle(SamSkin, 2.6f, Offset(14f, -111f))
        drawCircle(SamShoe, 1.5f, Offset(9f, -114f))
        drawPath(Path().apply { moveTo(3f, -105f); lineTo(14f, -105f); lineTo(9f, -93f); close() }, SamHair)
        drawPath(Path().apply {
            moveTo(-1f, -101f); lineTo(-1f, -95f); lineTo(4f, -98f); lineTo(9f, -95f); lineTo(9f, -101f); lineTo(4f, -98f); close()
        }, FlagRed)

        // The tall striped hat with a starred band.
        drawRect(FlagWhite, Offset(-7f, -156f), Size(22f, 36f))
        listOf(-3f, 4f, 11f).forEach { x -> drawRect(FlagRed, Offset(x, -156f), Size(3.5f, 28f)) }
        drawRect(SamCoat, Offset(-7f, -130f), Size(22f, 9f))
        drawPath(starPath(Offset(4f, -125.5f), 3.6f), FlagWhite)
        drawRoundRect(SamCoat, Offset(-12f, -123f), Size(32f, 5f), CornerRadius(2.5f))

        // The front arm, over everything: it swings while he marches; stopped, it rises
        // out past his face and the hand waves.
        val waving = PI.toFloat() * 0.68f + 0.25f * sin(seconds * 7f)
        val armAngle = armSwing + (waving - armSwing) * pose.raise
        sleeve(shoulder, limb(shoulder, 36f, armAngle), SamCoat)
    }
}

private fun DrawScope.sleeve(from: Offset, hand: Offset, color: Color) {
    drawLine(color, from, hand, strokeWidth = 8f, cap = StrokeCap.Round)
    drawCircle(FlagWhite, 4.5f, hand)
}

// Red and white striped trousers ending in a black shoe pointing forward.
private fun DrawScope.trouserLeg(from: Offset, foot: Offset) {
    drawLine(FlagWhite, from, foot, strokeWidth = 9f, cap = StrokeCap.Round)
    drawLine(FlagRed, from, foot, strokeWidth = 3.2f)
    drawOval(SamShoe, Offset(foot.x - 4f, foot.y - 3f), Size(15f, 7f))
}

private fun limb(from: Offset, length: Float, angle: Float) =
    Offset(from.x + length * sin(angle), from.y + length * cos(angle))
