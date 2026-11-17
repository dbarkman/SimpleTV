package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin

// More places (see Places.kt), for February through April: a rose garden (Valentine's), a
// stadium (game day), a capitol lawn (Presidents' Day), a churchyard at dusk (Ash Wednesday)
// and a circus big top (April Fools). Drawn in design px (1920x1080).

// ---- Rose garden at sunset (Valentine's) ----

private val RoseMid = Color(0xFFF7E3E0)
private val RoseLow = Color(0xFFF1CDCB)
private val Lawn = Color(0xFFD4DDB8)
private val LawnDark = Color(0xFFC3CFA4)
private val Picket = Color(0xFFFBF8F2)
private val PicketShade = Color(0xFFE2DCD2)
private val Hedge = Color(0xFF8FAE7E)
private val HedgeDark = Color(0xFF7C9C6C)
private val RoseColors = listOf(Color(0xFFD9534F), Color(0xFFF4A6B8), Color(0xFFC2477A), Color(0xFFE8796B))

private fun DrawScope.drawRoseGarden() {
    drawSky(RoseMid, RoseLow)
    // A white picket fence along the band.
    drawRect(PicketShade, Offset(0f, 806f), Size(SceneWidth, 8f))
    drawRect(PicketShade, Offset(0f, 840f), Size(SceneWidth, 8f))
    var x = 8f
    while (x < SceneWidth) {
        drawPath(Path().apply { moveTo(x, CounterY); lineTo(x, 786f); lineTo(x + 9f, 774f); lineTo(x + 18f, 786f); lineTo(x + 18f, CounterY); close() }, Picket)
        x += 34f
    }
    drawRect(Lawn, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    var sx = 0f
    while (sx < SceneWidth) { drawLine(LawnDark, Offset(sx, CounterY), Offset(sx + 30f, SceneHeight), strokeWidth = 18f, alpha = 0.35f); sx += 120f }
    drawRoseBush(110f)
    drawRoseBush(1810f)
}

// A round rose bush at an edge, where the trees stand outdoors, dotted with blooms.
private fun DrawScope.drawRoseBush(x: Float) {
    listOf(Offset(-60f, 780f) to 80f, Offset(40f, 760f) to 92f, Offset(-10f, 690f) to 84f, Offset(70f, 840f) to 70f, Offset(-80f, 860f) to 64f).forEachIndexed { i, (d, r) ->
        drawCircle(if (i % 2 == 0) Hedge else HedgeDark, r, Offset(x + d.x, d.y))
    }
    listOf(-90f to 740f, -40f to 650f, 30f to 700f, 90f to 760f, -20f to 790f, 60f to 830f, -100f to 840f, 20f to 610f, 110f to 690f).forEachIndexed { i, (dx, y) ->
        val c = Offset(x + dx, y)
        val color = RoseColors[i % RoseColors.size]
        drawCircle(color, 14f, c)
        drawCircle(Color.Black, 6f, Offset(c.x + 2f, c.y + 1f), alpha = 0.12f)
        drawArc(Color.White, 200f, 120f, useCenter = false, topLeft = Offset(c.x - 9f, c.y - 9f), size = Size(18f, 18f), style = Stroke(2f), alpha = 0.35f)
    }
}

// ---- Stadium (game day) ----

private val StadiumMid = Color(0xFFE5EAF0)
private val StadiumLow = Color(0xFFD5DDE6)
private val Stands = Color(0xFFCDD3DC)
private val StandsTier = Color(0xFFBEC6D0)
private val FanColors = listOf(Color(0xFFC9483F), Color(0xFF3A5A92), Color(0xFFF2C14E), Color(0xFFFBF6EC), Color(0xFF5E8C61), Color(0xFFE8793A))
private val Field = Color(0xFF8DBA74)
private val FieldStripe = Color(0xFF82B069)
private val YardLine = Color(0xFFF7F7F2)
private val Tower = Color(0xFF8C96A3)
private val Lamp = Color(0xFFFFF4D6)

private const val StandsTop = 650f
private const val StandsBottom = 846f

private fun DrawScope.drawStadium() {
    drawSky(StadiumMid, StadiumLow)
    // Tiered stands full of fans, each a dot of team color.
    drawRect(Stands, Offset(0f, StandsTop), Size(SceneWidth, StandsBottom - StandsTop))
    var y = StandsTop + 6f
    var row = 0
    while (y < StandsBottom - 10f) {
        drawRect(StandsTier, Offset(0f, y + 22f), Size(SceneWidth, 4f))
        var x = 6f + (row % 2) * 9f
        var k = row * 7
        while (x < SceneWidth) {
            drawCircle(FanColors[(k * 5 + row) % FanColors.size], 4.5f, Offset(x, y + 12f), alpha = 0.4f)
            x += 22f
            k++
        }
        y += 28f
        row++
    }
    drawRect(Color(0xFF9AA4B1), Offset(0f, StandsBottom), Size(SceneWidth, CounterY - StandsBottom))
    // The field: mown stripes and yard lines.
    drawRect(Field, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    var fx = 0f
    var stripe = 0
    while (fx < SceneWidth) {
        if (stripe % 2 == 0) drawRect(FieldStripe, Offset(fx, CounterY), Size(120f, SceneHeight - CounterY))
        drawLine(YardLine, Offset(fx, CounterY), Offset(fx - 30f, SceneHeight), strokeWidth = 4f, alpha = 0.8f)
        fx += 120f
        stripe++
    }
    drawLightTower(60f)
    drawLightTower(1860f)
}

private fun DrawScope.drawLightTower(x: Float) {
    drawLine(Tower, Offset(x, CounterY + 20f), Offset(x, 600f), strokeWidth = 12f)
    drawRoundRect(Tower, Offset(x - 56f, 548f), Size(112f, 60f), CornerRadius(6f))
    repeat(3) { r -> repeat(4) { c -> drawCircle(Lamp, 9f, Offset(x - 39f + c * 26f, 562f + r * 18f)) } }
}

// Camera flashes popping in the crowd.
private fun DrawScope.drawCrowdFlashes(seconds: Float) {
    repeat(10) { i ->
        val t = (seconds * 0.8f + i * 0.37f) % 3f
        if (t < 0.18f) {
            val x = 80f + ((i * 389 + (seconds / 3f).toInt() * 211) % 1760)
            val y = StandsTop + 18f + ((i * 53) % 6) * 28f
            drawCircle(Color.White, 10f * (1f - t / 0.18f) + 3f, Offset(x, y), alpha = 0.9f)
        }
    }
}

// ---- Capitol lawn (Presidents' Day) ----

private val WinterMid = Color(0xFFE8EDF2)
private val WinterLow = Color(0xFFDCE4EC)
private val Marble = Color(0xFFF6F3EE)
private val MarbleShade = Color(0xFFE2DDD4)
private val Column = Color(0xFFEDE8E0)
private val WinterLawn = Color(0xFFD5DCC6)
private val BareTree = Color(0xFF9C8A7C)

private fun DrawScope.drawCapitolLawn() {
    drawSky(WinterMid, WinterLow)
    val cx = 960f
    // A wide base with columns, a drum and a dome with its lantern.
    drawRect(MarbleShade, Offset(cx - 330f, 790f), Size(660f, CounterY - 790f))
    drawRect(Marble, Offset(cx - 320f, 798f), Size(640f, CounterY - 798f))
    var colX = cx - 300f
    while (colX <= cx + 300f) { drawRect(Column, Offset(colX - 6f, 804f), Size(12f, CounterY - 812f)); drawRect(MarbleShade, Offset(colX - 6f, 804f), Size(3f, CounterY - 812f)); colX += 40f }
    drawPath(Path().apply { moveTo(cx - 120f, 798f); lineTo(cx, 770f); lineTo(cx + 120f, 798f); close() }, MarbleShade)
    drawRect(Marble, Offset(cx - 140f, 726f), Size(280f, 52f))
    var dc = cx - 126f
    while (dc <= cx + 126f) { drawRect(MarbleShade, Offset(dc - 3f, 732f), Size(6f, 42f)); dc += 28f }
    drawArc(Marble, 180f, 180f, useCenter = true, topLeft = Offset(cx - 130f, 616f), size = Size(260f, 220f))
    drawArc(MarbleShade, 180f, 180f, useCenter = false, topLeft = Offset(cx - 130f, 616f), size = Size(260f, 220f), style = Stroke(3f))
    listOf(-70f, -30f, 30f, 70f).forEach { dx -> drawLine(MarbleShade, Offset(cx + dx * 0.4f, 620f), Offset(cx + dx, 724f), strokeWidth = 2f) }
    drawRect(Marble, Offset(cx - 16f, 594f), Size(32f, 26f))
    drawCircle(MarbleShade, 9f, Offset(cx, 590f))
    // A winter lawn in front, bare trees at the edges.
    drawRect(WinterLawn, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    drawBareTree(110f)
    drawBareTree(1810f)
}

private fun DrawScope.drawBareTree(x: Float) {
    val base = CounterY + 30f
    drawLine(BareTree, Offset(x, base), Offset(x, 700f), strokeWidth = 18f, cap = StrokeCap.Round)
    listOf(-1f, 1f).forEach { side ->
        drawLine(BareTree, Offset(x, 780f), Offset(x + side * 90f, 660f), strokeWidth = 9f, cap = StrokeCap.Round)
        drawLine(BareTree, Offset(x, 730f), Offset(x + side * 60f, 600f), strokeWidth = 7f, cap = StrokeCap.Round)
        drawLine(BareTree, Offset(x + side * 50f, 712f), Offset(x + side * 110f, 690f), strokeWidth = 5f, cap = StrokeCap.Round)
        drawLine(BareTree, Offset(x + side * 30f, 670f), Offset(x + side * 30f, 590f), strokeWidth = 5f, cap = StrokeCap.Round)
    }
    drawLine(BareTree, Offset(x, 700f), Offset(x, 570f), strokeWidth = 7f, cap = StrokeCap.Round)
}

// ---- Churchyard at dusk (Ash Wednesday) ----

private val DuskGrayMid = Color(0xFFE9E3E8)
private val DuskGrayLow = Color(0xFFDAD2DB)
private val Stone = Color(0xFFCFC7C2)
private val StoneDark = Color(0xFFBDB4AE)
private val QuietGround = Color(0xFFD9D6CB)

private fun DrawScope.drawChurchyard() {
    drawSky(DuskGrayMid, DuskGrayLow)
    // A low dry-stone wall behind the chapel.
    drawRect(StoneDark, Offset(0f, 812f), Size(SceneWidth, CounterY - 812f))
    var row = 0
    var y = 814f
    while (y < CounterY) {
        var x = if (row % 2 == 0) 0f else -30f
        while (x < SceneWidth) {
            drawRoundRect(Stone, Offset(x + 3f, y + 2f), Size(56f, 14f), CornerRadius(6f))
            x += 62f
        }
        y += 18f
        row++
    }
    drawRect(Stone, Offset(0f, 804f), Size(SceneWidth, 10f))
    drawRect(QuietGround, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    drawBareTree(110f)
    drawBareTree(1810f)
}

// ---- Circus big top (April Fools) ----

private val TentRed = Color(0xFFEFB3AC)
private val TentCream = Color(0xFFFBF2EA)
private val Valance = Color(0xFFD9675C)
private val Sawdust = Color(0xFFE8D4B0)
private val RingRed = Color(0xFFC9483F)
private val PoleStripe = listOf(Color(0xFFD9675C), Color(0xFFFBF2EA))
private val Flag = listOf(Color(0xFFF2C14E), Color(0xFF7FB7D9), Color(0xFF9BCB8C))

private fun DrawScope.drawBigTop() {
    drawRect(Color(0xFFF4ECE1), Offset.Zero, Size(SceneWidth, SceneHeight))
    // Striped canvas walls along the band, under a scalloped valance.
    var x = 0f
    var k = 0
    while (x < SceneWidth) {
        drawRect(if (k % 2 == 0) TentRed else TentCream, Offset(x, 600f), Size(60f, CounterY - 600f))
        x += 60f
        k++
    }
    var vx = 0f
    var v = 0
    while (vx < SceneWidth) {
        drawRect(Valance, Offset(vx, 592f), Size(60f, 18f))
        drawArc(Valance, 0f, 180f, useCenter = true, topLeft = Offset(vx, 596f), size = Size(60f, 32f))
        vx += 60f
        v++
    }
    // The ring: sawdust inside a red curb.
    drawRect(Sawdust, Offset(0f, CounterY), Size(SceneWidth, SceneHeight - CounterY))
    drawRect(RingRed, Offset(0f, CounterY - 4f), Size(SceneWidth, 16f))
    drawRect(Color(0xFFF2C14E), Offset(0f, CounterY - 4f), Size(SceneWidth, 3f))
    drawTentPole(80f)
    drawTentPole(1840f)
}

private fun DrawScope.drawTentPole(x: Float) {
    val top = 540f
    var y = top
    var k = 0
    while (y < SceneHeight) {
        drawRect(PoleStripe[k % 2], Offset(x - 14f, y), Size(28f, 30f))
        y += 30f
        k++
    }
    drawCircle(Color(0xFFF2C14E), 14f, Offset(x, top))
}

// Pennants fluttering from the tent poles.
private fun DrawScope.drawPoleFlags(seconds: Float) {
    listOf(80f, 1840f).forEachIndexed { i, x ->
        val dir = if (i == 0) 1f else -1f
        repeat(3) { k ->
            val y = 560f + k * 34f
            val flutter = 6f * sin(seconds * 3f + k + i)
            drawPath(Path().apply {
                moveTo(x + dir * 14f, y); lineTo(x + dir * (70f), y + 12f + flutter); lineTo(x + dir * 14f, y + 24f); close()
            }, Flag[k % Flag.size])
        }
    }
}

// ---- Dispatch ----

fun DrawScope.drawWinterPlace(room: Room) {
    when (room) {
        Room.RoseGarden -> drawRoseGarden()
        Room.Stadium -> drawStadium()
        Room.CapitolLawn -> drawCapitolLawn()
        Room.Churchyard -> drawChurchyard()
        Room.BigTop -> drawBigTop()
        else -> {}
    }
}

fun DrawScope.drawWinterPlaceMotion(room: Room, seconds: Float) {
    when (room) {
        Room.Stadium -> drawCrowdFlashes(seconds)
        Room.BigTop -> drawPoleFlags(seconds)
        else -> {}
    }
}
