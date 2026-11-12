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
import kotlin.math.sin

// Valentine's week (February 7th through the 14th): a garland of hearts swagged along the
// hill, little hearts rising off the snow and fading, a mailbox with its flag up and a
// love letter, a vase of roses, a big "BE MINE" candy heart, and two lovebirds on a branch
// leaning in now and then. Drawn in design px (1920x1080).

private val HeartColors = listOf(Color(0xFFD9534F), Color(0xFFF4A6B8), Color(0xFFE8796B), Color(0xFFC2477A), Color(0xFFF7C6CF))
private val GarlandString = Color(0xFFB98A8A)
private val MailboxRed = Color(0xFFC0503A)
private val MailboxDark = Color(0xFF9A3B2C)
private val Post = Color(0xFF9C7A55)
private val Envelope = Color(0xFFFBF6EC)
private val EnvelopeEdge = Color(0xFFD9CDBB)
private val Vase = Color(0xFF7FB7D9)
private val Stem = Color(0xFF5E8C61)
private val Rose = Color(0xFFD2403A)
private val RoseDark = Color(0xFFA8302C)
private val CandyPink = Color(0xFFF7C6CF)
private val Lovebird = listOf(Color(0xFF6DBE7B), Color(0xFFE8796B))
private val Beak = Color(0xFFE9C14A)
private val Branch = Color(0xFF6E5440)

private const val GarlandY = 664f
private const val GarlandDip = 30f
private val GarlandAnchors = listOf(200f, 585f, 970f, 1355f, 1740f)

private val CandyText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xC2, 0x47, 0x7A)
    textSize = 24f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.06f
}

fun DrawScope.drawValentinesFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        RisingHearts.forEach { heart ->
            val pose = heart.poseAt(seconds)
            drawHeart(Offset(pose.x, pose.y), heart.size, HeartColors[heart.colorIndex % HeartColors.size], pose.alpha * 0.85f)
        }
        drawHeartGarland(seconds)
        drawMailbox(400f, 866f, seconds)
        drawRoses(720f, 866f, seconds)
        drawCandyHeart(1180f, 866f, seconds)
        drawLovebirds(1480f, 866f, seconds)
    }
}

// A heart size wide, centered on center.
internal fun DrawScope.drawHeart(center: Offset, size: Float, color: Color, alpha: Float = 1f) {
    val w = size
    val h = size * 0.95f
    drawPath(Path().apply {
        moveTo(center.x, center.y + h / 2)
        cubicTo(center.x - w * 0.95f, center.y - h * 0.05f, center.x - w * 0.45f, center.y - h * 0.75f, center.x, center.y - h * 0.22f)
        cubicTo(center.x + w * 0.45f, center.y - h * 0.75f, center.x + w * 0.95f, center.y - h * 0.05f, center.x, center.y + h / 2)
        close()
    }, color, alpha = alpha)
}

private fun DrawScope.drawHeartGarland(seconds: Float) {
    var count = 0
    GarlandAnchors.zipWithNext().forEach { (from, to) ->
        drawPath(Path().apply {
            moveTo(from, GarlandY); quadraticTo((from + to) / 2, GarlandY + 2f * GarlandDip, to, GarlandY)
        }, GarlandString, style = Stroke(2f))
        val hearts = 7
        repeat(hearts) { i ->
            val t = (i + 0.5f) / hearts
            val at = Offset(from + (to - from) * t, GarlandY + 4f * GarlandDip * t * (1f - t))
            val sway = 5f * sin(seconds * 1.3f + count * 0.8f)
            withTransform({ rotate(sway, pivot = at) }) {
                drawLine(GarlandString, at, Offset(at.x, at.y + 8f), strokeWidth = 1.5f)
                drawHeart(Offset(at.x, at.y + 22f), if (count % 2 == 0) 30f else 24f, HeartColors[count % HeartColors.size])
            }
            count++
        }
    }
    GarlandAnchors.forEach { drawHeart(Offset(it, GarlandY + 4f), 20f, HeartColors[0]) }
}

// A rural mailbox on a post, its flag up and a heart-sealed letter sticking out.
private fun DrawScope.drawMailbox(x: Float, baseY: Float, seconds: Float) {
    val top = baseY - 130f
    drawRect(Post, Offset(x - 6f, top + 40f), Size(12f, baseY - top - 40f))
    drawRoundRect(MailboxDark, Offset(x - 50f, top + 2f), Size(100f, 42f), CornerRadius(8f))
    drawRoundRect(MailboxRed, Offset(x - 50f, top - 18f), Size(100f, 56f), CornerRadius(26f, 26f))
    drawRect(MailboxRed, Offset(x - 50f, top + 10f), Size(100f, 30f))
    drawRoundRect(MailboxDark, Offset(x + 44f, top - 12f), Size(8f, 50f), CornerRadius(3f))
    // The letter, nudging out.
    val nudge = 3f * sin(seconds * 1.1f)
    withTransform({ rotate(-12f, pivot = Offset(x + 48f, top + 16f)) }) {
        drawRect(Envelope, Offset(x + 40f + nudge, top + 2f), Size(44f, 28f))
        drawRect(EnvelopeEdge, Offset(x + 40f + nudge, top + 2f), Size(44f, 28f), style = Stroke(1.2f))
        drawPath(Path().apply { moveTo(x + 40f + nudge, top + 2f); lineTo(x + 62f + nudge, top + 18f); lineTo(x + 84f + nudge, top + 2f) }, EnvelopeEdge, style = Stroke(1.2f))
        drawHeart(Offset(x + 62f + nudge, top + 18f), 11f, Rose)
    }
    // The flag, up, with a little flutter.
    withTransform({ rotate(-4f * sin(seconds * 2f), pivot = Offset(x - 38f, top + 14f)) }) {
        drawRect(Rose, Offset(x - 42f, top - 34f), Size(5f, 48f))
        drawRect(Rose, Offset(x - 37f, top - 34f), Size(18f, 12f))
    }
}

