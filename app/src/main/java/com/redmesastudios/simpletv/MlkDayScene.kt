package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.sin

// Martin Luther King Jr. Day on the winter scene: a sun rising behind the hill, two lines of
// people of every color holding hands along it on either side, a ripple passing through
// their joined hands, and now and then a dove with an olive sprig crossing the sky. The
// greeting says "Honoring Dr. King". Drawn in design px (1920x1080).

private val SunCore = Color(0xFFF6CF7A)
private val SunGlow = Color(0xFFF8DFA2)
private val SkinTones = listOf(
    Color(0xFF6B4630), Color(0xFFF1CFB0), Color(0xFFA9714B), Color(0xFFE2B48C), Color(0xFF8A5A3B), Color(0xFFC98E62),
)
private val Coats = listOf(
    Color(0xFFC0503A), Color(0xFF3A5A92), Color(0xFF5E8C61), Color(0xFFE2A13B), Color(0xFF8C5A9C), Color(0xFF3F7F8C), Color(0xFFD9774F),
)
private val Hair = listOf(Color(0xFF2A211B), Color(0xFF4A3324), Color(0xFF6E5440), Color(0xFF1E1A18))
private val Trousers = Color(0xFF3A3A44)
private val Beanie = listOf(Color(0xFFF2C14E), Color(0xFFE8796B), Color(0xFF7FB7D9))
private val DoveWhite = Color(0xFFFDFCF8)
private val DoveShade = Color(0xFFD9DEE4)
private val DoveBeak = Color(0xFFE2A13B)
private val Olive = Color(0xFF6E9C6D)

private const val PeopleBaseY = 868f

// Each person: where they stand, how tall (1 is an adult), and whether a child's beanie.
private data class HandHolder(val x: Float, val scale: Float, val beanie: Boolean)

private val LeftLine = listOf(
    HandHolder(250f, 1.08f, false), HandHolder(338f, 0.78f, true), HandHolder(420f, 1.12f, false),
    HandHolder(506f, 0.86f, false), HandHolder(590f, 1.04f, false), HandHolder(668f, 0.74f, true),
)
private val RightLine = listOf(
    HandHolder(1252f, 0.76f, true), HandHolder(1332f, 1.1f, false), HandHolder(1418f, 0.84f, false),
    HandHolder(1502f, 1.06f, false), HandHolder(1588f, 0.8f, true), HandHolder(1670f, 1.12f, false),
)

// The dove crosses high over the apps every 46 seconds, left to right.
val Dove = Bat(cycleSeconds = 46f, flightSeconds = 19f, offsetSeconds = 9f, y = 104f, size = 60f, leftToRight = true)

// The sun behind the far hill, drawn with the backdrop so the hill covers its lower half.
fun DrawScope.drawRisingSun(unit: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        val center = Offset(960f, 856f)
        drawCircle(SunGlow, 250f, center, alpha = 0.16f)
        drawCircle(SunGlow, 180f, center, alpha = 0.24f)
        drawCircle(SunCore, 112f, center, alpha = 0.85f)
    }
}

fun DrawScope.drawMlkDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawLineOfPeople(LeftLine, seconds, 0)
        drawLineOfPeople(RightLine, seconds, LeftLine.size)
        Dove.poseAt(seconds)?.let { drawDove(it) }
    }
}

// A line of people holding hands: each pair's hands meet halfway between them, and a
// gentle ripple runs along the line, lifting the joined hands in turn.
private fun DrawScope.drawLineOfPeople(line: List<HandHolder>, seconds: Float, colorStart: Int) {
    fun shoulder(p: HandHolder, side: Float) = Offset(p.x + side * 11f * p.scale, PeopleBaseY - 74f * p.scale)
    val hands = line.zipWithNext().mapIndexed { i, (a, b) ->
        val ripple = sin(seconds * 1.4f - i * 0.7f)
        Offset((a.x + b.x) / 2, (shoulder(a, 1f).y + shoulder(b, -1f).y) / 2 + 26f - 12f * ripple)
    }
    line.forEachIndexed { i, p ->
        val color = colorStart + i
        // Arms first, so the bodies sit over the shoulders. The two at the ends wave.
        val left = hands.getOrNull(i - 1) ?: Offset(p.x - 30f * p.scale, PeopleBaseY - (104f + 8f * sin(seconds * 2.6f)) * p.scale)
        val right = hands.getOrNull(i) ?: Offset(p.x + 30f * p.scale, PeopleBaseY - (104f + 8f * sin(seconds * 2.6f + 1f)) * p.scale)
        val sleeve = Coats[color % Coats.size]
        val skin = SkinTones[(color * 5) % SkinTones.size]
        listOf(shoulder(p, -1f) to left, shoulder(p, 1f) to right).forEach { (from, hand) ->
            drawLine(sleeve, from, hand, strokeWidth = 8f * p.scale, cap = StrokeCap.Round)
            drawCircle(skin, 4.6f * p.scale, hand)
        }
        drawPerson(p, color, seconds)
    }
}

