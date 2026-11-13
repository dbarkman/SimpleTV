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
import kotlin.math.sin

// Pi Day (March 14th, which it takes from St. Patrick's stretch), in a room of its own
// (Rooms.kt): a cherry pie on a cake stand with a π cut in its crust and steam rising, the
// digits of pi running endlessly, and a compass drawing a circle beside it. The greeting says
// "Happy Pi Day!". Drawn in design px (1920x1080).

private val PieCrust = Color(0xFFE2A85E)
private val PieCrustDark = Color(0xFFC98A44)
private val Filling = Color(0xFFB8303A)
private val Stand = Color(0xFFE6EEF2)
private val StandShade = Color(0xFFC6D0D8)
private val Ribbon = Color(0xFF3A5A92)
private val RibbonDark = Color(0xFF2C4372)
private val Steam = Color(0xFFD6DCE2)
private val Chalk = Color(0xFF3A3A44)
private val CompassMetal = Color(0xFF8C9AA6)
private val Pencil = Color(0xFFE9C14A)

private const val RibbonLeft = 180f
private const val RibbonRight = 1740f
private const val RibbonY = 676f
private const val RibbonHeight = 40f

private val Digits = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xFB, 0xF6, 0xEC)
    textSize = 26f
    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    letterSpacing = 0.1f
}

private val PiMark = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xB8, 0x30, 0x3A)
    textSize = 40f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
}

// Each Pi Day room puts the digits and the compass where they fit: chalked on the classroom's
// board, on a shorter ribbon beside the bakery's sign, on a monitor in the computer room.
fun DrawScope.drawPiDayFigures(unit: Float, seconds: Float, room: Room? = null) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        when (room) {
            Room.Classroom -> {
                drawChalkDigits(seconds)
                drawCompassArm(Offset(1300f, 740f), 46f, seconds)
            }
            Room.Bakery -> {
                drawDigitRibbon(seconds, left = 300f, right = 1400f)
                drawCompass(1300f, 866f, seconds)
            }
            Room.ComputerRoom -> {
                drawDigitRibbon(seconds, left = 300f, right = 1620f)
                drawMonitor(1300f, 866f, seconds)
            }
            else -> {
                drawDigitRibbon(seconds)
                drawCompass(1300f, 866f, seconds)
            }
        }
        drawPie(800f, 866f, seconds)
    }
}

private val ChalkDigits = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.argb(0xDD, 0xF4, 0xF4, 0xEE)
    textSize = 30f
    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    letterSpacing = 0.12f
}

// The digits written in chalk across the top of the classroom board, scrolling.
private fun DrawScope.drawChalkDigits(seconds: Float) {
    val text = PiDigits + "   "
    val length = ChalkDigits.measureText(text)
    val shift = piScroll(seconds, length)
    clipRect(ClassBoardLeft + 16f, ClassBoardTop, ClassBoardRight - 16f, ClassBoardBottom) {
        drawIntoCanvas {
            var x = ClassBoardLeft + 20f - shift
            while (x < ClassBoardRight) {
                it.nativeCanvas.drawText(text, x, ClassBoardTop + 50f, ChalkDigits)
                x += length
            }
        }
    }
}

// A computer monitor on a stand, a green circle tracing itself on its dark screen.
private fun DrawScope.drawMonitor(x: Float, baseY: Float, seconds: Float) {
    drawRect(Color(0xFF8C9AA6), Offset(x - 8f, baseY - 40f), Size(16f, 40f))
    drawRoundRect(Color(0xFF8C9AA6), Offset(x - 50f, baseY - 8f), Size(100f, 10f), CornerRadius(4f))
    drawRoundRect(Color(0xFF3A414A), Offset(x - 106f, baseY - 176f), Size(212f, 140f), CornerRadius(8f))
    drawRect(Color(0xFF14201A), Offset(x - 96f, baseY - 166f), Size(192f, 112f))
    val center = Offset(x, baseY - 110f)
    val sweep = (seconds * 90f) % 420f
    val drawn = minOf(sweep, 360f)
    drawArc(Color(0xFF6DDB8A), -90f, drawn, useCenter = false, topLeft = Offset(center.x - 38f, center.y - 38f), size = Size(76f, 76f),
        style = Stroke(2.5f, cap = StrokeCap.Round), alpha = if (sweep > 360f) (420f - sweep) / 60f else 1f)
    drawLine(Color(0xFF6DDB8A), center, Offset(center.x + 38f, center.y), strokeWidth = 1.5f, alpha = 0.6f)
}

