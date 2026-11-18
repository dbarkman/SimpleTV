package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

// More places (see Places.kt), for late summer: a sky of chat bubbles (Emoji Day), an ice
// cream parlor (Ice Cream Day), a campsite at dusk (S'mores Day), a dog park (Dog Day), a
// building going up (Labor Day) and a grandparents' living room (Grandparents Day). Drawn in
// design px (1920x1080).

// ---- Chat bubbles (Emoji Day) ----

private val ChatMid = Color(0xFFEAE6F4)
private val ChatLow = Color(0xFFDCD6EE)
private val Bubble = Color(0xFFFDFCF8)
private val BubbleBlue = Color(0xFFC9DDF2)
private val Floor = Color(0xFFD2CCE6)

private fun DrawScope.drawChatSky() {
    drawSky(ChatMid, ChatLow)
    // Soft speech bubbles drifting behind the band, some from each side.
    listOf(Triple(220f, 650f, false), Triple(420f, 760f, true), Triple(1360f, 660f, true), Triple(1580f, 770f, false), Triple(780f, 700f, false), Triple(1120f, 760f, true)).forEach { (x, y, mine) ->
        val w = 180f
        val h = 64f
        val color = if (mine) BubbleBlue else Bubble
        drawRoundRect(color, Offset(x - w / 2, y - h / 2), Size(w, h), CornerRadius(30f), alpha = 0.85f)
        val tailX = if (mine) x + w / 2 - 24f else x - w / 2 + 24f
        drawPath(Path().apply { moveTo(tailX - 10f, y + h / 2 - 4f); lineTo(tailX + (if (mine) 16f else -16f), y + h / 2 + 16f); lineTo(tailX + 10f, y + h / 2 - 4f); close() }, color, alpha = 0.85f)
        repeat(3) { k -> drawRoundRect(Color(0xFFB8B2CC), Offset(x - w / 2 + 22f, y - 16f + k * 13f), Size(w * (0.7f - k * 0.15f), 6f), CornerRadius(3f), alpha = 0.5f) }
    }
    drawRect(Floor, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
}

// ---- Ice cream parlor (Ice Cream Day) ----

private val ParlorWall = Color(0xFFF1F8F4)
private val ParlorStripe = Color(0xFFDCEFE6)
private val Awning = listOf(Color(0xFFF4A6B8), Color(0xFFFBF6EC))
private val Freezer = Color(0xFFE6EEF2)
private val FreezerTubs = listOf(Color(0xFFF7C6CF), Color(0xFF8C5A3A), Color(0xFFFBF2EA), Color(0xFFB8E0C8), Color(0xFFF7D27A), Color(0xFFC99BD9))

private fun DrawScope.drawIceCreamShop() {
    drawRect(ParlorWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    var x = 0f
    var k = 0
    while (x < SceneWidth) { if (k % 2 == 0) drawRect(ParlorStripe, Offset(x, 610f), Size(50f, CounterY - 610f)); x += 50f; k++ }
    var ax = 0f
    var a = 0
    while (ax < SceneWidth) {
        drawRect(Awning[a % 2], Offset(ax, 590f), Size(64f, 22f))
        drawArc(Awning[a % 2], 0f, 180f, useCenter = true, topLeft = Offset(ax, 600f), size = Size(64f, 30f))
        ax += 64f
        a++
    }
    // A dipping freezer along the bottom, tubs of flavors showing through the glass.
    drawRect(Freezer, Offset(0f, CounterY + 12f), Size(SceneWidth, SceneHeight - CounterY))
    var tx = 30f
    var t = 0
    while (tx < SceneWidth) { drawRoundRect(FreezerTubs[t % FreezerTubs.size], Offset(tx, CounterY + 30f), Size(90f, 36f), CornerRadius(6f)); tx += 110f; t++ }
    drawRect(Color(0xFFFBF6EC), Offset(0f, CounterY - 2f), Size(SceneWidth, 16f))
}

// ---- Campsite at dusk (S'mores Day) ----

private val CampMid = Color(0xFFEFE2D8)
private val CampLow = Color(0xFFDCC9C4)
private val Ridge = Color(0xFFB7AFB8)
private val Pines = Color(0xFF7E8E78)
private val PinesDark = Color(0xFF6E7E68)
private val Clearing = Color(0xFFC9C2A8)
private val Tent = Color(0xFFE8955A)
private val TentDark = Color(0xFFC67944)

private fun DrawScope.drawCampsite() {
    drawSky(CampMid, CampLow)
    drawPath(Path().apply { moveTo(0f, 780f); lineTo(260f, 700f); lineTo(520f, 770f); lineTo(800f, 690f); lineTo(1100f, 760f); lineTo(1420f, 680f); lineTo(1700f, 750f); lineTo(SceneWidth, 710f); lineTo(SceneWidth, CounterY); lineTo(0f, CounterY); close() }, Ridge)
    var x = 0f
    var k = 0
    while (x < SceneWidth) {
        val h = 70f + (k * 23) % 50
        drawPath(Path().apply { moveTo(x - 24f, CounterY); lineTo(x, CounterY - h); lineTo(x + 24f, CounterY); close() }, if (k % 2 == 0) Pines else PinesDark)
        x += 36f
        k++
    }
    drawRect(Clearing, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    // A tent pitched at the left edge.
    drawPath(Path().apply { moveTo(0f, CounterY + 20f); lineTo(110f, 640f); lineTo(230f, CounterY + 20f); close() }, Tent)
    drawPath(Path().apply { moveTo(110f, 640f); lineTo(80f, CounterY + 20f); lineTo(140f, CounterY + 20f); close() }, TentDark)
    drawPineTree(1810f)
}

private fun DrawScope.drawPineTree(x: Float) {
    drawRect(Color(0xFF7A5A40), Offset(x - 10f, 820f), Size(20f, 110f))
    listOf(560f to 70f, 640f to 100f, 720f to 130f).forEach { (top, half) ->
        drawPath(Path().apply { moveTo(x - half, top + 130f); lineTo(x, top); lineTo(x + half, top + 130f); close() }, PinesDark)
    }
}

// ---- Dog park (Dog Day) ----

private val DogParkMid = Color(0xFFE3EEF2)
private val DogParkLow = Color(0xFFD5E6ED)
private val ParkGrass = Color(0xFFB6D19A)
private val ParkGrassFar = Color(0xFFC6DBAA)
private val ChainLink = Color(0xFFB3BBC3)
private val Hydrant = Color(0xFFD9534F)

private fun DrawScope.drawDogPark() {
    drawSky(DogParkMid, DogParkLow)
    drawRect(ParkGrassFar, Offset(0f, 800f), Size(SceneWidth, CounterY - 800f))
    // A chain-link fence with posts.
    var x = 0f
    while (x < SceneWidth) {
        drawLine(ChainLink, Offset(x, 720f), Offset(x + 24f, CounterY), strokeWidth = 1.5f, alpha = 0.7f)
        drawLine(ChainLink, Offset(x + 24f, 720f), Offset(x, CounterY), strokeWidth = 1.5f, alpha = 0.7f)
        x += 24f
    }
    var px = 0f
    while (px < SceneWidth) { drawRect(Color(0xFF9CA6B0), Offset(px - 4f, 714f), Size(8f, CounterY - 714f)); px += 240f }
    drawRect(Color(0xFF9CA6B0), Offset(0f, 714f), Size(SceneWidth, 6f))
    drawRect(ParkGrass, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    // A fire hydrant by the fence.
    drawRoundRect(Hydrant, Offset(1640f, CounterY - 70f), Size(36f, 70f), CornerRadius(8f))
    drawRoundRect(Hydrant, Offset(1630f, CounterY - 54f), Size(56f, 14f), CornerRadius(6f))
    drawCircle(Hydrant, 18f, Offset(1658f, CounterY - 70f))
    drawEdgeTree(110f, Color(0xFF9C7A55), listOf(Color(0xFF9CC98A), Color(0xFFB1D69C), Color(0xFF86B977)))
    drawEdgeTree(1810f, Color(0xFF9C7A55), listOf(Color(0xFFB1D69C), Color(0xFF9CC98A), Color(0xFF86B977)))
}

// ---- A building going up (Labor Day) ----

private val WorkMid = Color(0xFFF2E8DA)
private val WorkLow = Color(0xFFE8D8C2)
private val Steel = Color(0xFFC0503A)
private val SteelDark = Color(0xFFA03E2C)
private val Crane = Color(0xFFE9C14A)
private val Dirt = Color(0xFFD2BC9C)

private fun DrawScope.drawWorksite() {
    drawSky(WorkMid, WorkLow)
    // A steel frame rising behind the band: columns and beams.
    val left = 560f
    val right = 1360f
    var y = 640f
    while (y < CounterY) { drawRect(Steel, Offset(left, y), Size(right - left, 10f)); y += 70f }
    var x = left
    while (x <= right) { drawRect(SteelDark, Offset(x - 6f, 630f), Size(12f, CounterY - 630f)); x += 100f }
    // A tower crane at the right edge, its jib reaching over.
    drawRect(Crane, Offset(1790f, 560f), Size(30f, CounterY - 560f))
    var cy = 570f
    while (cy < CounterY) { drawLine(Color(0xFFC9A23A), Offset(1790f, cy), Offset(1820f, cy + 30f), strokeWidth = 3f); cy += 30f }
    drawRect(Crane, Offset(1380f, 560f), Size(540f, 18f))
    drawLine(Color(0xFF6E6258), Offset(1500f, 578f), Offset(1500f, 680f), strokeWidth = 2f)
    drawRect(Steel, Offset(1460f, 680f), Size(80f, 10f))
    drawRect(Dirt, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    drawEdgeTree(110f, Color(0xFF9C7A55), listOf(Color(0xFF9CC98A), Color(0xFFB1D69C), Color(0xFF86B977)))
}

// ---- Grandparents' living room (Grandparents Day) ----

private val Wallpaper = Color(0xFFF4EADB)
private val WallpaperDot = Color(0xFFE8D9C2)
private val Wainscot = Color(0xFFD9C3A2)
private val FrameWood = Color(0xFFB98A5E)
private val Photo = listOf(Color(0xFFDCE7D0), Color(0xFFE5EFF2), Color(0xFFF2E0DA))
private val Rug = Color(0xFFC98E7A)
private val Hearth = Color(0xFFC9A57E)

private fun DrawScope.drawParlor() {
    drawRect(Wallpaper, Offset.Zero, Size(SceneWidth, SceneHeight))
    var y = 620f
    var r = 0
    while (y < 780f) { var x = 20f + (r % 2) * 30f; while (x < SceneWidth) { drawCircle(WallpaperDot, 4f, Offset(x, y)); x += 60f }; y += 30f; r++ }
    drawRect(Wainscot, Offset(0f, 790f), Size(SceneWidth, CounterY - 790f))
    drawRect(FrameWood, Offset(0f, 784f), Size(SceneWidth, 8f))
    // Family photos in frames on the wall.
    listOf(Triple(600f, 640f, 0), Triple(760f, 660f, 1), Triple(1220f, 650f, 2), Triple(1380f, 640f, 0)).forEach { (x, top, i) ->
        drawRect(FrameWood, Offset(x - 50f, top), Size(100f, 110f))
        drawRect(Photo[i], Offset(x - 40f, top + 10f), Size(80f, 90f))
        drawCircle(Color(0xFFC9A57E), 14f, Offset(x, top + 44f), alpha = 0.6f)
        drawRect(Color(0xFFC9A57E), Offset(x - 22f, top + 62f), Size(44f, 38f), alpha = 0.6f)
    }
    drawRect(Rug, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    // A fireplace at the left edge with a clock on the mantel.
    drawRect(Hearth, Offset(0f, 640f), Size(220f, CounterY - 640f))
    drawRect(Color(0xFF3A2620), Offset(50f, 740f), Size(120f, CounterY - 740f))
    drawRect(FrameWood, Offset(-10f, 628f), Size(240f, 16f))
    drawRoundRect(FrameWood, Offset(80f, 576f), Size(60f, 52f), CornerRadius(24f, 24f))
    drawCircle(Color(0xFFFBF6EC), 18f, Offset(110f, 602f))
    drawLine(Color(0xFF2A211B), Offset(110f, 602f), Offset(110f, 590f), strokeWidth = 2f)
    drawLine(Color(0xFF2A211B), Offset(110f, 602f), Offset(118f, 604f), strokeWidth = 2f)
    // A tall plant at the right edge.
    drawRect(Color(0xFFD9895E), Offset(1780f, CounterY - 70f), Size(70f, 70f))
    listOf(-40f, -20f, 0f, 20f, 40f).forEach { a -> drawLine(Color(0xFF6E9C6D), Offset(1815f, CounterY - 70f), Offset(1815f + a * 2f, CounterY - 260f + kotlin.math.abs(a)), strokeWidth = 10f, cap = StrokeCap.Round) }
}

// The fire flickering in the grate.
private fun DrawScope.drawParlorFire(seconds: Float) {
    listOf(80f, 110f, 140f).forEachIndexed { i, x ->
        val h = 40f + 12f * sin(seconds * 6f + i * 1.7f)
        drawPath(Path().apply { moveTo(x - 16f, CounterY); quadraticTo(x - 14f, CounterY - h * 0.6f, x + 3f * sin(seconds * 5f + i), CounterY - h); quadraticTo(x + 14f, CounterY - h * 0.6f, x + 16f, CounterY); close() }, Color(0xFFE8793A), alpha = 0.9f)
        drawPath(Path().apply { moveTo(x - 7f, CounterY); quadraticTo(x - 6f, CounterY - h * 0.35f, x, CounterY - h * 0.5f); quadraticTo(x + 6f, CounterY - h * 0.35f, x + 7f, CounterY); close() }, Color(0xFFF7D27A))
    }
}

// ---- Dispatch ----

fun DrawScope.drawLateSummerPlace(room: Room) {
    when (room) {
        Room.ChatSky -> drawChatSky()
        Room.IceCreamShop -> drawIceCreamShop()
        Room.Campsite -> drawCampsite()
        Room.DogPark -> drawDogPark()
        Room.Worksite -> drawWorksite()
        Room.Parlor -> drawParlor()
        else -> {}
    }
}

fun DrawScope.drawLateSummerPlaceMotion(room: Room, seconds: Float) {
    when (room) {
        Room.Parlor -> drawParlorFire(seconds)
        else -> {}
    }
}