private fun DrawScope.drawPerson(p: HandHolder, color: Int, seconds: Float) {
    val s = p.scale
    val y = PeopleBaseY
    val coat = Coats[color % Coats.size]
    val skin = SkinTones[(color * 5) % SkinTones.size]
    // Legs, then the coat, a scarf, the head and hair or a beanie.
    drawLine(Trousers, Offset(p.x - 6f * s, y - 34f * s), Offset(p.x - 7f * s, y), strokeWidth = 7f * s, cap = StrokeCap.Round)
    drawLine(Trousers, Offset(p.x + 6f * s, y - 34f * s), Offset(p.x + 7f * s, y), strokeWidth = 7f * s, cap = StrokeCap.Round)
    drawPath(Path().apply {
        moveTo(p.x - 12f * s, y - 78f * s)
        lineTo(p.x + 12f * s, y - 78f * s)
        quadraticTo(p.x + 17f * s, y - 52f * s, p.x + 16f * s, y - 30f * s)
        lineTo(p.x - 16f * s, y - 30f * s)
        quadraticTo(p.x - 17f * s, y - 52f * s, p.x - 12f * s, y - 78f * s)
        close()
    }, coat)
    val head = Offset(p.x, y - 92f * s)
    drawCircle(skin, 13f * s, head)
    drawLine(Coats[(color + 3) % Coats.size], Offset(p.x - 11f * s, y - 78f * s), Offset(p.x + 11f * s, y - 78f * s), strokeWidth = 6f * s, cap = StrokeCap.Round)
    if (p.beanie) {
        val hat = Beanie[color % Beanie.size]
        drawArc(hat, 180f, 180f, useCenter = true, topLeft = Offset(head.x - 14f * s, head.y - 15f * s), size = Size(28f * s, 26f * s))
        drawCircle(hat, 5f * s, Offset(head.x, head.y - 15f * s))
    } else {
        drawArc(Hair[color % Hair.size], 170f, 200f, useCenter = true, topLeft = Offset(head.x - 13.5f * s, head.y - 14f * s), size = Size(27f * s, 22f * s))
    }
    // A slow blink now and then, so they're alive but still.
    val blink = sin(seconds * 0.9f + color * 1.9f) > 0.985f
    if (!blink) {
        drawCircle(Hair[0], 1.6f * s, Offset(head.x - 4.5f * s, head.y + 1f * s))
        drawCircle(Hair[0], 1.6f * s, Offset(head.x + 4.5f * s, head.y + 1f * s))
    }
    drawArc(Hair[0], 20f, 140f, useCenter = false, topLeft = Offset(head.x - 4.5f * s, head.y + 1f * s), size = Size(9f * s, 7f * s), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4f * s, cap = StrokeCap.Round))
}

// A white dove in flight, an olive sprig in its beak, its wings beating slowly.
private fun DrawScope.drawDove(pose: BatPose) {
    val lift = 0.25f + 0.75f * (pose.flap * 0.5f + 0.5f)
    val wing = Path().apply {
        moveTo(6f, -2f)
        cubicTo(0f, -26f, -18f, -42f, -40f, -46f)
        cubicTo(-32f, -30f, -22f, -14f, -14f, -2f)
        close()
    }
    withTransform({
        translate(pose.x, pose.y)
        scale(if (pose.leftToRight) 1.3f else -1.3f, 1.3f, pivot = Offset.Zero)
    }) {
        withTransform({ translate(-6f, 2f); scale(0.85f, 0.85f * (1.3f - lift), pivot = Offset.Zero) }) { drawPath(wing, DoveShade) }
        // Tail, body, head.
        drawPath(Path().apply { moveTo(-18f, 0f); lineTo(-40f, -6f); lineTo(-40f, 8f); close() }, DoveWhite)
        drawOval(DoveWhite, Offset(-24f, -9f), Size(46f, 18f))
        drawCircle(DoveWhite, 8f, Offset(22f, -6f))
        drawPath(Path().apply { moveTo(28f, -7f); lineTo(36f, -4f); lineTo(28f, -2f); close() }, DoveBeak)
        drawCircle(Hair[0], 1.3f, Offset(24f, -8f))
        // The sprig: a stem and a few leaves.
        drawLine(Olive, Offset(32f, -4f), Offset(44f, 6f), strokeWidth = 1.6f, cap = StrokeCap.Round)
        listOf(Offset(36f, -1f), Offset(40f, 3f), Offset(44f, 6f)).forEachIndexed { i, at ->
            withTransform({ rotate(if (i % 2 == 0) 30f else -40f, pivot = at) }) { drawOval(Olive, Offset(at.x - 2.5f, at.y - 5f), Size(5f, 10f)) }
        }
        withTransform({ scale(1f, lift, pivot = Offset(0f, -2f)) }) {
            drawPath(wing, DoveWhite)
            drawPath(wing, DoveShade, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f))
        }
    }
}
