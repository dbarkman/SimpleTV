package com.redmesastudios.simpletv

import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.sin

// Outdoor places for holidays that suit somewhere other than the season's hills, drawn like
// the rooms (Rooms.kt): the sky stays close to the plain background behind the header and
// apps so their text reads the same, deepening only along the band, and the place's ground
// meets the band's floor at y 868. Drawn in design px (1920x1080).

private val Plain = Color(0xFFF4ECE1)

// A sky from the plain background at the top to `low` at the band's floor.
internal fun DrawScope.drawSky(mid: Color, low: Color) {
    drawRect(
        Brush.verticalGradient(0f to Plain, 0.42f to Plain, 0.68f to mid, 0.84f to low, startY = 0f, endY = SceneHeight),
        Offset.Zero, Size(SceneWidth, SceneHeight),
    )
}

// ---- City at dusk (New Year's) ----

private val DuskMid = Color(0xFFE6DEEC)
private val DuskLow = Color(0xFFD3CBE2)
private val FarCity = Color(0xFFC6C0D9)
private val NearCity = Color(0xFFADA7C6)
private val EdgeCity = Color(0xFF9F99BA)
private val Sidewalk = Color(0xFFD9D3E4)
private val Street = Color(0xFFBEB8D1)
private val Window = Color(0xFFF7E3A6)

// Buildings: left edge, width, top.
private val FarBuildings = listOf(
    180f to 120f to 700f, 300f to 90f to 662f, 390f to 140f to 720f, 530f to 80f to 640f, 610f to 130f to 690f, 740f to 100f to 650f,
    840f to 150f to 708f, 990f to 90f to 630f, 1080f to 140f to 690f, 1220f to 110f to 655f, 1330f to 130f to 712f, 1460f to 90f to 668f,
    1550f to 160f to 700f,
).map { (a, top) -> Triple(a.first, a.second, top) }
private val NearBuildings = listOf(
    200f to 140f to 760f, 340f to 100f to 730f, 440f to 160f to 780f, 600f to 120f to 745f, 1200f to 130f to 750f, 1330f to 150f to 772f,
    1480f to 110f to 738f, 1590f to 130f to 768f,
).map { (a, top) -> Triple(a.first, a.second, top) }

private fun DrawScope.drawCityDusk() {
    drawSky(DuskMid, DuskLow)
    FarBuildings.forEach { (x, w, top) -> drawRect(FarCity, Offset(x, top), Size(w, CounterY - top)) }
    NearBuildings.forEach { (x, w, top) ->
        drawRect(NearCity, Offset(x, top), Size(w, CounterY - top))
        // A grid of windows, about half of them lit.
        var wy = top + 16f
        var row = 0
        while (wy < CounterY - 24f) {
            var wx = x + 12f
            var col = 0
            while (wx < x + w - 18f) {
                if ((row * 7 + col * 3 + x.toInt() / 10) % 5 < 3) drawRect(Window, Offset(wx, wy), Size(10f, 14f), alpha = 0.75f)
                wx += 22f
                col++
            }
            wy += 28f
            row++
        }
    }
    // Tall towers at the edges, where the trees stand outdoors.
    listOf(0f to 200f, 1720f to 200f).forEach { (x, w) ->
        drawRect(EdgeCity, Offset(x, 560f), Size(w, CounterY - 560f))
        drawRect(EdgeCity, Offset(x + w / 2 - 6f, 510f), Size(12f, 50f))
        var wy = 580f
        while (wy < CounterY - 20f) {
            var wx = x + 18f
            while (wx < x + w - 20f) {
                drawRect(Window, Offset(wx, wy), Size(14f, 18f), alpha = 0.6f)
                wx += 34f
            }
            wy += 36f
        }
    }
    drawRect(Sidewalk, Offset(0f, CounterY), Size(SceneWidth, 34f))
    drawRect(Street, Offset(0f, CounterY + 34f), Size(SceneWidth, SceneHeight - CounterY))
}

// A few windows switching on and off across the skyline.
private fun DrawScope.drawCityWindows(seconds: Float) {
    NearBuildings.forEachIndexed { i, (x, w, top) ->
        val on = sin(seconds * 0.7f + i * 2.3f) > 0.3f
        drawRect(Window, Offset(x + w / 2 - 5f, top + 44f), Size(10f, 14f), alpha = if (on) 0.95f else 0.15f)
    }
}

// ---- Desert at dusk (Epiphany) ----

private val DesertMid = Color(0xFFF4E2CC)
private val DesertLow = Color(0xFFEDCFAE)
private val FarDune = Color(0xFFE6C49A)
private val NearDune = Color(0xFFDBB283)
private val Palm = Color(0xFF8C6A4F)
private val Frond = Color(0xFF7E9A6A)

