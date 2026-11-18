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
import androidx.compose.ui.graphics.nativeCanvas

// More places (see Places.kt), for early fall: a diner (Cheeseburger Day), a cove with a
// ship on the horizon (Talk Like a Pirate Day), a beer garden under the Alps (Oktoberfest), a
// café (Coffee Day), a street with a taco truck (Taco Day) and a cozy room with a window seat
// (Cat Day). Drawn in design px (1920x1080).

private fun sign(color: Int, size: Float, face: Typeface = Typeface.DEFAULT, style: Int = Typeface.BOLD, spacing: Float = 0.1f) =
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = Typeface.create(face, style)
        letterSpacing = spacing
    }

// ---- Diner (Cheeseburger Day) ----

private val DinerWall = Color(0xFFF4ECE1)
private val DinerTeal = Color(0xFFB8DDD6)
private val Chrome = Color(0xFFD5DCE2)
private val Booth = Color(0xFFD9675C)
private val Check = listOf(Color(0xFF2E2A28), Color(0xFFF7F3EC))
private val DinerSign = sign(android.graphics.Color.rgb(0xD9, 0x53, 0x4F), 42f, Typeface.SANS_SERIF, Typeface.BOLD_ITALIC, 0.2f)

private fun DrawScope.drawDiner() {
    drawRect(DinerWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    drawRect(DinerTeal, Offset(0f, 620f), Size(SceneWidth, CounterY - 620f))
    drawRect(Chrome, Offset(0f, 612f), Size(SceneWidth, 10f))
    drawRect(Chrome, Offset(0f, 760f), Size(SceneWidth, 6f))
    // Red booths along the wall, a neon DINER sign over them.
    listOf(260f, 1440f).forEach { x ->
        drawRoundRect(Booth, Offset(x, 680f), Size(220f, CounterY - 680f), CornerRadius(18f, 18f))
        drawRect(Color(0xFFB8453B), Offset(x, 680f), Size(220f, 14f))
    }
    drawRoundRect(Color(0xFFD9534F), Offset(820f, 636f), Size(280f, 66f), CornerRadius(33f), style = Stroke(4f))
    drawIntoCanvas { it.nativeCanvas.drawText("DINER", 960f, 684f, DinerSign) }
    // Chrome-edged counter and a checkerboard floor.
    drawRect(Color(0xFFD9675C), Offset(0f, CounterY + 12f), Size(SceneWidth, 40f))
    var y = CounterY + 52f
    var r = 0
    while (y < SceneHeight) {
        var x = 0f
        var c = r % 2
        while (x < SceneWidth) { drawRect(Check[c % 2], Offset(x, y), Size(44f, 44f), alpha = 0.85f); x += 44f; c++ }
        y += 44f
        r++
    }
    drawRect(Chrome, Offset(0f, CounterY - 2f), Size(SceneWidth, 16f))
}

// ---- Pirate cove (Talk Like a Pirate Day) ----

private val SeaMid = Color(0xFFE2EEF0)
private val SeaLow = Color(0xFFCFE3E8)
private val Sea = Color(0xFF8DBCCB)
private val SeaLight = Color(0xFFB3D5DE)
private val Sand = Color(0xFFEBD6AE)
private val Hull = Color(0xFF8C5A3A)
private val Sail = Color(0xFFFBF6EC)

private fun DrawScope.drawCove() {
    drawSky(SeaMid, SeaLow)
    drawRect(Sea, Offset(0f, 790f), Size(SceneWidth, CounterY - 790f))
    // A ship on the horizon.
    val sx = 1320f
    drawPath(Path().apply { moveTo(sx - 70f, 790f); lineTo(sx + 70f, 790f); lineTo(sx + 52f, 812f); lineTo(sx - 52f, 812f); close() }, Hull)
    drawLine(Hull, Offset(sx, 790f), Offset(sx, 700f), strokeWidth = 4f)
    drawPath(Path().apply { moveTo(sx + 2f, 706f); lineTo(sx + 50f, 760f); lineTo(sx + 2f, 770f); close() }, Sail)
    drawPath(Path().apply { moveTo(sx - 2f, 716f); lineTo(sx - 44f, 770f); lineTo(sx - 2f, 778f); close() }, Sail)
    drawRect(Color(0xFF2E2A28), Offset(sx, 690f), Size(22f, 12f))
    drawRect(Sand, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    drawPath(Path().apply { moveTo(0f, CounterY); cubicTo(500f, 852f, 1300f, 858f, SceneWidth, CounterY); lineTo(SceneWidth, CounterY + 10f); lineTo(0f, CounterY + 10f); close() }, Sand)
    drawPalmTree(100f)
    drawPalmTree(1820f)
}

private fun DrawScope.drawPalmTree(x: Float) {
    val top = Offset(x + 30f, 580f)
    drawPath(Path().apply { moveTo(x, CounterY + 40f); quadraticTo(x - 8f, 760f, top.x, top.y) }, Color(0xFF9C7A55), style = Stroke(16f, cap = StrokeCap.Round))
    listOf(-150f, -110f, -70f, -30f, 10f, 200f).forEach { angle ->
        val a = Math.toRadians(angle.toDouble())
        val tip = Offset(top.x + 110f * kotlin.math.cos(a).toFloat(), top.y + 60f * kotlin.math.sin(a).toFloat() + 30f)
        drawPath(Path().apply { moveTo(top.x, top.y); quadraticTo((top.x + tip.x) / 2, (top.y + tip.y) / 2 - 26f, tip.x, tip.y); quadraticTo((top.x + tip.x) / 2, (top.y + tip.y) / 2 - 6f, top.x, top.y); close() }, Color(0xFF6E9C6D))
    }
}

// Waves lapping in the cove.
private fun DrawScope.drawWaves(seconds: Float) {
    repeat(8) { i ->
        val x = (i * 260f + seconds * 20f) % (SceneWidth + 200f) - 100f
        val y = 812f + (i % 3) * 16f
        drawLine(SeaLight, Offset(x, y), Offset(x + 80f, y), strokeWidth = 4f, cap = StrokeCap.Round, alpha = 0.8f)
    }
}

// ---- Beer garden (Oktoberfest) ----

private val AlpsMid = Color(0xFFE3ECF2)
private val AlpsLow = Color(0xFFD5E2EB)
private val Mountain = Color(0xFFC3CDD8)
private val Snowcap = Color(0xFFF7F9FA)
private val Timber = Color(0xFF7A5A40)
private val Plaster = Color(0xFFF6EFE4)
private val Gravel = Color(0xFFDDD3C2)
private val BavarianBlue = Color(0xFF6FA0D8)

private fun DrawScope.drawBeerGarden() {
    drawSky(AlpsMid, AlpsLow)
    // The Alps far off.
    listOf(Triple(300f, 640f, 260f), Triple(760f, 610f, 300f), Triple(1240f, 630f, 280f), Triple(1660f, 650f, 240f)).forEach { (x, top, half) ->
        drawPath(Path().apply { moveTo(x - half, 820f); lineTo(x, top); lineTo(x + half, 820f); close() }, Mountain)
        drawPath(Path().apply { moveTo(x - half * 0.25f, top + 50f); lineTo(x, top); lineTo(x + half * 0.25f, top + 50f); lineTo(x + 10f, top + 40f); lineTo(x - 12f, top + 46f); close() }, Snowcap)
    }
    // Half-timbered houses at both edges.
    listOf(0f, 1700f).forEach { x ->
        drawRect(Plaster, Offset(x, 640f), Size(220f, CounterY - 640f))
        drawPath(Path().apply { moveTo(x - 10f, 646f); lineTo(x + 110f, 560f); lineTo(x + 230f, 646f); close() }, Color(0xFFB8453B))
        listOf(0f, 70f, 140f, 210f).forEach { dx -> drawRect(Timber, Offset(x + dx, 640f), Size(10f, CounterY - 640f)) }
        drawLine(Timber, Offset(x, 700f), Offset(x + 70f, 760f), strokeWidth = 8f)
        drawLine(Timber, Offset(x + 140f, 760f), Offset(x + 210f, 700f), strokeWidth = 8f)
        drawRect(Timber, Offset(x, 760f), Size(220f, 10f))
        drawRect(BavarianBlue, Offset(x + 84f, 780f), Size(46f, 40f), alpha = 0.6f)
    }
    drawRect(Gravel, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    // Long wooden tables.
    drawRect(Color(0xFFB98A5E), Offset(260f, CounterY - 2f), Size(1400f, 16f))
}

// ---- Café (Coffee Day) ----

private val CafeWall = Color(0xFFF4ECE1)
private val CafeBrick = Color(0xFFE6D2BF)
private val CafeBoard = Color(0xFF3E4A44)
private val Espresso = Color(0xFFB8C0C8)
private val CafeCounter = Color(0xFF8C6A4F)
private val CafeTitle = sign(android.graphics.Color.rgb(0xF7, 0xD2, 0x7A), 26f, spacing = 0.2f)
private val CafeItem = sign(android.graphics.Color.rgb(0xF4, 0xEC, 0xE1), 17f, style = Typeface.NORMAL, spacing = 0f)

private fun DrawScope.drawCafe() {
    drawRect(CafeWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    var y = 620f
    var row = 0
    while (y < CounterY) {
        var x = if (row % 2 == 0) 0f else -40f
        while (x < SceneWidth) { drawRoundRect(CafeBrick, Offset(x + 3f, y + 3f), Size(74f, 22f), CornerRadius(3f)); x += 80f }
        y += 28f
        row++
    }
    // A chalk menu on the wall.
    drawRoundRect(Color(0xFF9C7A55), Offset(1480f, 630f), Size(300f, 200f), CornerRadius(6f))
    drawRect(CafeBoard, Offset(1488f, 638f), Size(284f, 184f))
    drawIntoCanvas {
        it.nativeCanvas.drawText("COFFEE", 1630f, 676f, CafeTitle)
        listOf("Espresso", "Latte", "Cappuccino", "Cold brew").forEachIndexed { i, item -> it.nativeCanvas.drawText(item, 1630f, 712f + i * 28f, CafeItem) }
    }
    // An espresso machine at the left edge.
    drawRoundRect(Espresso, Offset(30f, CounterY - 180f), Size(200f, 180f), CornerRadius(14f))
    drawRect(Color(0xFF8C9AA6), Offset(30f, CounterY - 196f), Size(200f, 20f))
    listOf(80f, 180f).forEach { x -> drawRect(Color(0xFF4A4440), Offset(x - 8f, CounterY - 100f), Size(16f, 26f)) }
    drawCircle(Color(0xFF4A4440), 16f, Offset(130f, CounterY - 140f))
    drawRect(CafeCounter, Offset(0f, CounterY + 12f), Size(SceneWidth, SceneHeight - CounterY))
    drawRect(Color(0xFFB98A5E), Offset(0f, CounterY - 2f), Size(SceneWidth, 16f))
}

// ---- Taco truck (Taco Day) ----

private val StreetMid = Color(0xFFF4E6D4)
private val StreetLow = Color(0xFFEBD6BC)
private val TruckBody = Color(0xFFF2C14E)
private val TruckTrim = Color(0xFFE8517A)
private val TruckWindow = Color(0xFF3A3A44)
private val Sidewalk = Color(0xFFDCD2C4)
private val TacoSign = sign(android.graphics.Color.rgb(0xE8, 0x51, 0x7A), 40f, Typeface.SANS_SERIF, Typeface.BOLD, 0.15f)
private val BulbColors = listOf(Color(0xFFE8517A), Color(0xFFF2D24E), Color(0xFF6DBE7B), Color(0xFF5FA8D3))

private fun DrawScope.drawTacoTruck() {
    drawSky(StreetMid, StreetLow)
    // The truck, parked behind the band: body, service window, awning, wheels.
    val left = 520f
    val right = 1400f
    drawRoundRect(TruckBody, Offset(left, 640f), Size(right - left, CounterY - 640f), CornerRadius(20f))
    drawRect(TruckTrim, Offset(left, 820f), Size(right - left, 14f))
    drawRect(TruckWindow, Offset(left + 160f, 684f), Size(500f, 100f))
    var ax = left + 150f
    var k = 0
    while (ax < left + 670f) { drawRect(if (k % 2 == 0) TruckTrim else Color(0xFFFBF6EC), Offset(ax, 664f), Size(40f, 22f)); ax += 40f; k++ }
    drawIntoCanvas { it.nativeCanvas.drawText("TACOS", right - 110f, 748f, TacoSign) }
    drawRect(Sidewalk, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    drawEdgeTree(110f, Color(0xFF9C7A55), listOf(Color(0xFFC9B47A), Color(0xFFD9C48A), Color(0xFFB8A46A)))
    drawEdgeTree(1810f, Color(0xFF9C7A55), listOf(Color(0xFFD9C48A), Color(0xFFC9B47A), Color(0xFFB8A46A)))
}

// String lights over the truck, twinkling.
private fun DrawScope.drawStringBulbs(seconds: Float) {
    var i = 0
    var x = 260f
    while (x < 1680f) {
        val t = (x - 260f) / 1420f
        val y = 610f + 60f * 4f * t * (1f - t) * 0.5f
        val glow = twinkle(seconds, i)
        drawCircle(BulbColors[i % BulbColors.size], 9f, Offset(x, y + 10f), alpha = 0.25f * glow)
        drawCircle(BulbColors[i % BulbColors.size], 5f, Offset(x, y + 10f), alpha = 0.5f + 0.5f * glow)
        x += 44f
        i++
    }
}

// ---- Cozy room with a window seat (Cat Day) ----

private val CozyWall = Color(0xFFF2E9DE)
private val WindowView = Color(0xFFE9DCC6)
private val FallTrees = listOf(Color(0xFFE2A85E), Color(0xFFD9775A), Color(0xFFC9A04E))
private val SeatCushion = Color(0xFF8FA6C9)
private val Shelf = Color(0xFFB98A5E)

private fun DrawScope.drawCatRoom() {
    drawRect(CozyWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    // A big window onto fall trees, with a cushioned seat below it.
    val left = 640f
    val width = 640f
    drawRect(WindowView, Offset(left, 620f), Size(width, 200f))
    listOf(left + 80f, left + 230f, left + 400f, left + 560f).forEachIndexed { i, x -> drawCircle(FallTrees[i % 3], 60f, Offset(x, 760f)) }
    drawRect(Color(0xFFFBF8F2), Offset(left - 12f, 610f), Size(width + 24f, 12f))
    drawRect(Color(0xFFFBF8F2), Offset(left + width / 2 - 5f, 610f), Size(10f, 210f))
    drawRect(Color(0xFFFBF8F2), Offset(left - 12f, 610f), Size(12f, 210f))
    drawRect(Color(0xFFFBF8F2), Offset(left + width, 610f), Size(12f, 210f))
    drawRect(Color(0xFFFBF8F2), Offset(left - 20f, 818f), Size(width + 40f, 12f))
    drawRoundRect(SeatCushion, Offset(left - 20f, 830f), Size(width + 40f, 38f), CornerRadius(12f))
    drawRect(Color(0xFFD9C3A2), Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    // A cat tree at the left edge and a bookshelf at the right.
    drawRect(Color(0xFFC9B49A), Offset(90f, 640f), Size(24f, CounterY - 640f))
    listOf(640f, 740f, 830f).forEach { y -> drawRoundRect(Color(0xFFB0A08C), Offset(50f, y), Size(110f, 20f), CornerRadius(8f)) }
    drawRect(Shelf, Offset(1720f, 620f), Size(180f, CounterY - 620f))
    listOf(700f, 780f, 860f).forEach { y ->
        drawRect(Color(0xFF8C6A4F), Offset(1720f, y), Size(180f, 6f))
        var bx = 1728f
        var k = (y / 10).toInt()
        while (bx < 1890f) { drawRect(FallTrees[k % 3], Offset(bx, y - 56f), Size(16f, 56f)); bx += 20f; k++ }
    }
}

// ---- Dispatch ----

fun DrawScope.drawAutumnPlace(room: Room) {
    when (room) {
        Room.Diner -> drawDiner()
        Room.Cove -> drawCove()
        Room.BeerGarden -> drawBeerGarden()
        Room.Cafe -> drawCafe()
        Room.TacoTruck -> drawTacoTruck()
        Room.CatRoom -> drawCatRoom()
        else -> drawNovemberPlace(room)
    }
}

fun DrawScope.drawAutumnPlaceMotion(room: Room, seconds: Float) {
    when (room) {
        Room.Cove -> drawWaves(seconds)
        Room.TacoTruck -> drawStringBulbs(seconds)
        else -> drawNovemberPlaceMotion(room, seconds)
    }
}
