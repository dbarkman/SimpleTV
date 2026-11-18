package com.redmesastudios.simpletv

import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.sin

// More places (see Places.kt), for late October and November: a haunted hill at dusk under the
// harvest moon (Halloween), an evening plaza for the ofrenda (Día de los Muertos), a small-town
// main street (Election Day) and a veterans memorial wall (Veterans Day). Drawn in design px
// (1920x1080).

// ---- Haunted dusk (Halloween) ----

private val SpookyMid = Color(0xFFE9DFE6)
private val SpookyLow = Color(0xFFCFC0D2)
private val SpookyHill = Color(0xFFB9AABD)
private val SpookyGround = Color(0xFFCDBFAE)
private val HouseDark = Color(0xFF6E6476)
private val HouseRoof = Color(0xFF574E5E)
private val LitWindow = Color(0xFFF7D27A)
private val Gnarled = Color(0xFF6E6058)

private fun DrawScope.drawHauntedDusk() {
    drawSky(SpookyMid, SpookyLow)
    drawHarvestMoon(1f)
    drawPath(Path().apply {
        moveTo(0f, 840f); cubicTo(420f, 790f, 820f, 830f, 1200f, 806f); cubicTo(1500f, 790f, 1760f, 812f, SceneWidth, 826f)
        lineTo(SceneWidth, SceneHeight); lineTo(0f, SceneHeight); close()
    }, SpookyHill)
    drawRect(SpookyGround, Offset(0f, CounterY + 6f), Size(SceneWidth, SceneHeight - CounterY))
    // A crooked old house at the right edge with a lit window or two.
    val x = 1790f
    drawPath(Path().apply { moveTo(x - 110f, CounterY + 10f); lineTo(x - 100f, 680f); lineTo(x + 130f, 690f); lineTo(x + 130f, CounterY + 10f); close() }, HouseDark)
    drawPath(Path().apply { moveTo(x - 130f, 690f); lineTo(x - 10f, 580f); lineTo(x + 150f, 696f); close() }, HouseRoof)
    drawRect(HouseDark, Offset(x + 40f, 600f), Size(30f, 60f))
    drawRect(LitWindow, Offset(x - 70f, 720f), Size(36f, 46f), alpha = 0.85f)
    drawRect(Color(0xFF4A4250), Offset(x - 10f, 720f), Size(36f, 46f))
    drawRect(Color(0xFF4A4250), Offset(x - 50f, 790f), Size(46f, 76f))
    // A gnarled bare tree at the left edge.
    val t = 80f
    drawLine(Gnarled, Offset(t, CounterY + 20f), Offset(t + 10f, 700f), strokeWidth = 20f, cap = StrokeCap.Round)
    listOf(Offset(-70f, 640f), Offset(80f, 630f), Offset(-40f, 590f), Offset(50f, 600f)).forEachIndexed { i, tip ->
        drawPath(Path().apply { moveTo(t + 10f, 720f - i * 10f); quadraticTo(t + tip.x * 0.3f, 680f, t + tip.x, tip.y) }, Gnarled,
            style = androidx.compose.ui.graphics.drawscope.Stroke(7f - i, cap = StrokeCap.Round))
    }
}

// The house's lit window flickering, as if someone's moving inside.
private fun DrawScope.drawHauntedWindow(seconds: Float) {
    val flicker = (sin(seconds * 0.9f) + sin(seconds * 2.3f)) * 0.25f + 0.5f
    drawRect(LitWindow, Offset(1780f, 720f), Size(36f, 46f), alpha = flicker.coerceIn(0f, 0.9f))
}

// ---- Evening plaza with the ofrenda's wall (Día de los Muertos) ----

private val EveMid = Color(0xFFF6E2D2)
private val EveLow = Color(0xFFEFC9B4)
private val Adobe = Color(0xFFE9C8A2)
private val ArchShade = Color(0xFFD2A57C)
private val Tiles = Color(0xFFD9A47E)
private val Marigold = Color(0xFFF2A93B)
private val MarigoldDeep = Color(0xFFE8862E)

