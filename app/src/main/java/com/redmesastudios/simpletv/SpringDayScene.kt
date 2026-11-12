package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.sin

// The first day of spring, on the spring backdrop: beds of tulips and daffodils nodding along
// the hill, a birdhouse on a pole with a bird popping in and out of its door, and butterflies
// fluttering across. The greeting says "Hello, spring!". Drawn in design px (1920x1080).

private val TulipColors = listOf(Color(0xFFE8796B), Color(0xFFF4A6B8), Color(0xFFC99BD9), Color(0xFFF2C14E))
private val Daffodil = Color(0xFFF7D24E)
private val DaffodilCup = Color(0xFFF2A93B)
private val Leaf = Color(0xFF6E9C6D)
private val ButterflyColors = listOf(Color(0xFFF2A93B), Color(0xFF7FB7D9), Color(0xFFE88AA2))
private val ButterflyBody = Color(0xFF3A3A44)
private val HouseWall = Color(0xFF7FB7D9)
private val HouseRoof = Color(0xFFC0503A)
private val PoleWood = Color(0xFF9C7A55)
private val Robin = Color(0xFF8A6446)
private val RobinBreast = Color(0xFFE8793A)

private val Flowers = List(18) { i ->
    val left = i < 9
    val k = if (left) i else i - 9
    Triple(if (left) 230f + k * 52f else 1230f + k * 52f, 862f + (k % 3) * 5f, i)
}

fun DrawScope.drawSpringDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        Flowers.forEach { (x, baseY, i) ->
            val nod = 5f * sin(seconds * 1.2f + i * 0.7f)
            if (i % 3 == 1) drawDaffodil(x, baseY, nod) else drawTulip(x, baseY, nod, TulipColors[i % TulipColors.size])
        }
        drawBirdhouse(960f, 866f, seconds)
        Butterflies.forEach { b -> b.poseAt(seconds)?.let { drawButterfly(it, ButterflyColors[b.colorIndex % ButterflyColors.size]) } }
    }
}

private fun DrawScope.drawStem(x: Float, baseY: Float, height: Float, nod: Float): Offset {
    val head = Offset(x + nod, baseY - height)
    drawPath(Path().apply { moveTo(x, baseY); quadraticTo(x, baseY - height * 0.5f, head.x, head.y) }, Leaf,
        style = androidx.compose.ui.graphics.drawscope.Stroke(3f, cap = StrokeCap.Round))
    drawPath(Path().apply { moveTo(x, baseY); quadraticTo(x - 16f, baseY - 26f, x - 4f, baseY - 44f); quadraticTo(x - 2f, baseY - 20f, x, baseY); close() }, Leaf)
    return head
}

private fun DrawScope.drawTulip(x: Float, baseY: Float, nod: Float, color: Color) {
    val head = drawStem(x, baseY, 62f, nod)
    withTransform({ rotate(nod * 1.5f, pivot = head) }) {
        drawPath(Path().apply {
            moveTo(head.x - 11f, head.y - 22f); lineTo(head.x - 5f, head.y - 12f); lineTo(head.x, head.y - 24f); lineTo(head.x + 5f, head.y - 12f)
            lineTo(head.x + 11f, head.y - 22f); quadraticTo(head.x + 13f, head.y + 2f, head.x, head.y + 2f); quadraticTo(head.x - 13f, head.y + 2f, head.x - 11f, head.y - 22f); close()
        }, color)
    }
}

private fun DrawScope.drawDaffodil(x: Float, baseY: Float, nod: Float) {
    val head = drawStem(x, baseY, 56f, nod)
    repeat(6) { k ->
        val a = Math.toRadians(k * 60.0)
        drawCircle(Daffodil, 7f, Offset(head.x + 8f * kotlin.math.cos(a).toFloat(), head.y - 6f + 8f * kotlin.math.sin(a).toFloat()))
    }
    drawCircle(DaffodilCup, 6f, Offset(head.x + 1f, head.y - 6f))
}

// A birdhouse whose robin pokes out of the door and ducks back in.
private fun DrawScope.drawBirdhouse(x: Float, baseY: Float, seconds: Float) {
    drawLine(PoleWood, Offset(x, baseY), Offset(x, baseY - 110f), strokeWidth = 8f, cap = StrokeCap.Round)
    val top = baseY - 190f
    drawRect(HouseWall, Offset(x - 34f, top + 26f), Size(68f, 64f))
    drawPath(Path().apply { moveTo(x - 46f, top + 30f); lineTo(x, top - 8f); lineTo(x + 46f, top + 30f); close() }, HouseRoof)
    val door = Offset(x, top + 56f)
    drawCircle(Color(0xFF2E2A28), 13f, door)
    drawLine(PoleWood, Offset(x, door.y + 20f), Offset(x + 18f, door.y + 20f), strokeWidth = 3f, cap = StrokeCap.Round)
    // The robin: out for a few seconds of every eight.
    val t = seconds % 8f
    val out = when {
        t < 3f -> 0f
        t < 3.4f -> (t - 3f) / 0.4f
        t < 6f -> 1f
        t < 6.4f -> 1f - (t - 6f) / 0.4f
        else -> 0f
    }
    if (out > 0f) {
        val head = Offset(door.x + 4f * out, door.y - 2f + 6f * (1f - out))
        drawCircle(Robin, 9f * out + 2f, head)
        drawCircle(RobinBreast, 5f * out, Offset(head.x + 2f, head.y + 6f))
        if (out > 0.6f) {
            drawCircle(Color(0xFF2A211B), 1.6f, Offset(head.x + 4f, head.y - 3f))
            drawPath(Path().apply { moveTo(head.x + 8f, head.y - 2f); lineTo(head.x + 15f, head.y); lineTo(head.x + 8f, head.y + 2f); close() }, Color(0xFFE9C14A))
        }
    }
}

// Two pairs of wings on a slim body; open squeezes them toward closed as they beat.
private fun DrawScope.drawButterfly(pose: ButterflyPose, color: Color) {
    withTransform({ translate(pose.x, pose.y); scale(if (pose.leftToRight) 1f else -1f, 1f, pivot = Offset.Zero) }) {
        withTransform({ scale(pose.open, 1f, pivot = Offset.Zero) }) {
            drawOval(color, Offset(-22f, -20f), Size(22f, 20f))
            drawOval(color, Offset(0f, -20f), Size(22f, 20f))
            drawOval(color, Offset(-16f, -2f), Size(16f, 14f), alpha = 0.85f)
            drawOval(color, Offset(0f, -2f), Size(16f, 14f), alpha = 0.85f)
            drawCircle(Color.White, 3f, Offset(-12f, -11f), alpha = 0.6f)
            drawCircle(Color.White, 3f, Offset(12f, -11f), alpha = 0.6f)
        }
        drawRoundRect(ButterflyBody, Offset(-2f, -16f), Size(4f, 26f), androidx.compose.ui.geometry.CornerRadius(2f))
        drawLine(ButterflyBody, Offset(0f, -16f), Offset(-5f, -25f), strokeWidth = 1.2f)
        drawLine(ButterflyBody, Offset(0f, -16f), Offset(5f, -25f), strokeWidth = 1.2f)
    }
}