// The digits scroll right to left inside the ribbon; two copies end to end so it never runs out.
private fun DrawScope.drawDigitRibbon(seconds: Float, left: Float = RibbonLeft, right: Float = RibbonRight) {
    // Swallowtail ends.
    listOf(left to -1f, right to 1f).forEach { (x, dir) ->
        drawPath(Path().apply {
            moveTo(x, RibbonY + 6f); lineTo(x + dir * 40f, RibbonY + 6f); lineTo(x + dir * 26f, RibbonY + 26f)
            lineTo(x + dir * 40f, RibbonY + 46f); lineTo(x, RibbonY + 46f); close()
        }, RibbonDark)
    }
    drawRoundRect(Ribbon, Offset(left, RibbonY), Size(right - left, RibbonHeight), CornerRadius(4f))
    val text = PiDigits + "  "
    val length = Digits.measureText(text)
    val shift = piScroll(seconds, length)
    clipRect(left + 8f, RibbonY, right - 8f, RibbonY + RibbonHeight) {
        drawIntoCanvas {
            var x = left + 12f - shift
            while (x < right) {
                it.nativeCanvas.drawText(text, x, RibbonY + 29f, Digits)
                x += length
            }
        }
    }
}

private fun DrawScope.drawPie(x: Float, baseY: Float, seconds: Float) {
    // Cake stand: a foot, a stem and a plate.
    drawOval(StandShade, Offset(x - 50f, baseY - 12f), Size(100f, 14f))
    drawRect(Stand, Offset(x - 8f, baseY - 50f), Size(16f, 42f))
    drawOval(StandShade, Offset(x - 112f, baseY - 60f), Size(224f, 26f))
    drawOval(Stand, Offset(x - 112f, baseY - 64f), Size(224f, 24f))
    // The pie: side, top crust, crimped edge and the π.
    val top = baseY - 92f
    drawRoundRect(PieCrustDark, Offset(x - 92f, top + 6f), Size(184f, 30f), CornerRadius(10f))
    drawOval(PieCrust, Offset(x - 96f, top - 22f), Size(192f, 52f))
    drawOval(Filling, Offset(x - 74f, top - 12f), Size(148f, 34f), alpha = 0.25f)
    repeat(16) { i ->
        val a = Math.toRadians(i * 22.5)
        drawCircle(PieCrustDark, 6f, Offset(x + 92f * kotlin.math.cos(a).toFloat(), top + 4f + 24f * kotlin.math.sin(a).toFloat()), alpha = 0.6f)
    }
    withTransform({ scale(1f, 0.55f, pivot = Offset(x, top + 4f)) }) {
        drawIntoCanvas { it.nativeCanvas.drawText("π", x, top + 18f, PiMark) }
    }
    // Steam.
    repeat(3) { i ->
        val p = (seconds * 0.3f + i / 3f) % 1f
        val sx = x - 40f + i * 40f + 8f * sin(seconds * 1.4f + i)
        val sy = top - 24f - 80f * p
        drawPath(Path().apply { moveTo(sx, sy + 18f); quadraticTo(sx + 9f, sy + 9f, sx, sy); quadraticTo(sx - 9f, sy - 9f, sx, sy - 18f) },
            Steam, style = Stroke(4f, cap = StrokeCap.Round), alpha = 0.6f * sin(Math.PI.toFloat() * p))
    }
}

// A drawing compass on a little chalkboard, its pencil leg sweeping a circle over and over.
private fun DrawScope.drawCompass(x: Float, baseY: Float, seconds: Float) {
    val board = Offset(x - 90f, baseY - 150f)
    drawRect(Color(0xFF9C7A55), Offset(x - 70f, baseY - 30f), Size(8f, 30f))
    drawRect(Color(0xFF9C7A55), Offset(x + 62f, baseY - 30f), Size(8f, 30f))
    drawRoundRect(Color(0xFF9C7A55), Offset(board.x - 6f, board.y - 6f), Size(192f, 132f), CornerRadius(6f))
    drawRect(Chalk, board, Size(180f, 120f))
    drawCompassArm(Offset(x, baseY - 90f), 40f, seconds)
}

// The compass itself, its pencil leg sweeping a circle around center over and over.
private fun DrawScope.drawCompassArm(center: Offset, radius: Float, seconds: Float) {
    val sweep = (seconds * 90f) % 420f
    val drawn = minOf(sweep, 360f)
    drawArc(Color.White, -90f, drawn, useCenter = false, topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2),
        style = Stroke(2.5f, cap = StrokeCap.Round), alpha = 0.85f * if (sweep > 360f) (420f - sweep) / 60f else 1f)
    val angle = Math.toRadians((drawn - 90f).toDouble())
    val pencil = Offset(center.x + radius * kotlin.math.cos(angle).toFloat(), center.y + radius * kotlin.math.sin(angle).toFloat())
    val hinge = Offset((center.x + pencil.x) / 2, (center.y + pencil.y) / 2 - 46f)
    drawLine(CompassMetal, hinge, center, strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(CompassMetal, hinge, pencil, strokeWidth = 4f, cap = StrokeCap.Round)
    drawCircle(Pencil, 4f, pencil)
    drawCircle(CompassMetal, 6f, hinge)
    drawRoundRect(CompassMetal, Offset(hinge.x - 3f, hinge.y - 18f), Size(6f, 14f), CornerRadius(3f))
}
