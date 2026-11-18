package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

// More places (see Places.kt), for spring: a hill with three crosses at dusk (Good Friday), a
// meadow at sunrise (Easter), a green valley with wind turbines (Earth Day), a plaza with
// adobe arches (Cinco de Mayo), a sunroom (Mother's Day) and a national cemetery (Memorial
// Day). Drawn in design px (1920x1080).

internal fun DrawScope.drawRollingHills(far: Color, near: Color) {
    drawPath(Path().apply {
        moveTo(0f, 850f); cubicTo(360f, 790f, 700f, 830f, 980f, 812f); cubicTo(1300f, 792f, 1640f, 800f, SceneWidth, 836f)
        lineTo(SceneWidth, SceneHeight); lineTo(0f, SceneHeight); close()
    }, far)
    drawPath(Path().apply {
        moveTo(0f, 896f); cubicTo(480f, 860f, 900f, 884f, 1260f, 872f); cubicTo(1560f, 862f, 1760f, 870f, SceneWidth, 884f)
        lineTo(SceneWidth, SceneHeight); lineTo(0f, SceneHeight); close()
    }, near)
}

// A round-crowned tree at an edge, where the season's trees stand.
internal fun DrawScope.drawEdgeTree(x: Float, trunk: Color, crowns: List<Color>, blossoms: Color? = null) {
    drawRoundRect(trunk, Offset(x - 8f, 740f), Size(16f, 190f), CornerRadius(6f))
    listOf(Offset(0f, 720f) to 78f, Offset(-48f, 780f) to 56f, Offset(54f, 790f) to 52f).forEachIndexed { i, (d, r) ->
        drawCircle(crowns[i % crowns.size], r, Offset(x + d.x, d.y))
    }
    blossoms?.let { color ->
        listOf(-40f to 700f, 10f to 680f, 46f to 720f, -60f to 770f, 30f to 770f, 70f to 800f, -20f to 740f).forEach { (dx, y) ->
            drawCircle(color, 7f, Offset(x + dx, y))
        }
    }
}

// ---- A hill at dusk with three crosses (Good Friday) ----

private val SolemnMid = Color(0xFFE7E0E4)
private val SolemnLow = Color(0xFFD7CCD4)
private val SolemnFar = Color(0xFFC8BEC2)
private val SolemnNear = Color(0xFFD8D2CC)
private val CrossWood = Color(0xFF8C7A6E)

private fun DrawScope.drawCalvary() {
    drawSky(SolemnMid, SolemnLow)
    // A distant rise in the middle with three crosses on it, the middle one tallest.
    drawPath(Path().apply {
        moveTo(560f, CounterY); cubicTo(780f, 760f, 1140f, 760f, 1360f, CounterY); close()
    }, SolemnFar)
    listOf(Triple(880f, 770f, 0.75f), Triple(960f, 740f, 1f), Triple(1040f, 770f, 0.75f)).forEach { (x, top, k) ->
        drawLine(CrossWood, Offset(x, top + 110f * k), Offset(x, top), strokeWidth = 6f * k)
        drawLine(CrossWood, Offset(x - 24f * k, top + 24f * k), Offset(x + 24f * k, top + 24f * k), strokeWidth = 5f * k)
    }
    drawRollingHills(Color.Transparent, SolemnNear)
    drawEdgeTree(110f, Color(0xFF9C8A7C), listOf(Color(0xFFB7B3A2), Color(0xFFC4C0AE), Color(0xFFABA796)))
    drawEdgeTree(1810f, Color(0xFF9C8A7C), listOf(Color(0xFFC4C0AE), Color(0xFFB7B3A2), Color(0xFFABA796)))
}

// ---- A meadow at sunrise (Easter) ----

private val EasterMid = Color(0xFFF8EAD2)
private val EasterLow = Color(0xFFF6DDB8)
private val MeadowFar = Color(0xFFD3E2B8)
private val MeadowNear = Color(0xFFC3D9A2)
private val Sunrise = Color(0xFFF7D486)
private val Wildflowers = listOf(Color(0xFFF4A6B8), Color(0xFFFBF6EC), Color(0xFFC99BD9), Color(0xFFF2C14E))

