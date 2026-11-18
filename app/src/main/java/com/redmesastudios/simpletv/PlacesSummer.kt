package com.redmesastudios.simpletv

import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.sin

// More places (see Places.kt), for late spring and early summer: a church with stained-glass
// windows (Pentecost), a donut shop (Donut Day), a park with a gazebo at golden hour
// (Juneteenth), a backyard patio (Father's Day) and a lake at dusk (Fourth of July). Drawn in
// design px (1920x1080).

// ---- Church with stained glass (Pentecost) ----

private val NaveWall = Color(0xFFF1EBE2)
private val NaveStone = Color(0xFFE2D9CC)
private val Lead = Color(0xFF6E6258)
private val Panes = listOf(Color(0xFFE57373), Color(0xFF7FB7D9), Color(0xFFF2C14E), Color(0xFF9BCB8C), Color(0xFFC99BD9))
private val Pew = Color(0xFFA9845E)
private val PewDark = Color(0xFF8C6A4F)

private fun DrawScope.drawStainedGlass() {
    drawRect(NaveWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    drawRect(NaveStone, Offset(0f, 600f), Size(SceneWidth, CounterY - 600f))
    // Tall arched windows along the wall, each a grid of colored panes.
    listOf(300f, 620f, 1240f, 1560f).forEachIndexed { w, x ->
        val top = 620f
        val width = 90f
        drawRoundRect(Lead, Offset(x - 6f, top - 6f), Size(width + 12f, CounterY - top - 14f), CornerRadius(51f, 51f))
        var row = 0
        var y = top
        while (y < CounterY - 30f) {
            var col = 0
            var px = x
            while (px < x + width - 1f) {
                drawRect(Panes[(row + col * 2 + w) % Panes.size], Offset(px + 2f, y + 2f), Size(width / 3 - 4f, 26f), alpha = 0.8f)
                px += width / 3
                col++
            }
            y += 30f
            row++
        }
        drawArc(Panes[w % Panes.size], 180f, 180f, useCenter = true, topLeft = Offset(x, top - 2f), size = Size(width, 90f), alpha = 0.85f)
    }
    // Rows of pews in front.
    drawRect(Color(0xFFD9CFC1), Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    var py = CounterY + 10f
    while (py < SceneHeight) { drawRect(Pew, Offset(0f, py), Size(SceneWidth, 14f)); drawRect(PewDark, Offset(0f, py + 14f), Size(SceneWidth, 6f)); py += 54f }
    // Stone columns at the edges.
    listOf(80f, 1840f).forEach { cx ->
        drawRect(NaveStone, Offset(cx - 40f, 540f), Size(80f, SceneHeight - 540f))
        drawRect(Color(0xFFD2C8BA), Offset(cx - 48f, 540f), Size(96f, 20f))
        drawRect(Color(0xFFEDE6DB), Offset(cx - 20f, 560f), Size(10f, SceneHeight - 560f))
    }
}

// ---- Donut shop (Donut Day) ----

private val ShopWall = Color(0xFFFBEFF1)
private val TileMint = Color(0xFFD7EEE6)
private val TilePink = Color(0xFFF7DCE3)
private val Neon = Color(0xFFE8517A)
private val CaseGlass = Color(0xFFE6F2F4)
private val CaseFrame = Color(0xFFF2B8C6)

private val NeonText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xE8, 0x51, 0x7A)
    textSize = 40f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
    letterSpacing = 0.12f
}