// Six roses in a vase, nodding a little.
private fun DrawScope.drawRoses(x: Float, baseY: Float, seconds: Float) {
    val vaseTop = baseY - 60f
    listOf(-26f to 92f, -12f to 112f, 2f to 120f, 16f to 104f, 28f to 88f, -4f to 96f).forEachIndexed { i, (dx, height) ->
        val nod = 2.5f * sin(seconds * 0.9f + i)
        val head = Offset(x + dx * 1.5f + nod, vaseTop - height + 60f)
        drawLine(Stem, Offset(x + dx * 0.3f, vaseTop + 6f), head, strokeWidth = 3f, cap = StrokeCap.Round)
        drawOval(Stem, Offset((x + head.x) / 2 + 2f, (vaseTop + head.y) / 2 - 4f), Size(12f, 7f))
        drawCircle(RoseDark, 11f, head)
        drawCircle(Rose, 9f, Offset(head.x - 1f, head.y - 1f))
        drawArc(RoseDark, 200f, 220f, useCenter = false, topLeft = Offset(head.x - 5f, head.y - 6f), size = Size(9f, 9f), style = Stroke(1.6f))
    }
    drawPath(Path().apply {
        moveTo(x - 22f, vaseTop); lineTo(x + 22f, vaseTop)
        quadraticTo(x + 34f, baseY - 26f, x + 18f, baseY); lineTo(x - 18f, baseY)
        quadraticTo(x - 34f, baseY - 26f, x - 22f, vaseTop); close()
    }, Vase)
    drawOval(Color.White, Offset(x - 16f, vaseTop + 12f), Size(7f, 24f), alpha = 0.35f)
}

// A big pale candy heart on a stake, "BE MINE" stamped on it.
private fun DrawScope.drawCandyHeart(x: Float, baseY: Float, seconds: Float) {
    val center = Offset(x, baseY - 92f)
    drawLine(Post, Offset(x, baseY), Offset(x, center.y), strokeWidth = 4f)
    withTransform({ rotate(3f * sin(seconds * 0.9f), pivot = Offset(x, baseY)) }) {
        drawHeart(Offset(center.x + 3f, center.y + 4f), 112f, Color(0xFFE6A3B1))
        drawHeart(center, 112f, CandyPink)
        drawIntoCanvas { it.nativeCanvas.drawText("BE MINE", center.x, center.y + 6f, CandyText) }
    }
}

// Two lovebirds on a little branch post, leaning in to touch beaks every few seconds.
private fun DrawScope.drawLovebirds(x: Float, baseY: Float, seconds: Float) {
    val perch = baseY - 92f
    drawLine(Branch, Offset(x, baseY), Offset(x, perch), strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(Branch, Offset(x - 70f, perch + 4f), Offset(x + 70f, perch - 4f), strokeWidth = 6f, cap = StrokeCap.Round)
    // A lean that rises and falls, held for a moment at the top.
    val phase = (seconds % 6f) / 6f
    val lean = (sin(phase * 2f * Math.PI.toFloat()) * 1.4f).coerceIn(0f, 1f)
    listOf(-1f, 1f).forEachIndexed { i, side ->
        val body = Offset(x + side * 26f, perch - 20f)
        withTransform({ rotate(-side * 12f * lean, pivot = Offset(body.x, perch)) }) {
            val color = Lovebird[i]
            drawOval(color, Offset(body.x - 16f, body.y - 14f), Size(32f, 30f))
            drawPath(Path().apply { moveTo(body.x + side * 12f, body.y + 6f); lineTo(body.x + side * 30f, body.y + 22f); lineTo(body.x + side * 20f, body.y + 4f); close() }, color)
            val head = Offset(body.x - side * 9f, body.y - 18f)
            drawCircle(color, 12f, head)
            drawCircle(Color(0xFF2A211B), 2.2f, Offset(head.x - side * 4f, head.y - 2f))
            drawPath(Path().apply { moveTo(head.x - side * 10f, head.y - 2f); lineTo(head.x - side * 18f, head.y + 2f); lineTo(head.x - side * 10f, head.y + 5f); close() }, Beak)
        }
    }
    if (lean > 0.9f) drawHeart(Offset(x, perch - 70f - 10f * (lean - 0.9f) * 10f), 18f, Rose, alpha = 0.9f)
}
