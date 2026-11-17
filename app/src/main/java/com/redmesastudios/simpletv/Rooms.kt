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

// Indoor backdrops for holidays that suit a room better than the season's hills. Each fills
// the whole screen: a pale wall behind the header and apps (so their dark text still reads
// as on the plain background), the room's character along the band between the apps and the
// cards, a surface at y 866-880 for the holiday's things to stand on, and a feature at each
// edge where the trees stand outdoors. drawRoom is the still part; drawRoomMotion, on the
// moving layer, is what flickers. Drawn in design px (1920x1080).

const val CounterY = 868f

fun DrawScope.drawRoom(room: Room, unit: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        when (room) {
            Room.Pizzeria -> drawPizzeria()
            Room.Classroom -> drawClassroom()
            else -> drawPlace(room)
        }
    }
}

fun DrawScope.drawRoomMotion(room: Room, unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        when (room) {
            Room.Pizzeria -> drawOvenFire(seconds)
            else -> drawPlaceMotion(room, seconds)
        }
    }
}

private fun textPaint(color: Int, size: Float, face: Typeface = Typeface.DEFAULT, bold: Boolean = true, align: android.graphics.Paint.Align = android.graphics.Paint.Align.CENTER) =
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        textAlign = align
        typeface = Typeface.create(face, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

// A wooden counter top along the band's floor, with its front running down behind the cards.
private fun DrawScope.drawCounter(top: Color, front: Color, edge: Color) {
    drawRect(front, Offset(0f, CounterY + 12f), Size(SceneWidth, SceneHeight - CounterY))
    drawRect(top, Offset(0f, CounterY - 2f), Size(SceneWidth, 16f))
    drawRect(edge, Offset(0f, CounterY + 12f), Size(SceneWidth, 3f))
}

// ---- Pizzeria (Pizza Day) ----

private val PizzeriaWall = Color(0xFFF5EADF)
private val Brick = Color(0xFFEBCDB8)
private val Mortar = Color(0xFFF6E6DA)
private val Shelf = Color(0xFFB98A5E)
private val CheckRed = Color(0xFFEDBDB2)
private val CheckCream = Color(0xFFF9F0E8)
private val OvenBrick = Color(0xFFC98E6E)
private val OvenDark = Color(0xFF8C5A42)
private val OvenMouth = Color(0xFF3A2620)
private val Chalkboard = Color(0xFF3E4A44)
private val ChalkFrame = Color(0xFF9C7A55)

private val MenuTitle = textPaint(android.graphics.Color.rgb(0xF7, 0xD2, 0x7A), 24f)
private val MenuItem = textPaint(android.graphics.Color.rgb(0xF4, 0xEC, 0xE1), 17f, bold = false)

private fun DrawScope.drawPizzeria() {
    drawRect(PizzeriaWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    // A brick wainscot behind the band, under a wooden ledge.
    val brickTop = 610f
    drawRect(Mortar, Offset(0f, brickTop), Size(SceneWidth, CounterY - brickTop))
    val rowHeight = 32f
    var row = 0
    var y = brickTop + 3f
    while (y < CounterY) {
        val shift = if (row % 2 == 0) 0f else 50f
        var x = -shift
        while (x < SceneWidth) {
            drawRoundRect(Brick, Offset(x + 3f, y), Size(94f, rowHeight - 6f), CornerRadius(3f))
            x += 100f
        }
        y += rowHeight
        row++
    }
    drawRect(Shelf, Offset(0f, brickTop - 10f), Size(SceneWidth, 12f))
    // Checkered tablecloth hanging down the counter front.
    drawRect(Shelf, Offset(0f, CounterY - 2f), Size(SceneWidth, 14f))
    val check = 36f
    var cy = CounterY + 12f
    var r = 0
    while (cy < SceneHeight) {
        var cx = 0f
        var c = r % 2
        while (cx < SceneWidth) {
            drawRect(if (c % 2 == 0) CheckRed else CheckCream, Offset(cx, cy), Size(check, check))
            cx += check
            c++
        }
        cy += check
        r++
    }
    drawPizzaOven(110f)
    drawMenuBoard(1810f)
}

// A domed brick oven at the left edge, its mouth dark until the fire lights it.
private fun DrawScope.drawPizzaOven(x: Float) {
    val base = CounterY
    drawRect(OvenDark, Offset(x - 120f, base - 70f), Size(240f, 70f))
    drawArc(OvenBrick, 180f, 180f, useCenter = true, topLeft = Offset(x - 120f, base - 250f), size = Size(240f, 360f))
    // Brick courses around the dome.
    listOf(0.35f, 0.6f, 0.82f).forEach { k ->
        drawArc(OvenDark, 180f, 180f, useCenter = false, topLeft = Offset(x - 120f * k, base - 70f - 180f * k), size = Size(240f * k, 360f * k),
            style = Stroke(2f), alpha = 0.35f)
    }
    drawArc(OvenMouth, 180f, 180f, useCenter = true, topLeft = Offset(x - 52f, base - 128f), size = Size(104f, 116f))
    drawRect(OvenDark, Offset(x - 60f, base - 72f), Size(120f, 8f))
    // Chimney.
    drawRect(OvenDark, Offset(x - 18f, base - 300f), Size(36f, 70f))
}

private fun DrawScope.drawOvenFire(seconds: Float) {
    val x = 110f
    val base = CounterY - 72f
    val glow = candleGlow(seconds, 0.5f)
    drawCircle(Color(0xFFF2A93B), 60f * glow, Offset(x, base - 20f), alpha = 0.25f)
    listOf(-22f to 0f, 0f to 1.7f, 20f to 3.1f).forEach { (dx, phase) ->
        val h = 34f + 10f * sin(seconds * 7f + phase)
        drawPath(Path().apply {
            moveTo(x + dx - 12f, base)
            quadraticTo(x + dx - 10f, base - h * 0.6f, x + dx + 2f * sin(seconds * 5f + phase), base - h)
            quadraticTo(x + dx + 10f, base - h * 0.6f, x + dx + 12f, base)
            close()
        }, Color(0xFFE8793A), alpha = 0.9f)
        drawPath(Path().apply {
            moveTo(x + dx - 6f, base)
            quadraticTo(x + dx - 4f, base - h * 0.4f, x + dx, base - h * 0.6f)
            quadraticTo(x + dx + 4f, base - h * 0.4f, x + dx + 6f, base)
            close()
        }, Color(0xFFF7D27A))
    }
}

private fun DrawScope.drawMenuBoard(x: Float) {
    val top = 640f
    drawRoundRect(ChalkFrame, Offset(x - 106f, top - 6f), Size(212f, 212f), CornerRadius(6f))
    drawRect(Chalkboard, Offset(x - 100f, top), Size(200f, 200f))
    drawIntoCanvas {
        it.nativeCanvas.drawText("MENU", x, top + 36f, MenuTitle)
        listOf("Margherita", "Pepperoni", "Veggie", "Calzone").forEachIndexed { i, item ->
            it.nativeCanvas.drawText(item, x, top + 76f + i * 30f, MenuItem)
        }
    }
}

// ---- Classroom (Pi Day) ----

private val ClassWall = Color(0xFFEEF0E6)
private val Board = Color(0xFF3F5A4C)
private val BoardFrame = Color(0xFFB08A62)
private val Floor = Color(0xFFD9C3A2)
private val FloorLine = Color(0xFFC8B08C)
private val Desk = Color(0xFFB98A5E)
private val DeskDark = Color(0xFF8C6A4F)
private val GlobeSea = Color(0xFF7FB7D9)
private val GlobeLand = Color(0xFF9BCB8C)
private val BookColors = listOf(Color(0xFFC0503A), Color(0xFF3A5A92), Color(0xFFE9C14A), Color(0xFF5E8C61), Color(0xFF8C5A9C))

private val ChalkText = textPaint(android.graphics.Color.argb(0xDD, 0xF4, 0xF4, 0xEE), 28f, Typeface.SERIF, bold = false)

const val ClassBoardLeft = 300f
const val ClassBoardRight = 1620f
const val ClassBoardTop = 616f
const val ClassBoardBottom = 836f

private fun DrawScope.drawClassroom() {
    drawRect(ClassWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    // Floorboards.
    drawRect(Floor, Offset(0f, 900f), Size(SceneWidth, SceneHeight - 900f))
    var x = 0f
    while (x < SceneWidth) { drawLine(FloorLine, Offset(x, 900f), Offset(x, SceneHeight), strokeWidth = 2f); x += 90f }
    drawRect(Color(0xFFCFC6B6), Offset(0f, 892f), Size(SceneWidth, 10f))
    // The chalkboard, its tray and a few formulas.
    drawRoundRect(BoardFrame, Offset(ClassBoardLeft - 12f, ClassBoardTop - 12f), Size(ClassBoardRight - ClassBoardLeft + 24f, ClassBoardBottom - ClassBoardTop + 24f), CornerRadius(6f))
    drawRect(Board, Offset(ClassBoardLeft, ClassBoardTop), Size(ClassBoardRight - ClassBoardLeft, ClassBoardBottom - ClassBoardTop))
    drawRect(BoardFrame, Offset(ClassBoardLeft - 20f, ClassBoardBottom + 10f), Size(ClassBoardRight - ClassBoardLeft + 40f, 10f))
    listOf(ClassBoardLeft + 80f, ClassBoardLeft + 96f, ClassBoardRight - 140f).forEachIndexed { i, cx ->
        drawRoundRect(if (i == 2) Color(0xFFF4A6B8) else Color(0xFFF4F4EE), Offset(cx, ClassBoardBottom + 4f), Size(14f, 6f), CornerRadius(2f))
    }
    drawIntoCanvas {
        it.nativeCanvas.drawText("C = 2πr", ClassBoardLeft + 120f, ClassBoardTop + 120f, ChalkText)
        it.nativeCanvas.drawText("A = πr²", ClassBoardLeft + 120f, ClassBoardTop + 170f, ChalkText)
        it.nativeCanvas.drawText("π ≈ 22/7", ClassBoardRight - 380f, ClassBoardTop + 160f, ChalkText)
    }
    // The teacher's desk, which the pie sits on.
    drawRect(DeskDark, Offset(640f, CounterY + 10f), Size(16f, 90f))
    drawRect(DeskDark, Offset(944f, CounterY + 10f), Size(16f, 90f))
    drawRect(Desk, Offset(620f, CounterY - 2f), Size(360f, 16f))
    drawGlobe(130f)
    drawBookshelf(1800f)
}

private fun DrawScope.drawGlobe(x: Float) {
    val center = Offset(x, 760f)
    drawRect(DeskDark, Offset(x - 50f, CounterY + 10f), Size(100f, 14f))
    drawLine(DeskDark, Offset(x, CounterY + 10f), Offset(x, center.y + 60f), strokeWidth = 8f)
    drawArc(BoardFrame, 100f, 250f, useCenter = false, topLeft = Offset(center.x - 74f, center.y - 74f), size = Size(148f, 148f), style = Stroke(6f))
    drawCircle(GlobeSea, 62f, center)
    drawOval(GlobeLand, Offset(center.x - 40f, center.y - 36f), Size(40f, 50f))
    drawOval(GlobeLand, Offset(center.x + 8f, center.y - 10f), Size(34f, 44f))
    drawOval(GlobeLand, Offset(center.x - 20f, center.y + 30f), Size(30f, 16f))
}

private fun DrawScope.drawBookshelf(x: Float) {
    val top = 620f
    drawRect(DeskDark, Offset(x - 100f, top), Size(200f, CounterY + 30f - top))
    drawRect(Color(0xFF6E5440), Offset(x - 90f, top + 10f), Size(180f, CounterY + 10f - top))
    listOf(top + 90f, top + 180f, top + 260f).forEach { shelf ->
        drawRect(DeskDark, Offset(x - 90f, shelf), Size(180f, 8f))
        var bx = x - 86f
        var k = (shelf / 7f).toInt()
        while (bx < x + 80f) {
            val w = 14f + (k % 3) * 4f
            val h = 56f + (k % 4) * 6f
            drawRect(BookColors[k % BookColors.size], Offset(bx, shelf - h), Size(w, h))
            bx += w + 2f
            k++
        }
    }
}