private fun DrawScope.drawDonutShop() {
    drawRect(ShopWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    // A checkerboard of mint and pink tiles behind the band.
    var y = 640f
    var r = 0
    while (y < CounterY) {
        var x = 0f
        var c = r % 2
        while (x < SceneWidth) { drawRect(if (c % 2 == 0) TileMint else TilePink, Offset(x, y), Size(38f, 38f)); x += 38f; c++ }
        y += 38f
        r++
    }
    drawRoundRect(Neon, Offset(1500f, 656f), Size(260f, 64f), CornerRadius(32f), style = Stroke(4f))
    drawIntoCanvas { it.nativeCanvas.drawText("DONUTS", 1630f, 703f, NeonText) }
    // A glass case along the bottom.
    drawRect(CaseFrame, Offset(0f, CounterY + 12f), Size(SceneWidth, SceneHeight - CounterY))
    drawRect(CaseGlass, Offset(0f, CounterY + 22f), Size(SceneWidth, SceneHeight - CounterY - 22f), alpha = 0.8f)
    drawRect(Color(0xFFFBF6EC), Offset(0f, CounterY - 2f), Size(SceneWidth, 16f))
}

// ---- Park with a gazebo at golden hour (Juneteenth) ----

private val GoldenMid = Color(0xFFF8E6C8)
private val GoldenLow = Color(0xFFF4D6A6)
private val ParkFar = Color(0xFFC9D9A6)
private val ParkNear = Color(0xFFB6CE90)
private val Gazebo = Color(0xFFFBF6EC)
private val GazeboRoof = Color(0xFFC0503A)

private fun DrawScope.drawSummerPark() {
    drawSky(GoldenMid, GoldenLow)
    drawRollingHills(ParkFar, ParkNear)
    // A white gazebo on the far rise, in the middle.
    val gx = 960f
    val floor = 810f
    drawRect(Gazebo, Offset(gx - 110f, floor), Size(220f, 12f))
    listOf(-100f, -50f, 0f, 50f, 100f).forEach { dx -> drawRect(Gazebo, Offset(gx + dx - 4f, floor - 90f), Size(8f, 90f)) }
    drawPath(Path().apply { moveTo(gx - 130f, floor - 86f); lineTo(gx, floor - 150f); lineTo(gx + 130f, floor - 86f); close() }, GazeboRoof)
    drawCircle(Gazebo, 8f, Offset(gx, floor - 154f))
    drawRect(Gazebo, Offset(gx - 110f, floor - 40f), Size(220f, 5f))
    drawEdgeTree(110f, Color(0xFF9C7A55), listOf(Color(0xFF9CC98A), Color(0xFFB1D69C), Color(0xFF86B977)))
    drawEdgeTree(1810f, Color(0xFF9C7A55), listOf(Color(0xFFB1D69C), Color(0xFF9CC98A), Color(0xFF86B977)))
}

// ---- Backyard patio (Father's Day) ----

private val YardSky = Color(0xFFE3EEF2)
private val YardSkyLow = Color(0xFFD7E7EE)
private val Fence = Color(0xFFC9A57E)
private val FenceDark = Color(0xFFB08A62)
private val YardLawn = Color(0xFFB9D39A)
private val Patio = Color(0xFFDCCFBE)
private val PatioLine = Color(0xFFCBBDA9)

private fun DrawScope.drawBackyard() {
    drawSky(YardSky, YardSkyLow)
    // A tall board fence, then lawn and a stone patio.
    var x = 0f
    while (x < SceneWidth) {
        drawRect(if ((x / 40f).toInt() % 2 == 0) Fence else FenceDark, Offset(x, 700f), Size(40f, CounterY - 700f))
        drawPath(Path().apply { moveTo(x, 700f); lineTo(x + 20f, 686f); lineTo(x + 40f, 700f); close() }, Fence)
        x += 40f
    }
    drawRect(FenceDark, Offset(0f, 730f), Size(SceneWidth, 8f))
    drawRect(FenceDark, Offset(0f, 830f), Size(SceneWidth, 8f))
    drawRect(YardLawn, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    drawRect(Patio, Offset(300f, CounterY), Size(1320f, SceneHeight - CounterY))
    var px = 300f
    while (px < 1620f) { drawLine(PatioLine, Offset(px, CounterY), Offset(px, SceneHeight), strokeWidth = 2f); px += 110f }
    drawEdgeTree(110f, Color(0xFF9C7A55), listOf(Color(0xFF9CC98A), Color(0xFFB1D69C), Color(0xFF86B977)))
    drawEdgeTree(1810f, Color(0xFF9C7A55), listOf(Color(0xFFB1D69C), Color(0xFF9CC98A), Color(0xFF86B977)))
}

// ---- Lake at dusk (Fourth of July) ----

private val SummerDuskMid = Color(0xFFE4DEEC)
private val SummerDuskLow = Color(0xFFBCC0DA)
private val FarShore = Color(0xFF9CA6BE)
private val Lake = Color(0xFFA9B6D2)
private val NearShore = Color(0xFFB4C29C)

private fun DrawScope.drawLakeDusk() {
    drawSky(SummerDuskMid, SummerDuskLow)
    // A far shore of low hills and pines, the lake, and the near grassy bank.
    drawPath(Path().apply { moveTo(0f, 820f); cubicTo(400f, 790f, 900f, 806f, 1300f, 796f); cubicTo(1600f, 790f, 1800f, 800f, SceneWidth, 812f); lineTo(SceneWidth, 836f); lineTo(0f, 836f); close() }, FarShore)
    drawRect(Lake, Offset(0f, 836f), Size(SceneWidth, 44f))
    drawRect(NearShore, Offset(0f, CounterY + 8f), Size(SceneWidth, SceneHeight - CounterY))
    drawEdgeTree(110f, Color(0xFF7A6A5C), listOf(Color(0xFF8FA584), Color(0xFF9EB492), Color(0xFF7E9474)))
    drawEdgeTree(1810f, Color(0xFF7A6A5C), listOf(Color(0xFF9EB492), Color(0xFF8FA584), Color(0xFF7E9474)))
}

// Fireworks glinting on the water as they burst.
private fun DrawScope.drawLakeGlints(seconds: Float) {
    repeat(6) { i ->
        val x = 300f + i * 260f
        val glow = (sin(seconds * 2.3f + i * 1.7f) * 0.5f + 0.5f)
        drawLine(Color(0xFFFBF6EC), Offset(x - 20f * glow, 846f + (i % 3) * 10f), Offset(x + 20f * glow, 846f + (i % 3) * 10f), strokeWidth = 3f, alpha = 0.6f * glow)
    }
}

// ---- Dispatch ----

fun DrawScope.drawSummerPlace(room: Room) {
    when (room) {
        Room.StainedGlass -> drawStainedGlass()
        Room.DonutShop -> drawDonutShop()
        Room.SummerPark -> drawSummerPark()
        Room.Backyard -> drawBackyard()
        Room.LakeDusk -> drawLakeDusk()
        else -> drawLateSummerPlace(room)
    }
}

fun DrawScope.drawSummerPlaceMotion(room: Room, seconds: Float) {
    when (room) {
        Room.LakeDusk -> drawLakeGlints(seconds)
        else -> drawLateSummerPlaceMotion(room, seconds)
    }
}