private fun DrawScope.drawEasterMeadow() {
    drawSky(EasterMid, EasterLow)
    drawCircle(Sunrise, 200f, Offset(960f, 840f), alpha = 0.25f)
    drawCircle(Sunrise, 96f, Offset(960f, 840f), alpha = 0.85f)
    drawRollingHills(MeadowFar, MeadowNear)
    // Wildflowers dotted over the near hill.
    repeat(70) { i ->
        val x = (i * 97 % 1920).toFloat()
        val y = 900f + (i * 37 % 160)
        drawCircle(Wildflowers[i % Wildflowers.size], 3.5f, Offset(x, y), alpha = 0.8f)
    }
    val blossom = Color(0xFFF7D6DD)
    drawEdgeTree(110f, Color(0xFFA48A72), listOf(Color(0xFFF4C3CF), Color(0xFFF7D6DD), Color(0xFFEBA9BA)))
    drawEdgeTree(1810f, Color(0xFFA48A72), listOf(Color(0xFFF7D6DD), Color(0xFFF4C3CF), Color(0xFFEBA9BA)), blossom)
}

// ---- A green valley with wind turbines (Earth Day) ----

private val ValleyMid = Color(0xFFE3EEF2)
private val ValleyLow = Color(0xFFD2E4EC)
private val ValleyFar = Color(0xFFBFD6A8)
private val ValleyNear = Color(0xFFADCB92)
private val Pine = Color(0xFF7FA270)
private val PineDark = Color(0xFF6E9160)
private val Turbine = Color(0xFFF7F8F8)
private val TurbineShade = Color(0xFFD8DDE0)

val TurbineSpots = listOf(Triple(470f, 700f, 0.8f), Triple(640f, 730f, 0.6f), Triple(1380f, 712f, 0.75f), Triple(1540f, 740f, 0.55f))

private fun DrawScope.drawGreenValley() {
    drawSky(ValleyMid, ValleyLow)
    // A line of pines along the far ridge.
    drawRollingHills(ValleyFar, ValleyNear)
    var x = 0f
    var k = 0
    while (x < SceneWidth) {
        val h = 40f + (k * 13) % 26
        val baseY = 820f + 14f * sin(x / 260f)
        drawPath(Path().apply { moveTo(x - 14f, baseY); lineTo(x, baseY - h); lineTo(x + 14f, baseY); close() }, if (k % 2 == 0) Pine else PineDark)
        x += 22f
        k++
    }
    TurbineSpots.forEach { (tx, top, s) ->
        drawPath(Path().apply { moveTo(tx - 4f * s, CounterY - 30f); lineTo(tx - 2f * s, top); lineTo(tx + 2f * s, top); lineTo(tx + 4f * s, CounterY - 30f); close() }, Turbine)
        drawRect(TurbineShade, Offset(tx - 8f * s, top - 5f * s), Size(16f * s, 9f * s))
    }
    drawEdgeTree(110f, Color(0xFF9C7A55), listOf(Color(0xFF9CC98A), Color(0xFFB1D69C), Color(0xFF86B977)))
    drawEdgeTree(1810f, Color(0xFF9C7A55), listOf(Color(0xFFB1D69C), Color(0xFF9CC98A), Color(0xFF86B977)))
}

// The turbines' blades turning, each at its own speed.
private fun DrawScope.drawTurbineBlades(seconds: Float) {
    TurbineSpots.forEachIndexed { i, (tx, top, s) ->
        val hub = Offset(tx, top)
        val turn = seconds * (40f + i * 9f) + i * 30f
        repeat(3) { b ->
            val a = Math.toRadians((turn + b * 120f).toDouble())
            drawLine(Turbine, hub, Offset(hub.x + 64f * s * cos(a).toFloat(), hub.y + 64f * s * sin(a).toFloat()), strokeWidth = 5f * s, cap = StrokeCap.Round)
        }
        drawCircle(TurbineShade, 5f * s, hub)
    }
}

// ---- A plaza with adobe arches (Cinco de Mayo) ----

private val PlazaMid = Color(0xFFF8E7D2)
private val PlazaLow = Color(0xFFF3D7B5)
private val Adobe = Color(0xFFEBCBA6)
private val AdobeShade = Color(0xFFDDB68C)
private val ArchShadow = Color(0xFFC99A6E)
private val Tile = Color(0xFFD9A47E)
private val TileLine = Color(0xFFC88E66)
private val Saguaro = Color(0xFF7E9A6A)
private val SaguaroDark = Color(0xFF6B8758)