private fun DrawScope.drawDesert() {
    drawSky(DesertMid, DesertLow)
    drawPath(Path().apply {
        moveTo(0f, 860f)
        cubicTo(300f, 790f, 520f, 800f, 760f, 836f)
        cubicTo(1000f, 872f, 1260f, 780f, 1560f, 806f)
        cubicTo(1740f, 822f, 1840f, 840f, SceneWidth, 830f)
        lineTo(SceneWidth, SceneHeight); lineTo(0f, SceneHeight); close()
    }, FarDune)
    drawPath(Path().apply {
        moveTo(0f, 900f)
        cubicTo(420f, 858f, 780f, 880f, 1100f, 872f)
        cubicTo(1400f, 864f, 1700f, 880f, SceneWidth, 892f)
        lineTo(SceneWidth, SceneHeight); lineTo(0f, SceneHeight); close()
    }, NearDune)
    drawPalm(90f, 900f, 1f)
    drawPalm(1830f, 900f, -1f)
}

private fun DrawScope.drawPalm(x: Float, baseY: Float, lean: Float) {
    val top = Offset(x + 40f * lean, baseY - 330f)
    drawPath(Path().apply { moveTo(x, baseY); quadraticTo(x - 10f * lean, baseY - 180f, top.x, top.y) }, Palm, style = Stroke(16f, cap = StrokeCap.Round))
    listOf(-150f, -110f, -70f, -30f, 10f, 200f, 240f).forEachIndexed { i, angle ->
        val a = Math.toRadians(angle.toDouble())
        val tip = Offset(top.x + 110f * kotlin.math.cos(a).toFloat(), top.y + 70f * kotlin.math.sin(a).toFloat() + 30f)
        drawPath(Path().apply {
            moveTo(top.x, top.y)
            quadraticTo((top.x + tip.x) / 2, (top.y + tip.y) / 2 - 30f, tip.x, tip.y)
            quadraticTo((top.x + tip.x) / 2, (top.y + tip.y) / 2 - 8f, top.x, top.y)
            close()
        }, Frond, alpha = if (i % 2 == 0) 1f else 0.85f)
    }
}

// ---- Reflecting pool at dawn (MLK Day) ----

private val DawnMid = Color(0xFFF7E6CD)
private val DawnLow = Color(0xFFF3D6AE)
private val Sun = Color(0xFFF6CF7A)
private val SunGlow = Color(0xFFF8DFA2)
private val Obelisk = Color(0xFFD9D1C6)
private val ObeliskShade = Color(0xFFC6BDB0)
private val TreeRow = Color(0xFFB7C2A4)
private val TreeRowDark = Color(0xFFA6B392)
private val Walk = Color(0xFFE9E1D3)
private val Water = Color(0xFFC6DBE4)
private val WaterDeep = Color(0xFFB4CEDA)

private fun DrawScope.drawReflectingPool() {
    drawSky(DawnMid, DawnLow)
    val sun = Offset(960f, 830f)
    drawCircle(SunGlow, 250f, sun, alpha = 0.18f)
    drawCircle(SunGlow, 170f, sun, alpha = 0.28f)
    drawCircle(Sun, 104f, sun, alpha = 0.9f)
    // Rows of trees along both sides of the far end.
    listOf(0f to 720f, 1200f to 1920f).forEach { (from, to) ->
        var x = from + 30f
        var k = 0
        while (x < to) {
            drawCircle(if (k % 2 == 0) TreeRow else TreeRowDark, 46f + (k % 3) * 8f, Offset(x, CounterY - 30f))
            x += 70f
            k++
        }
    }
    // The obelisk in the distance, in front of the rising sun.
    drawPath(Path().apply {
        moveTo(944f, CounterY); lineTo(950f, 600f); lineTo(960f, 578f); lineTo(970f, 600f); lineTo(976f, CounterY); close()
    }, Obelisk)
    drawPath(Path().apply { moveTo(960f, CounterY); lineTo(960f, 578f); lineTo(970f, 600f); lineTo(976f, CounterY); close() }, ObeliskShade)
    // Walks on both sides, and the long pool narrowing into the distance.
    drawRect(Walk, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    drawPath(Path().apply {
        moveTo(700f, CounterY); lineTo(1220f, CounterY); lineTo(1760f, SceneHeight); lineTo(160f, SceneHeight); close()
    }, Water)
    drawPath(Path().apply {
        moveTo(700f, CounterY); lineTo(1220f, CounterY); lineTo(1760f, SceneHeight); lineTo(160f, SceneHeight); close()
    }, WaterDeep, style = Stroke(4f))
}

// The sun's reflection shimmering down the pool.
private fun DrawScope.drawPoolShimmer(seconds: Float) {
    repeat(9) { i ->
        val y = CounterY + 10f + i * 22f
        val half = 30f + i * 10f
        val drift = 14f * sin(seconds * 1.2f + i * 0.9f)
        drawLine(Sun, Offset(960f - half + drift, y), Offset(960f + half + drift, y), strokeWidth = 4f, cap = StrokeCap.Round, alpha = 0.55f - i * 0.04f)
    }
}

// ---- Movie theater concession stand (Popcorn Day) ----

private val LobbyWall = Color(0xFFF6E9E4)
private val Curtain = Color(0xFFB8434A)
private val CurtainFold = Color(0xFF953038)
private val Tieback = Color(0xFFE9C14A)
private val MarqueeBoard = Color(0xFF3A2E3A)
private val MarqueeBulb = Color(0xFFF7D27A)
private val CounterTop = Color(0xFFFBF6EC)
private val StripeRed = Color(0xFFE6A1A1)
private val StripeCream = Color(0xFFF8EEE8)
private val MachineRed = Color(0xFFC9483F)
private val Glass = Color(0xFFE8F1F4)
private val PoppedCorn = Color(0xFFFFF1CF)

private val MarqueeText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xF7, 0xD2, 0x7A)
    textSize = 26f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.3f
}

