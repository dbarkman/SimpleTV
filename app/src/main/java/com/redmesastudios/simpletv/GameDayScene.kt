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

// The big game's Sunday on the winter scene: a football on its tee at the left, kicked now
// and then end over end through the goalposts at the right, a scoreboard whose home score
// goes up 3 with every kick that's good, a GO TEAM! pennant and a bowl of chips. No team or league
// marks anywhere. The greeting says "Happy game day!". Drawn in design px (1920x1080).

private val Leather = Color(0xFF8A4B2A)
private val LeatherDark = Color(0xFF6E3A20)
private val Lace = Color(0xFFFBF6EC)
private val PostYellow = Color(0xFFE9C14A)
private val PostShade = Color(0xFFC9A23A)
private val Streamer = Color(0xFFE8793A)
private val TeeOrange = Color(0xFFE8793A)
private val Board = Color(0xFF2E2A28)
private val BoardEdge = Color(0xFF4A4440)
private val BoardPost = Color(0xFF8C8478)
private val Felt = Color(0xFF3A5A92)
private val Bowl = Color(0xFFC0503A)
private val BowlRim = Color(0xFFD9774F)
private val Chip = listOf(Color(0xFFF2C14E), Color(0xFFE9B949), Color(0xFFF5D27E))

private const val TeeX = 470f
private const val GroundY = 866f
private const val PostX = 1560f
private const val CrossbarY = 792f
private const val UprightTop = 652f
private const val PostHalfWidth = 72f

val FieldGoal = Kick(
    cycleSeconds = 16f, flightSeconds = 3.4f, gapSeconds = 1.5f, offsetSeconds = 4f,
    fromX = TeeX, toX = 1820f, groundY = GroundY - 16f, apexHeight = 206f,
)

private val ScoreLabel = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xFB, 0xF6, 0xEC)
    textSize = 17f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.16f
}

private val ScoreDigits = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xF2, 0xA9, 0x3B)
    textSize = 46f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
}

private val PennantText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.WHITE
    textSize = 17f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

fun DrawScope.drawGameDayFigures(unit: Float, seconds: Float) {
    val kick = FieldGoal.poseAt(seconds)
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawScoreboard(240f, homeScore(kick.kicks))
        drawPennant(960f, seconds)
        drawChipBowl(1200f)
        // The ball passes in front of the far upright and behind the near one's streamer.
        drawGoalpost(seconds)
        drawTee(TeeX)
        if (kick.flying) {
            drawFootball(Offset(kick.x, kick.y), kick.spinDegrees + 20f, 1f)
        } else if (kick.teeAlpha > 0f) {
            drawFootball(Offset(TeeX, GroundY - 36f), -70f, kick.teeAlpha)
        }
    }
}

// A football, long way across at 0 degrees, with its laces.
private fun DrawScope.drawFootball(center: Offset, degrees: Float, alpha: Float) {
    withTransform({ rotate(degrees, pivot = center) }) {
        drawOval(Leather, Offset(center.x - 22f, center.y - 13f), Size(44f, 26f), alpha = alpha)
        drawOval(LeatherDark, Offset(center.x - 22f, center.y - 2f), Size(44f, 15f), alpha = 0.35f * alpha)
        drawLine(Lace, Offset(center.x - 15f, center.y - 8f), Offset(center.x - 11f, center.y - 8f), strokeWidth = 2f, alpha = alpha)
        drawLine(Lace, Offset(center.x + 11f, center.y - 8f), Offset(center.x + 15f, center.y - 8f), strokeWidth = 2f, alpha = alpha)
        drawLine(Lace, Offset(center.x - 8f, center.y - 7f), Offset(center.x + 8f, center.y - 7f), strokeWidth = 1.8f, alpha = alpha)
        listOf(-5f, 0f, 5f).forEach { dx ->
            drawLine(Lace, Offset(center.x + dx, center.y - 10f), Offset(center.x + dx, center.y - 4f), strokeWidth = 1.8f, alpha = alpha)
        }
    }
}

private fun DrawScope.drawTee(x: Float) {
    drawPath(Path().apply {
        moveTo(x - 12f, GroundY); lineTo(x + 12f, GroundY); lineTo(x + 6f, GroundY - 12f); lineTo(x - 6f, GroundY - 12f); close()
    }, TeeOrange)
    drawOval(TeeOrange, Offset(x - 9f, GroundY - 16f), Size(18f, 7f))
}

