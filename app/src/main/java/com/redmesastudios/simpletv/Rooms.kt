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
// moving layer, is what flickers or blinks. Drawn in design px (1920x1080).

const val CounterY = 868f

fun DrawScope.drawRoom(room: Room, unit: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        when (room) {
            Room.Pizzeria -> drawPizzeria()
            Room.Classroom -> drawClassroom()
            Room.Bakery -> drawBakery()
            Room.ComputerRoom -> drawComputerRoom()
        }
    }
}

fun DrawScope.drawRoomMotion(room: Room, unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        when (room) {
            Room.Pizzeria -> drawOvenFire(seconds)
            Room.ComputerRoom -> drawServerLights(seconds)
            else -> {}
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

// ---- Bakery (Pi Day) ----

private val BakeryWall = Color(0xFFF8EEE8)
private val Stripe = Color(0xFFF2E0DA)
private val BakeryCounter = Color(0xFFE8B4A6)
private val BakeryCounterTop = Color(0xFFFBF6EC)
private val BakeryShelf = Color(0xFFC9A16E)
private val PieTop = Color(0xFFE2A85E)
private val PieDish = Color(0xFFCFD8DE)
private val Awning = listOf(Color(0xFFE8796B), Color(0xFFFBF6EC))
private val SignBlue = Color(0xFF3A5A92)

private val ShopSign = textPaint(android.graphics.Color.rgb(0xFB, 0xF6, 0xEC), 26f)

private fun DrawScope.drawBakery() {
    drawRect(BakeryWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    // Striped wallpaper behind the band.
    var x = 0f
    var k = 0
    while (x < SceneWidth) {
        if (k % 2 == 0) drawRect(Stripe, Offset(x, 600f), Size(40f, CounterY - 600f))
        x += 40f
        k++
    }
    // A little scalloped awning over the band.
    var ax = 0f
    var a = 0
    while (ax < SceneWidth) {
        drawRect(Awning[a % 2], Offset(ax, 586f), Size(60f, 24f))
        drawArc(Awning[a % 2], 0f, 180f, useCenter = true, topLeft = Offset(ax, 596f), size = Size(60f, 28f))
        ax += 60f
        a++
    }
    // The shop counter: a cream top over a pink front with panels.
    drawRect(BakeryCounter, Offset(0f, CounterY + 12f), Size(SceneWidth, SceneHeight - CounterY))
    var px = 30f
    while (px < SceneWidth) {
        drawRoundRect(Color(0xFFF0C7BC), Offset(px, CounterY + 34f), Size(150f, 150f), CornerRadius(8f))
        px += 190f
    }
    drawRect(BakeryCounterTop, Offset(0f, CounterY - 2f), Size(SceneWidth, 16f))
    drawPieShelf(120f)
    drawPieShelf(1800f)
    // The shop's sign on the wall, above the counter at the right.
    drawRoundRect(SignBlue, Offset(1440f, 640f), Size(230f, 60f), CornerRadius(30f))
    drawIntoCanvas { it.nativeCanvas.drawText("PIE SHOP", 1555f, 680f, ShopSign) }
}

// A wall shelf of three boards, a pie on each.
private fun DrawScope.drawPieShelf(x: Float) {
    listOf(680f, 770f, 860f).forEach { shelf ->
        drawRect(BakeryShelf, Offset(x - 90f, shelf), Size(180f, 10f))
        listOf(-46f, 46f).forEach { dx ->
            val c = Offset(x + dx, shelf - 14f)
            drawOval(PieDish, Offset(c.x - 36f, c.y - 2f), Size(72f, 16f))
            drawOval(PieTop, Offset(c.x - 32f, c.y - 14f), Size(64f, 22f))
            drawLine(Color(0xFFC98A44), Offset(c.x - 12f, c.y - 8f), Offset(c.x + 12f, c.y - 2f), strokeWidth = 2f)
            drawLine(Color(0xFFC98A44), Offset(c.x - 12f, c.y - 2f), Offset(c.x + 12f, c.y - 8f), strokeWidth = 2f)
        }
    }
}

// ---- Computer room (Pi Day) ----

private val CompWall = Color(0xFFEAEEF2)
private val Panel = Color(0xFFDDE3E9)
private val Rack = Color(0xFF3A414A)
private val RackSlot = Color(0xFF2C3238)
private val DeskGray = Color(0xFFB8C2CC)
private val TileFloor = Color(0xFFD3DAE1)
private val TileLine = Color(0xFFC2CAD3)

private fun DrawScope.drawComputerRoom() {
    drawRect(CompWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    // Wall panels along the band.
    var x = 0f
    while (x < SceneWidth) {
        drawRoundRect(Panel, Offset(x + 6f, 610f), Size(228f, CounterY - 620f), CornerRadius(6f))
        x += 240f
    }
    // A raised tile floor below a long desk.
    drawRect(TileFloor, Offset(0f, CounterY + 12f), Size(SceneWidth, SceneHeight - CounterY))
    var tx = 0f
    while (tx < SceneWidth) { drawLine(TileLine, Offset(tx, CounterY + 12f), Offset(tx, SceneHeight), strokeWidth = 2f); tx += 120f }
    var ty = CounterY + 70f
    while (ty < SceneHeight) { drawLine(TileLine, Offset(0f, ty), Offset(SceneWidth, ty), strokeWidth = 2f); ty += 60f }
    drawRect(DeskGray, Offset(240f, CounterY - 2f), Size(SceneWidth - 480f, 14f))
    drawServerRack(110f)
    drawServerRack(1810f)
}

private fun DrawScope.drawServerRack(x: Float) {
    val top = 560f
    drawRoundRect(Rack, Offset(x - 100f, top), Size(200f, SceneHeight - top), CornerRadius(6f))
    var y = top + 16f
    while (y < CounterY + 40f) {
        drawRoundRect(RackSlot, Offset(x - 86f, y), Size(172f, 30f), CornerRadius(3f))
        y += 38f
    }
}

private val LedColors = listOf(Color(0xFF6DBE7B), Color(0xFF5FA8D3), Color(0xFFF2C14E))

// Status lights on the racks, each blinking on its own rhythm.
private fun DrawScope.drawServerLights(seconds: Float) {
    listOf(110f, 1810f).forEachIndexed { r, x ->
        var y = 576f
        var i = r * 17
        while (y < CounterY + 40f) {
            repeat(3) { k ->
                val on = sin(seconds * (2f + (i + k) % 5) + (i * 3 + k) * 1.3f) > 0.2f
                drawCircle(LedColors[(i + k) % LedColors.size], 3.5f, Offset(x + 40f + k * 14f, y + 15f), alpha = if (on) 1f else 0.25f)
            }
            drawRect(Color(0xFF4A525C), Offset(x - 76f, y + 12f), Size(80f, 6f))
            y += 38f
            i++
        }
    }
}