private const val MachineX = 1580f
private val MachineGlassTop = CounterY - 170f

private fun DrawScope.drawConcessions() {
    drawRect(LobbyWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    // A lit sign over the stand.
    // A lit sign on the wall at the left, below where a second row of apps reaches.
    drawRoundRect(MarqueeBoard, Offset(250f, 652f), Size(440f, 52f), CornerRadius(10f))
    var bx = 264f
    while (bx < 680f) { drawCircle(MarqueeBulb, 2.5f, Offset(bx, 658f)); drawCircle(MarqueeBulb, 2.5f, Offset(bx, 698f)); bx += 18f }
    drawIntoCanvas { it.nativeCanvas.drawText("CONCESSIONS", 470f, 688f, MarqueeText) }
    // Counter: cream top over a red-and-white striped front.
    var x = 0f
    var k = 0
    while (x < SceneWidth) {
        drawRect(if (k % 2 == 0) StripeRed else StripeCream, Offset(x, CounterY + 12f), Size(40f, SceneHeight - CounterY))
        x += 40f
        k++
    }
    drawRect(CounterTop, Offset(0f, CounterY - 2f), Size(SceneWidth, 16f))
    drawPopcornMachine()
    drawCurtain(0f, 1f)
    drawCurtain(SceneWidth, -1f)
}

private fun DrawScope.drawCurtain(edge: Float, dir: Float) {
    // Hung from a gold rod below the header, so the greeting and clock stay clear.
    val width = 210f
    val top = 540f
    val left = if (dir > 0) edge else edge - width
    drawRect(Curtain, Offset(left, top), Size(width, SceneHeight - top))
    repeat(6) { i -> drawRect(CurtainFold, Offset(left + 14f + i * 34f, top), Size(10f, SceneHeight - top), alpha = 0.55f) }
    drawRoundRect(Tieback, Offset(left - 4f, top - 8f), Size(width + 8f, 12f), CornerRadius(6f))
    // Gathered by a gold tieback, flaring out below it.
    val tieY = 760f
    drawRoundRect(Tieback, Offset(left + (if (dir > 0) 40f else 30f), tieY - 8f), Size(140f, 16f), CornerRadius(8f))
    drawCircle(Tieback, 10f, Offset(if (dir > 0) left + width - 30f else left + 30f, tieY))
}

private fun DrawScope.drawPopcornMachine() {
    val w = 170f
    val left = MachineX - w / 2
    drawRect(MachineRed, Offset(left, CounterY - 30f), Size(w, 30f))
    drawRect(Glass, Offset(left + 8f, MachineGlassTop), Size(w - 16f, 140f), alpha = 0.9f)
    drawRect(MachineRed, Offset(left, MachineGlassTop - 28f), Size(w, 28f))
    drawPath(Path().apply { moveTo(left - 6f, MachineGlassTop - 28f); lineTo(MachineX, MachineGlassTop - 70f); lineTo(left + w + 6f, MachineGlassTop - 28f); close() }, MachineRed)
    // The heap of popped corn at the bottom of the glass.
    clipPath(Path().apply { addRect(androidx.compose.ui.geometry.Rect(left + 8f, MachineGlassTop, left + w - 8f, CounterY - 30f)) }) {
        repeat(18) { i -> drawCircle(PoppedCorn, 12f, Offset(left + 14f + (i % 9) * 18f, CounterY - 40f - (i / 9) * 14f + (i % 3) * 3f)) }
    }
    // The kettle hanging from the top.
    drawRoundRect(Color(0xFF8C9AA6), Offset(MachineX - 26f, MachineGlassTop + 6f), Size(52f, 26f), CornerRadius(6f))
}

// Kernels popping out of the kettle and falling onto the heap, inside the glass.
private fun DrawScope.drawMachineKernels(seconds: Float) {
    val w = 170f
    val left = MachineX - w / 2
    clipPath(Path().apply { addRect(androidx.compose.ui.geometry.Rect(left + 8f, MachineGlassTop, left + w - 8f, CounterY - 30f)) }) {
        repeat(5) { i ->
            poppedKernel(seconds * 1.4f, i + 3)?.let { pose ->
                // Thrown sideways out of the kettle, then dropping faster and faster onto the heap.
                val progress = kotlin.math.abs(pose.spinDegrees) / 400f
                drawCircle(PoppedCorn, 7f, Offset(MachineX + pose.dx * 0.9f, MachineGlassTop + 40f + 100f * progress * progress))
            }
        }
    }
}

// ---- Courtyard with a moon gate (Lunar New Year) ----

private val CourtWall = Color(0xFFF1EBE2)
private val TileCap = Color(0xFF7E8A86)
private val TileCapDark = Color(0xFF66716D)
private val Garden = Color(0xFFDCE7D0)
private val Bamboo = Color(0xFF8FB07A)
private val Paving = Color(0xFFDDD5C9)
private val PavingLine = Color(0xFFCCC3B5)
private val Pillar = Color(0xFFC0453B)
private val PillarGold = Color(0xFFE9C14A)

private fun DrawScope.drawCourtyard() {
    drawRect(Plain, Offset.Zero, Size(SceneWidth, SceneHeight))
    // The garden wall along the band, with a round moon gate looking onto bamboo.
    drawRect(CourtWall, Offset(0f, 610f), Size(SceneWidth, CounterY - 610f))
    val gate = Offset(970f, 762f)
    drawCircle(Garden, 128f, gate)
    clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(gate, 128f)) }) {
        listOf(880f, 920f, 1010f, 1050f, 960f).forEachIndexed { i, x ->
            drawLine(Bamboo, Offset(x, gate.y + 140f), Offset(x + 6f, gate.y - 140f), strokeWidth = 9f)
            repeat(4) { k -> drawLine(Color(0xFF7A9A66), Offset(x - 5f, gate.y - 90f + k * 50f + i * 7f), Offset(x + 11f, gate.y - 90f + k * 50f + i * 7f), strokeWidth = 2f) }
        }
    }
    drawCircle(TileCapDark, 128f, gate, style = Stroke(6f))
    // A tiled coping along the wall's top, its ends curling up.
    drawRect(TileCap, Offset(0f, 596f), Size(SceneWidth, 22f))
    var x = 0f
    while (x < SceneWidth) { drawLine(TileCapDark, Offset(x, 596f), Offset(x, 618f), strokeWidth = 2f); x += 24f }
    drawRect(TileCapDark, Offset(0f, 616f), Size(SceneWidth, 5f))
    // Paving in front.
    drawRect(Paving, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    var px = 0f
    while (px < SceneWidth) { drawLine(PavingLine, Offset(px, CounterY), Offset(px, SceneHeight), strokeWidth = 2f); px += 96f }
    var py = CounterY + 48f
    while (py < SceneHeight) { drawLine(PavingLine, Offset(0f, py), Offset(SceneWidth, py), strokeWidth = 2f); py += 48f }
    // Red lacquered pillars at the edges.
    listOf(80f, 1840f).forEach { cx ->
        drawRect(Pillar, Offset(cx - 34f, 540f), Size(68f, SceneHeight - 540f))
        drawRect(PillarGold, Offset(cx - 38f, 540f), Size(76f, 16f))
        drawRect(PillarGold, Offset(cx - 38f, CounterY - 16f), Size(76f, 12f))
        drawRect(Color.White, Offset(cx - 22f, 560f), Size(8f, CounterY - 580f), alpha = 0.12f)
    }
}

// ---- Dispatch ----

fun DrawScope.drawPlace(room: Room) {
    when (room) {
        Room.CityDusk -> drawCityDusk()
        Room.Desert -> drawDesert()
        Room.ReflectingPool -> drawReflectingPool()
        Room.Concessions -> drawConcessions()
        Room.Courtyard -> drawCourtyard()
        else -> drawWinterPlace(room)
    }
}

fun DrawScope.drawPlaceMotion(room: Room, seconds: Float) {
    when (room) {
        Room.CityDusk -> drawCityWindows(seconds)
        Room.ReflectingPool -> drawPoolShimmer(seconds)
        Room.Concessions -> drawMachineKernels(seconds)
        else -> drawWinterPlaceMotion(room, seconds)
    }
}