private fun DrawScope.drawGoalpost(seconds: Float) {
    // Gooseneck from the ground, the crossbar, two uprights with streamers on top.
    drawLine(PostShade, Offset(PostX + 10f, GroundY), Offset(PostX + 10f, CrossbarY + 34f), strokeWidth = 9f, cap = StrokeCap.Round)
    drawPath(Path().apply {
        moveTo(PostX + 10f, CrossbarY + 34f); quadraticTo(PostX + 10f, CrossbarY + 2f, PostX, CrossbarY)
    }, PostShade, style = Stroke(9f, cap = StrokeCap.Round))
    drawOval(BoardPost, Offset(PostX - 6f, GroundY - 4f), Size(32f, 8f), alpha = 0.4f)
    drawLine(PostYellow, Offset(PostX - PostHalfWidth, CrossbarY), Offset(PostX + PostHalfWidth, CrossbarY), strokeWidth = 7f, cap = StrokeCap.Round)
    listOf(-1f, 1f).forEach { side ->
        val x = PostX + side * PostHalfWidth
        drawLine(PostYellow, Offset(x, CrossbarY), Offset(x, UprightTop), strokeWidth = 6f, cap = StrokeCap.Round)
        val flutter = 6f * sin(seconds * 3.2f + side)
        drawPath(Path().apply {
            moveTo(x, UprightTop + 2f)
            quadraticTo(x + 12f, UprightTop - 4f + flutter * 0.5f, x + 28f, UprightTop + 4f + flutter)
            lineTo(x + 26f, UprightTop + 10f + flutter)
            quadraticTo(x + 12f, UprightTop + 6f + flutter * 0.5f, x, UprightTop + 10f)
            close()
        }, Streamer)
    }
}

private fun DrawScope.drawScoreboard(x: Float, home: Int) {
    val w = 220f
    val h = 104f
    val top = 708f
    drawLine(BoardPost, Offset(x - 60f, top + h), Offset(x - 60f, GroundY), strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(BoardPost, Offset(x + 60f, top + h), Offset(x + 60f, GroundY), strokeWidth = 7f, cap = StrokeCap.Round)
    drawRoundRect(Board, Offset(x - w / 2, top), Size(w, h), CornerRadius(10f))
    drawRoundRect(BoardEdge, Offset(x - w / 2 + 5f, top + 5f), Size(w - 10f, h - 10f), CornerRadius(7f), style = Stroke(2f))
    drawLine(BoardEdge, Offset(x, top + 16f), Offset(x, top + h - 16f), strokeWidth = 2f)
    drawIntoCanvas {
        it.nativeCanvas.drawText("HOME", x - 55f, top + 34f, ScoreLabel)
        it.nativeCanvas.drawText("GUEST", x + 55f, top + 34f, ScoreLabel)
        it.nativeCanvas.drawText(home.toString(), x - 55f, top + 84f, ScoreDigits)
        it.nativeCanvas.drawText("17", x + 55f, top + 84f, ScoreDigits)
    }
}

// A felt "GO TEAM!" pennant on a stick in the snow, rippling.
private fun DrawScope.drawPennant(x: Float, seconds: Float) {
    val top = GroundY - 168f
    drawLine(BoardPost, Offset(x, GroundY), Offset(x, top - 6f), strokeWidth = 5f, cap = StrokeCap.Round)
    drawCircle(PostYellow, 6f, Offset(x, top - 8f))
    val length = 150f
    val height = 58f
    fun ripple(along: Float) = 7f * along * sin(along * 5f - seconds * 3f)
    val steps = 12
    val pennant = Path().apply {
        moveTo(x + 2f, top)
        for (i in 1..steps) {
            val along = i.toFloat() / steps
            lineTo(x + 2f + along * length, top + along * height / 2 + ripple(along))
        }
        for (i in steps - 1 downTo 0) {
            val along = i.toFloat() / steps
            lineTo(x + 2f + along * length, top + height - along * height / 2 + ripple(along))
        }
        close()
    }
    drawPath(pennant, Felt)
    drawRect(PostYellow, Offset(x + 2f, top + 1f), Size(10f, height - 2f))
    withTransform({ rotate(Math.toDegrees(kotlin.math.atan2(ripple(0.45f) - ripple(0.25f), length * 0.2f).toDouble()).toFloat(), pivot = Offset(x + 66f, top + height / 2)) }) {
        drawIntoCanvas { it.nativeCanvas.drawText("GO TEAM!", x + 58f, top + height / 2 + 7f, PennantText) }
    }
}

private fun DrawScope.drawChipBowl(x: Float) {
    val rim = GroundY - 42f
    // Chips heaped above the rim, then the bowl in front of them.
    listOf(-34f to -10f, -18f to -18f, 0f to -22f, 18f to -16f, 34f to -8f, -8f to -6f, 12f to -4f).forEachIndexed { i, (dx, dy) ->
        val at = Offset(x + dx, rim + dy)
        withTransform({ rotate(i * 47f, pivot = at) }) {
            drawPath(Path().apply { moveTo(at.x, at.y - 11f); lineTo(at.x + 10f, at.y + 7f); lineTo(at.x - 10f, at.y + 7f); close() }, Chip[i % Chip.size])
        }
    }
    drawPath(Path().apply {
        moveTo(x - 52f, rim)
        lineTo(x + 52f, rim)
        quadraticTo(x + 48f, GroundY - 2f, x, GroundY - 2f)
        quadraticTo(x - 48f, GroundY - 2f, x - 52f, rim)
        close()
    }, Bowl)
    drawRoundRect(BowlRim, Offset(x - 54f, rim - 4f), Size(108f, 9f), CornerRadius(4f))
}