private fun DrawScope.drawOfrendaPlaza() {
    drawSky(EveMid, EveLow)
    drawRect(Adobe, Offset(0f, 620f), Size(SceneWidth, CounterY - 620f))
    // A tall arched niche in the middle, where the altar stands.
    drawRoundRect(ArchShade, Offset(720f, 650f), Size(480f, CounterY - 650f), CornerRadius(240f, 240f), alpha = 0.5f)
    // Marigold garlands swagged along the wall.
    listOf(0f, 480f, 1440f).forEach { from ->
        repeat(14) { i ->
            val t = i / 13f
            val y = 640f + 40f * 4f * t * (1f - t)
            drawCircle(if (i % 2 == 0) Marigold else MarigoldDeep, 9f, Offset(from + t * 480f, y))
        }
    }
    drawRect(Tiles, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    drawMarigoldPot(100f)
    drawMarigoldPot(1820f)
}

// Tall pots of marigolds at the edges.
private fun DrawScope.drawMarigoldPot(x: Float) {
    drawPath(Path().apply { moveTo(x - 60f, CounterY - 110f); lineTo(x + 60f, CounterY - 110f); lineTo(x + 44f, CounterY + 20f); lineTo(x - 44f, CounterY + 20f); close() }, Color(0xFFC9784E))
    repeat(14) { i -> drawCircle(if (i % 2 == 0) Marigold else MarigoldDeep, 16f, Offset(x - 52f + (i % 5) * 26f, CounterY - 122f - (i / 5) * 24f)) }
}

// ---- Small-town main street (Election Day) ----

private val StreetMid = Color(0xFFEBE6DE)
private val StreetLow = Color(0xFFDDD6CA)
private val Brick = Color(0xFFD7A68C)
private val BrickDark = Color(0xFFC48E74)
private val Storefront = Color(0xFFE6EEF2)
private val AwningColors = listOf(Color(0xFF3A5A92), Color(0xFFC9483F), Color(0xFF5E8C61), Color(0xFFE2A13B))
private val Curb = Color(0xFFD2CCC2)

private val ShopText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xFB, 0xF6, 0xEC)
    textSize = 15f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.15f
}

private fun DrawScope.drawMainStreet() {
    drawSky(StreetMid, StreetLow)
    // A row of two-story brick shops with striped awnings over their windows.
    val names = listOf("BAKERY", "BOOKS", "HARDWARE", "CAFE", "LIBRARY", "BARBER", "FLORIST")
    var x = 0f
    var i = 0
    while (x < SceneWidth) {
        val w = 260f + (i % 3) * 20f
        val top = 640f + (i % 2) * 20f
        drawRect(if (i % 2 == 0) Brick else BrickDark, Offset(x, top), Size(w, CounterY - top))
        drawRect(Color(0xFFB57C62), Offset(x, top), Size(w, 10f))
        drawRect(Storefront, Offset(x + 30f, 770f), Size(w - 60f, CounterY - 770f), alpha = 0.9f)
        val awning = AwningColors[i % AwningColors.size]
        drawRect(awning, Offset(x + 20f, 742f), Size(w - 40f, 30f))
        var sx = x + 20f
        var k = 0
        while (sx < x + w - 20f) { if (k % 2 == 1) drawRect(Color(0xFFFBF6EC), Offset(sx, 742f), Size(20f, 30f), alpha = 0.6f); sx += 20f; k++ }
        drawIntoCanvas { it.nativeCanvas.drawText(names[i % names.size], x + w / 2, top + 36f, ShopText) }
        listOf(x + w * 0.3f, x + w * 0.7f).forEach { wx -> drawRect(Storefront, Offset(wx - 18f, top + 50f), Size(36f, 40f), alpha = 0.8f) }
        x += w
        i++
    }
    drawRect(Curb, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
}

// ---- Veterans memorial (Veterans Day) ----

private val FallMid = Color(0xFFF2E6D6)
private val FallLow = Color(0xFFE8D6C0)
private val Granite = Color(0xFF6E6A70)
private val GraniteLight = Color(0xFF85818A)
private val Plaza = Color(0xFFDDD3C4)
private val FallCrowns = listOf(Color(0xFFE2A85E), Color(0xFFD9775A), Color(0xFFC9A04E))

private fun DrawScope.drawVeteransMemorial() {
    drawSky(FallMid, FallLow)
    drawRect(Color(0xFFD9CDB0), Offset(0f, 800f), Size(SceneWidth, CounterY - 800f))
    // A long polished granite wall, its engraved lines catching the light.
    drawPath(Path().apply { moveTo(240f, CounterY); lineTo(240f, 760f); lineTo(960f, 700f); lineTo(1680f, 760f); lineTo(1680f, CounterY); close() }, Granite)
    var lx = 270f
    while (lx < 1660f) {
        val top = 770f - (60f * (1f - kotlin.math.abs(lx - 960f) / 720f))
        var ly = top
        while (ly < CounterY - 14f) { drawLine(GraniteLight, Offset(lx, ly), Offset(lx + 60f, ly), strokeWidth = 1.5f, alpha = 0.5f); ly += 12f }
        lx += 80f
    }
    drawRect(Plaza, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    drawEdgeTree(110f, Color(0xFF8C6A4F), FallCrowns)
    drawEdgeTree(1810f, Color(0xFF8C6A4F), FallCrowns.reversed())
}

// ---- Dispatch ----

fun DrawScope.drawNovemberPlace(room: Room) {
    when (room) {
        Room.HauntedDusk -> drawHauntedDusk()
        Room.OfrendaPlaza -> drawOfrendaPlaza()
        Room.MainStreet -> drawMainStreet()
        Room.VeteransMemorial -> drawVeteransMemorial()
        else -> {}
    }
}

fun DrawScope.drawNovemberPlaceMotion(room: Room, seconds: Float) {
    when (room) {
        Room.HauntedDusk -> drawHauntedWindow(seconds)
        else -> {}
    }
}