private fun DrawScope.drawPlaza() {
    drawSky(PlazaMid, PlazaLow)
    // An adobe arcade along the band: a wall with round-topped arches.
    drawRect(Adobe, Offset(0f, 640f), Size(SceneWidth, CounterY - 640f))
    drawRect(AdobeShade, Offset(0f, 640f), Size(SceneWidth, 10f))
    var x = 250f
    while (x < 1700f) {
        drawRoundRect(ArchShadow, Offset(x, 700f), Size(120f, CounterY - 700f), CornerRadius(60f, 60f), alpha = 0.55f)
        x += 180f
    }
    // Terracotta tiles underfoot.
    drawRect(Tile, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    var tx = 0f
    while (tx < SceneWidth) { drawLine(TileLine, Offset(tx, CounterY), Offset(tx, SceneHeight), strokeWidth = 2f); tx += 80f }
    var ty = CounterY + 40f
    while (ty < SceneHeight) { drawLine(TileLine, Offset(0f, ty), Offset(SceneWidth, ty), strokeWidth = 2f); ty += 40f }
    drawSaguaro(100f, 1f)
    drawSaguaro(1820f, -1f)
}

private fun DrawScope.drawSaguaro(x: Float, dir: Float) {
    val base = CounterY + 30f
    drawRoundRect(Saguaro, Offset(x - 22f, 580f), Size(44f, base - 580f), CornerRadius(22f))
    // One arm up on each side, at different heights.
    drawRoundRect(Saguaro, Offset(x + dir * 22f - (if (dir > 0) 0f else 50f), 720f), Size(50f, 28f), CornerRadius(14f))
    drawRoundRect(Saguaro, Offset(x + dir * 58f - 14f, 640f), Size(28f, 100f), CornerRadius(14f))
    drawRoundRect(Saguaro, Offset(x - dir * 22f - (if (dir > 0) 40f else 0f), 780f), Size(40f, 24f), CornerRadius(12f))
    drawRoundRect(Saguaro, Offset(x - dir * 50f - 12f, 720f), Size(24f, 76f), CornerRadius(12f))
    repeat(3) { i -> drawLine(SaguaroDark, Offset(x - 10f + i * 10f, 600f), Offset(x - 10f + i * 10f, base - 10f), strokeWidth = 2f, alpha = 0.5f) }
}

// ---- A sunroom (Mother's Day) ----

private val SunroomWall = Color(0xFFF7EEDC)
private val WindowFrame = Color(0xFFFBF8F2)
private val WindowEdge = Color(0xFFE2DACB)
private val GardenSky = Color(0xFFE5EFF2)
private val GardenHedge = Color(0xFFA9C995)
private val GardenFlowers = listOf(Color(0xFFF4A6B8), Color(0xFFF2C14E), Color(0xFFC99BD9), Color(0xFFE8796B))
private val SunroomTable = Color(0xFFE6D3B8)
private val SunroomTableEdge = Color(0xFFD2BC9C)
private val Pot = Color(0xFFD9895E)
private val Fern = Color(0xFF7FA270)

private fun DrawScope.drawSunroom() {
    drawRect(SunroomWall, Offset.Zero, Size(SceneWidth, SceneHeight))
    // Two wide windows onto the garden.
    listOf(240f to 680f, 1000f to 680f).forEach { (left, width) ->
        drawRect(GardenSky, Offset(left, 640f), Size(width, 214f))
        drawRect(GardenHedge, Offset(left, 780f), Size(width, 74f))
        repeat(14) { i -> drawCircle(GardenFlowers[i % GardenFlowers.size], 7f, Offset(left + 24f + i * (width - 48f) / 13f, 786f + (i % 3) * 10f)) }
        drawRect(WindowFrame, Offset(left - 10f, 630f), Size(width + 20f, 10f))
        drawRect(WindowFrame, Offset(left - 10f, 852f), Size(width + 20f, 10f))
        var mx = left
        while (mx <= left + width + 1f) { drawRect(WindowFrame, Offset(mx - 5f, 630f), Size(10f, 232f)); mx += width / 4f }
        drawRect(WindowFrame, Offset(left, 742f), Size(width, 6f))
        drawRect(WindowEdge, Offset(left - 10f, 862f), Size(width + 20f, 4f))
    }
    // A table along the bottom.
    drawRect(SunroomTableEdge, Offset(0f, CounterY + 12f), Size(SceneWidth, SceneHeight - CounterY))
    drawRect(SunroomTable, Offset(0f, CounterY - 2f), Size(SceneWidth, 16f))
    drawPottedFern(100f)
    drawPottedFern(1820f)
}

private fun DrawScope.drawPottedFern(x: Float) {
    drawPath(Path().apply { moveTo(x - 50f, CounterY - 80f); lineTo(x + 50f, CounterY - 80f); lineTo(x + 38f, CounterY); lineTo(x - 38f, CounterY); close() }, Pot)
    listOf(-150f, -125f, -100f, -80f, -60f, -35f).forEach { angle ->
        withTransform({ rotate(angle + 90f, pivot = Offset(x, CounterY - 80f)) }) {
            drawOval(Fern, Offset(x - 12f, CounterY - 80f - 150f), Size(24f, 150f))
        }
        withTransform({ rotate(-(angle + 90f), pivot = Offset(x, CounterY - 80f)) }) {
            drawOval(Fern, Offset(x - 12f, CounterY - 80f - 150f), Size(24f, 150f), alpha = 0.85f)
        }
    }
}

// ---- A national cemetery (Memorial Day) ----

private val MemorialMid = Color(0xFFE9EDF0)
private val MemorialLow = Color(0xFFDCE3E8)
private val CemeteryLawn = Color(0xFFC4D6AE)
private val CemeteryLawnFar = Color(0xFFD0DFBC)
private val Headstone = Color(0xFFFBFAF6)
private val HeadstoneShade = Color(0xFFDCD8CF)
private val Oak = Color(0xFF7E9A6A)

// Where the front row's headstones stand, so the small flags beside them (MemorialDayScene)
// line up; the middle is left for the tall flag.
val FrontHeadstones = listOf(230f, 330f, 430f, 530f, 630f, 730f, 1190f, 1290f, 1390f, 1490f, 1590f, 1690f)

private fun DrawScope.drawCemetery() {
    drawSky(MemorialMid, MemorialLow)
    drawRect(CemeteryLawnFar, Offset(0f, 800f), Size(SceneWidth, CounterY - 800f))
    drawRect(CemeteryLawn, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    // Two rows behind, smaller with distance, then the front row.
    listOf(Triple(806f, 0.55f, 60f), Triple(834f, 0.75f, 80f)).forEach { (baseY, s, step) ->
        var x = 200f + step / 2
        while (x < 1720f) { drawHeadstone(x, baseY, s); x += step }
    }
    FrontHeadstones.forEach { drawHeadstone(it, CounterY, 1f) }
    drawEdgeTree(110f, Color(0xFF8C6A4F), listOf(Oak, Color(0xFF8FAE7E), Color(0xFF6E8A5E)))
    drawEdgeTree(1810f, Color(0xFF8C6A4F), listOf(Color(0xFF8FAE7E), Oak, Color(0xFF6E8A5E)))
}

private fun DrawScope.drawHeadstone(x: Float, baseY: Float, s: Float) {
    val w = 30f * s
    val h = 46f * s
    drawRoundRect(HeadstoneShade, Offset(x - w / 2 + 2f * s, baseY - h + 2f), Size(w, h), CornerRadius(w / 2, w / 2))
    drawRoundRect(Headstone, Offset(x - w / 2, baseY - h), Size(w, h), CornerRadius(w / 2, w / 2))
    drawRect(Headstone, Offset(x - w / 2, baseY - h / 2), Size(w, h / 2))
}

// ---- Dispatch ----

fun DrawScope.drawSpringPlace(room: Room) {
    when (room) {
        Room.Calvary -> drawCalvary()
        Room.EasterMeadow -> drawEasterMeadow()
        Room.GreenValley -> drawGreenValley()
        Room.Plaza -> drawPlaza()
        Room.Sunroom -> drawSunroom()
        Room.Cemetery -> drawCemetery()
        else -> drawSummerPlace(room)
    }
}

fun DrawScope.drawSpringPlaceMotion(room: Room, seconds: Float) {
    when (room) {
        Room.GreenValley -> drawTurbineBlades(seconds)
        else -> drawSummerPlaceMotion(room, seconds)
    }
}
