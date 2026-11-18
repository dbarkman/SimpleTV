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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// The late-spring and early-summer scenes, each on its own backdrop (SceneChoice.kt):
// Bitcoin Pizza Day (May 22, in the pizzeria): two pizzas in open boxes and a gold coin
//   turning, with the famous price on a receipt.
// Pentecost (in the stained-glass church): a dove hovering in a glow, a row of small flames
//   and red banners.
// Donut Day (first Friday of June, in the donut shop): a tower of donuts, plates of
//   sprinkled ones, a box and a cup of coffee.
// Juneteenth (June 19, in the park): the Juneteenth flag waving, red, black and green
//   bunting and balloons.
// Father's Day weekend (on the patio): a kettle grill with burgers and smoke, a lawn chair, a
//   "#1 DAD" mug and a cooler.
// First day of summer (the summer backdrop): sunflowers and a beach ball bouncing.
// Fourth of July (June 28 - July 5, at the lake): red, white and blue fireworks, bunting
//   and small flags along the bank.
// Drawn in design px (1920x1080).

private fun paint(color: Int, size: Float, bold: Boolean = true, face: Typeface = Typeface.DEFAULT) =
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = Typeface.create(face, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

private val Gold = Color(0xFFE9B949)
private val GoldDark = Color(0xFFC9963A)
private val BoxCard = Color(0xFFD9B98A)
private val BoxInside = Color(0xFFEBD6B4)
private val Crust = Color(0xFFE2A85E)
private val Cheese = Color(0xFFF7D27A)
private val Pepperoni = Color(0xFFB8432E)
private val Paper = Color(0xFFFBF8F2)

private val CoinMark = paint(android.graphics.Color.rgb(0xA8, 0x7A, 0x22), 64f)
private val ReceiptText = paint(android.graphics.Color.rgb(0x4A, 0x3F, 0x38), 17f, bold = false, face = Typeface.MONOSPACE)

// ---- Bitcoin Pizza Day ----

fun DrawScope.drawBitcoinPizzaFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawOpenPizzaBox(760f, 866f)
        drawOpenPizzaBox(1000f, 866f)
        drawReceipt(1250f, 866f)
        // The coin turns slowly, edge-on and back.
        val turn = cos(seconds * 1.4f)
        val center = Offset(560f, 760f + 6f * sin(seconds * 1.1f))
        withTransform({ scale(abs(turn).coerceAtLeast(0.08f), 1f, pivot = center) }) {
            drawCircle(GoldDark, 62f, Offset(center.x + 3f, center.y + 3f))
            drawCircle(Gold, 62f, center)
            drawCircle(GoldDark, 50f, center, style = Stroke(4f))
            if (turn > 0f) drawIntoCanvas { it.nativeCanvas.drawText("₿", center.x, center.y + 22f, CoinMark) }
        }
    }
}

private fun DrawScope.drawOpenPizzaBox(x: Float, baseY: Float) {
    drawPath(Path().apply { moveTo(x - 100f, baseY - 30f); lineTo(x + 100f, baseY - 30f); lineTo(x + 80f, baseY - 130f); lineTo(x - 80f, baseY - 130f); close() }, BoxCard)
    drawRect(BoxCard, Offset(x - 100f, baseY - 30f), Size(200f, 30f))
    drawOval(BoxInside, Offset(x - 96f, baseY - 52f), Size(192f, 40f))
    drawOval(Crust, Offset(x - 86f, baseY - 50f), Size(172f, 34f))
    drawOval(Cheese, Offset(x - 76f, baseY - 47f), Size(152f, 28f))
    listOf(-50f to -36f, -10f to -42f, 30f to -38f, 54f to -30f, -30f to -26f, 10f to -26f).forEach { (dx, dy) -> drawOval(Pepperoni, Offset(x + dx - 9f, baseY + dy - 4f), Size(18f, 8f)) }
}

private fun DrawScope.drawReceipt(x: Float, baseY: Float) {
    withTransform({ rotate(6f, pivot = Offset(x, baseY)) }) {
        drawRect(Paper, Offset(x - 70f, baseY - 140f), Size(140f, 140f))
        var zx = x - 70f
        while (zx < x + 70f) { drawPath(Path().apply { moveTo(zx, baseY - 140f); lineTo(zx + 7f, baseY - 148f); lineTo(zx + 14f, baseY - 140f); close() }, Paper); zx += 14f }
        drawIntoCanvas {
            it.nativeCanvas.drawText("2 PIZZAS", x, baseY - 106f, ReceiptText)
            it.nativeCanvas.drawText("--------", x, baseY - 84f, ReceiptText)
            it.nativeCanvas.drawText("10,000 BTC", x, baseY - 60f, ReceiptText)
            it.nativeCanvas.drawText("MAY 22 2010", x, baseY - 26f, ReceiptText)
        }
    }
}

// ---- Pentecost ----

private val Flame = Color(0xFFE8793A)
private val FlameCore = Color(0xFFF7D27A)
private val Banner = Color(0xFFC0453B)
private val Glow = Color(0xFFF7E3A6)

fun DrawScope.drawPentecostFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        listOf(150f, 1770f).forEach { x ->
            drawRect(Banner, Offset(x - 30f, 590f), Size(60f, 200f))
            drawPath(Path().apply { moveTo(x - 30f, 790f); lineTo(x, 820f); lineTo(x + 30f, 790f); close() }, Banner)
            drawRect(Gold, Offset(x - 36f, 584f), Size(72f, 8f))
        }
        val center = Offset(960f, 700f + 6f * sin(seconds * 1.2f))
        drawCircle(Glow, 90f, center, alpha = 0.35f)
        repeat(12) { k ->
            val a = Math.toRadians(k * 30.0)
            drawLine(Glow, Offset(center.x + 60f * cos(a).toFloat(), center.y + 60f * sin(a).toFloat()), Offset(center.x + 110f * cos(a).toFloat(), center.y + 110f * sin(a).toFloat()), strokeWidth = 4f, cap = StrokeCap.Round, alpha = 0.5f)
        }
        drawDove(BatPose(center.x - 10f, center.y, 60f, sin(seconds * 3f), true))
        repeat(7) { i ->
            val x = 720f + i * 80f
            val h = 34f + 8f * sin(seconds * 6f + i * 1.3f)
            drawCircle(Flame, 22f, Offset(x, 852f), alpha = 0.18f)
            drawPath(Path().apply { moveTo(x - 10f, 860f); quadraticTo(x - 9f, 860f - h * 0.6f, x + 2f * sin(seconds * 5f + i), 860f - h); quadraticTo(x + 9f, 860f - h * 0.6f, x + 10f, 860f); close() }, Flame)
            drawPath(Path().apply { moveTo(x - 5f, 860f); quadraticTo(x - 4f, 860f - h * 0.35f, x, 860f - h * 0.55f); quadraticTo(x + 4f, 860f - h * 0.35f, x + 5f, 860f); close() }, FlameCore)
        }
    }
}

// ---- Donut Day ----

private val Dough = Color(0xFFDDA368)
private val Glazes = listOf(Color(0xFFF4A6B8), Color(0xFF8C5A3A), Color(0xFFFBF2EA), Color(0xFFC99BD9), Color(0xFFF7D27A))
private val Sprinkles = listOf(Color(0xFFE8517A), Color(0xFF5FA8D3), Color(0xFFF2D24E), Color(0xFF6DBE7B), Color(0xFFFBF6EC))
private val Plate = Color(0xFFFDFCF8)
private val Mug = Color(0xFF7FB7D9)

fun DrawScope.drawDonutDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        // A tower of donuts, one on another, the top one wobbling.
        repeat(5) { i ->
            val wobble = if (i == 4) 3f * sin(seconds * 2f) else 0f
            drawDonut(Offset(960f + wobble, 846f - i * 30f), 64f, i)
        }
        drawPlateOfDonuts(700f, 866f, 0)
        drawPlateOfDonuts(1220f, 866f, 2)
        drawDonutBox(480f, 866f)
        drawCoffeeMug(1400f, 866f, seconds)
    }
}

// A donut seen from a little above: dough ring, glaze on top, a hole, sprinkles.
private fun DrawScope.drawDonut(center: Offset, width: Float, index: Int) {
    val h = width * 0.42f
    drawOval(Dough, Offset(center.x - width / 2, center.y - h / 2 + 6f), Size(width, h))
    drawOval(Glazes[index % Glazes.size], Offset(center.x - width / 2 + 3f, center.y - h / 2), Size(width - 6f, h - 4f))
    drawOval(Dough, Offset(center.x - width * 0.14f, center.y - h * 0.16f), Size(width * 0.28f, h * 0.3f))
    repeat(10) { k ->
        val a = k * 0.63f + index
        val r = width * 0.32f
        val at = Offset(center.x + r * cos(a), center.y + r * 0.4f * sin(a))
        drawLine(Sprinkles[(k + index) % Sprinkles.size], at, Offset(at.x + 4f * cos(a * 3), at.y + 2f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawPlateOfDonuts(x: Float, baseY: Float, start: Int) {
    drawOval(Color(0xFFDCD6CC), Offset(x - 84f, baseY - 14f), Size(168f, 22f))
    drawOval(Plate, Offset(x - 80f, baseY - 18f), Size(160f, 20f))
    drawDonut(Offset(x - 34f, baseY - 26f), 60f, start)
    drawDonut(Offset(x + 34f, baseY - 26f), 60f, start + 1)
    drawDonut(Offset(x, baseY - 50f), 60f, start + 2)
}

private fun DrawScope.drawDonutBox(x: Float, baseY: Float) {
    drawRect(Color(0xFFF7DCE3), Offset(x - 80f, baseY - 40f), Size(160f, 40f))
    drawPath(Path().apply { moveTo(x - 80f, baseY - 40f); lineTo(x + 80f, baseY - 40f); lineTo(x + 70f, baseY - 120f); lineTo(x - 70f, baseY - 120f); close() }, Color(0xFFF2B8C6))
    drawDonut(Offset(x - 36f, baseY - 52f), 54f, 3)
    drawDonut(Offset(x + 30f, baseY - 52f), 54f, 1)
}

private fun DrawScope.drawCoffeeMug(x: Float, baseY: Float, seconds: Float) {
    drawRoundRect(Mug, Offset(x - 32f, baseY - 72f), Size(64f, 72f), CornerRadius(10f))
    drawArc(Mug, -80f, 160f, useCenter = false, topLeft = Offset(x + 20f, baseY - 58f), size = Size(30f, 32f), style = Stroke(7f))
    drawOval(Color(0xFF8C5A3A), Offset(x - 28f, baseY - 78f), Size(56f, 12f))
    repeat(2) { i ->
        val p = (seconds * 0.35f + i / 2f) % 1f
        val sx = x - 8f + i * 16f + 5f * sin(seconds * 1.5f + i)
        val sy = baseY - 90f - 60f * p
        drawPath(Path().apply { moveTo(sx, sy + 14f); quadraticTo(sx + 7f, sy + 7f, sx, sy); quadraticTo(sx - 7f, sy - 7f, sx, sy - 14f) },
            Color(0xFFD6DCE2), style = Stroke(3.5f, cap = StrokeCap.Round), alpha = 0.7f * sin(Math.PI.toFloat() * p))
    }
}

// ---- Juneteenth ----

private val JRed = Color(0xFFC9483F)
private val JBlue = Color(0xFF3A5A92)
private val JWhite = Color(0xFFFBF6EC)
private val PanRed = Color(0xFFC9483F)
private val PanBlack = Color(0xFF2E2A28)
private val PanGreen = Color(0xFF4E8A5B)
private val Pole = Color(0xFFB3A99C)

fun DrawScope.drawJuneteenthFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawPennantBunting(seconds, listOf(PanRed, PanBlack, PanGreen))
        drawJuneteenthFlag(960f, 866f, 660f, seconds)
        drawBalloonCluster(620f, 866f, listOf(PanRed, PanBlack, PanGreen), seconds, 0f)
        drawBalloonCluster(1300f, 866f, listOf(PanGreen, PanRed, PanBlack), seconds, 2f)
    }
}

// The Juneteenth flag: blue over red with a horizon arc, a white star in a starburst,
// rippling on its pole.
private fun DrawScope.drawJuneteenthFlag(poleX: Float, baseY: Float, top: Float, seconds: Float) {
    drawLine(Pole, Offset(poleX, baseY), Offset(poleX, top), strokeWidth = 5f, cap = StrokeCap.Round)
    drawCircle(Gold, 6f, Offset(poleX, top - 4f))
    val w = 140f
    val h = 86f
    val strips = 14
    repeat(strips) { k ->
        val along = (k + 0.5f) / strips
        val lift = 6f * along * sin(along * 4f - seconds * 2.6f)
        val x = poleX + 2f + k * w / strips
        drawRect(JBlue, Offset(x, top + 6f + lift), Size(w / strips + 0.6f, h / 2))
        drawRect(JRed, Offset(x, top + 6f + h / 2 + lift), Size(w / strips + 0.6f, h / 2))
    }
    val sway = 6f * 0.45f * sin(0.45f * 4f - seconds * 2.6f)
    val c = Offset(poleX + 2f + w * 0.45f, top + 6f + h / 2 + sway)
    drawArc(JWhite, 180f, 180f, useCenter = false, topLeft = Offset(c.x - w * 0.5f, c.y - 10f), size = Size(w, 60f), style = Stroke(3f))
    val burst = Path().apply {
        repeat(24) { i ->
            val r = if (i % 2 == 0) 24f else 14f
            val a = Math.toRadians(-90.0 + i * 15.0)
            val p = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    drawPath(burst, JWhite, style = Stroke(2f))
    drawStar(c, 11f, JWhite)
}

internal fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    drawPath(Path().apply {
        repeat(10) { i ->
            val r = if (i % 2 == 0) radius else radius * 0.45f
            val a = Math.toRadians(-90.0 + i * 36.0)
            val p = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }, color)
}

// Triangle pennants swagged along the band in the given colors.
internal fun DrawScope.drawPennantBunting(seconds: Float, colors: List<Color>, y: Float = 660f, anchors: List<Float> = listOf(200f, 585f, 970f, 1355f, 1740f)) {
    var count = 0
    anchors.zipWithNext().forEach { (from, to) ->
        drawPath(Path().apply { moveTo(from, y); quadraticTo((from + to) / 2, y + 60f, to, y) }, Color(0xFF8A7B6A), style = Stroke(2f))
        repeat(8) { i ->
            val t = (i + 0.5f) / 8
            val at = Offset(from + (to - from) * t, y + 120f * t * (1f - t))
            if (abs(at.x - 970f) < 30f) return@repeat
            val sway = 5f * sin(seconds * 1.3f + count * 0.8f)
            withTransform({ rotate(sway, pivot = at) }) {
                drawPath(Path().apply { moveTo(at.x - 15f, at.y); lineTo(at.x + 15f, at.y); lineTo(at.x, at.y + 36f); close() }, colors[count % colors.size])
            }
            count++
        }
    }
}

internal fun DrawScope.drawBalloonCluster(x: Float, baseY: Float, colors: List<Color>, seconds: Float, phase: Float) {
    val knot = Offset(x, baseY - 8f)
    listOf(Offset(x - 30f, 760f), Offset(x + 4f, 744f), Offset(x + 34f, 764f)).forEachIndexed { i, center ->
        val p = phase + i * 2.1f
        val c = Offset(center.x + 3f * sin(seconds * 0.8f + p), center.y + 5f * sin(seconds * 1.2f + p))
        drawPath(Path().apply { moveTo(c.x, c.y + 30f); quadraticTo((c.x + knot.x) / 2, (c.y + knot.y) / 2, knot.x, knot.y) }, Color(0xFF9C8C7E), style = Stroke(1.5f))
        drawOval(colors[i % colors.size], Offset(c.x - 24f, c.y - 29f), Size(48f, 58f), alpha = 0.95f)
        drawOval(Color.White, Offset(c.x - 14f, c.y - 19f), Size(9f, 15f), alpha = 0.4f)
    }
    drawRoundRect(Color(0xFF8C8478), Offset(knot.x - 7f, knot.y), Size(14f, 9f), CornerRadius(3f))
}

// ---- Father's Day ----

private val GrillBlack = Color(0xFF2E2A28)
private val GrillShade = Color(0xFF4A4440)
private val Patty = Color(0xFF7A4A30)
private val Bun = Color(0xFFE2A85E)
private val ChairCanvas = Color(0xFF5FA8D3)
private val ChairFrame = Color(0xFF8C9AA6)
private val Smoke = Color(0xFFD6DCE2)
private val CoolerRed = Color(0xFFC9483F)

private val MugText = paint(android.graphics.Color.WHITE, 18f)

fun DrawScope.drawFathersDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawLawnChair(680f, 866f)
        drawGrill(960f, 866f, seconds)
        drawDadMug(1180f, 866f)
        drawCooler(1380f, 866f)
    }
}

private fun DrawScope.drawGrill(x: Float, baseY: Float, seconds: Float) {
    listOf(-40f, 0f, 40f).forEach { dx -> drawLine(GrillShade, Offset(x + dx * 0.5f, baseY - 80f), Offset(x + dx, baseY), strokeWidth = 5f) }
    drawArc(GrillBlack, 0f, 180f, useCenter = true, topLeft = Offset(x - 80f, baseY - 150f), size = Size(160f, 130f))
    drawRect(GrillShade, Offset(x - 84f, baseY - 90f), Size(168f, 6f))
    // Burgers on the grate, one flipping now and then.
    listOf(-46f, 0f, 46f).forEachIndexed { i, dx ->
        val flip = if (i == 1) ((seconds % 5f) / 5f).let { p -> if (p < 0.2f) sin(p / 0.2f * Math.PI.toFloat()) else 0f } else 0f
        drawOval(Patty, Offset(x + dx - 20f, baseY - 102f - 30f * flip), Size(40f, 12f))
    }
    // The lid propped open behind.
    drawArc(GrillShade, 180f, 180f, useCenter = true, topLeft = Offset(x - 80f, baseY - 190f), size = Size(160f, 110f))
    drawCircle(Color(0xFF8C9AA6), 6f, Offset(x, baseY - 182f))
    repeat(3) { i ->
        val p = (seconds * 0.3f + i / 3f) % 1f
        drawCircle(Smoke, 12f + 16f * p, Offset(x - 30f + i * 30f + 10f * sin(seconds + i), baseY - 140f - 110f * p), alpha = 0.5f * (1f - p))
    }
}

private fun DrawScope.drawLawnChair(x: Float, baseY: Float) {
    drawLine(ChairFrame, Offset(x - 50f, baseY), Offset(x + 30f, baseY - 70f), strokeWidth = 5f)
    drawLine(ChairFrame, Offset(x + 40f, baseY), Offset(x - 40f, baseY - 70f), strokeWidth = 5f)
    drawRect(ChairCanvas, Offset(x - 46f, baseY - 74f), Size(80f, 16f))
    drawPath(Path().apply { moveTo(x - 46f, baseY - 74f); lineTo(x - 70f, baseY - 160f); lineTo(x - 20f, baseY - 160f); lineTo(x + 4f, baseY - 74f); close() }, ChairCanvas)
    repeat(3) { i -> drawLine(Color.White, Offset(x - 62f + i * 14f, baseY - 158f), Offset(x - 40f + i * 14f, baseY - 76f), strokeWidth = 3f, alpha = 0.6f) }
}

private fun DrawScope.drawDadMug(x: Float, baseY: Float) {
    drawRoundRect(Color(0xFF3A5A92), Offset(x - 40f, baseY - 90f), Size(80f, 90f), CornerRadius(10f))
    drawArc(Color(0xFF3A5A92), -80f, 160f, useCenter = false, topLeft = Offset(x + 26f, baseY - 72f), size = Size(34f, 40f), style = Stroke(8f))
    drawIntoCanvas {
        it.nativeCanvas.drawText("#1", x, baseY - 54f, MugText)
        it.nativeCanvas.drawText("DAD", x, baseY - 30f, MugText)
    }
}

private fun DrawScope.drawCooler(x: Float, baseY: Float) {
    drawRoundRect(CoolerRed, Offset(x - 70f, baseY - 70f), Size(140f, 70f), CornerRadius(8f))
    drawRoundRect(Color(0xFFFBF6EC), Offset(x - 74f, baseY - 84f), Size(148f, 18f), CornerRadius(6f))
    drawRoundRect(Color(0xFF8C9AA6), Offset(x - 24f, baseY - 92f), Size(48f, 8f), CornerRadius(4f))
}

// ---- First day of summer ----

private val Petal = Color(0xFFF2C14E)
private val SeedHead = Color(0xFF7A5A40)
private val SunStem = Color(0xFF6E9C6D)
private val BallColors = listOf(Color(0xFFE8517A), Color(0xFFFBF6EC), Color(0xFF5FA8D3), Color(0xFFF2D24E))

fun DrawScope.drawSummerDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        listOf(260f, 340f, 420f, 1500f, 1580f, 1660f).forEachIndexed { i, x -> drawSunflower(x, 868f, 150f + (i % 3) * 30f, seconds, i) }
        // A beach ball bouncing in place, squashing a little as it lands.
        val p = (seconds % 1.4f) / 1.4f
        val height = 4f * p * (1f - p)
        val center = Offset(960f, 836f - 120f * height)
        val squash = if (height < 0.08f) 0.9f else 1f
        withTransform({ rotate(seconds * 60f, pivot = center); scale(1f / squash, squash, pivot = Offset(center.x, center.y + 34f)) }) {
            repeat(4) { k -> drawArc(BallColors[k], k * 90f, 90f, useCenter = true, topLeft = Offset(center.x - 34f, center.y - 34f), size = Size(68f, 68f)) }
            drawCircle(Color.White, 8f, center)
        }
        drawOval(Color(0xFF6E7884), Offset(960f - 30f + 10f * height, 862f), Size(60f - 20f * height, 8f), alpha = 0.2f)
    }
}

private fun DrawScope.drawSunflower(x: Float, baseY: Float, height: Float, seconds: Float, index: Int) {
    val nod = 3f * sin(seconds * 0.9f + index)
    val head = Offset(x + nod, baseY - height)
    drawLine(SunStem, Offset(x, baseY), head, strokeWidth = 5f)
    drawOval(SunStem, Offset(x - 26f, baseY - height * 0.45f), Size(26f, 12f))
    drawOval(SunStem, Offset(x, baseY - height * 0.6f), Size(26f, 12f))
    repeat(12) { k ->
        withTransform({ rotate(k * 30f, pivot = head) }) { drawOval(Petal, Offset(head.x - 6f, head.y - 34f), Size(12f, 24f)) }
    }
    drawCircle(SeedHead, 16f, head)
}

// ---- Fourth of July ----

private val Patriotic = listOf(Color(0xFFD9534F), Color(0xFFFBF6EC), Color(0xFF5F8FD3))

private val JulyFireworks = listOf(
    Firework(x = 340f, peakY = 700f, radius = 86f, cycleSeconds = 6.4f, offsetSeconds = 0f, colorIndex = 0, launchY = 850f),
    Firework(x = 640f, peakY = 690f, radius = 70f, cycleSeconds = 7.6f, offsetSeconds = 2.4f, colorIndex = 1, launchY = 850f),
    Firework(x = 1280f, peakY = 696f, radius = 78f, cycleSeconds = 7.0f, offsetSeconds = 4.6f, colorIndex = 2, launchY = 850f),
    Firework(x = 1580f, peakY = 706f, radius = 90f, cycleSeconds = 8.2f, offsetSeconds = 1.3f, colorIndex = 0, launchY = 850f),
    Firework(x = 960f, peakY = 680f, radius = 96f, cycleSeconds = 9.4f, offsetSeconds = 6.1f, colorIndex = 2, launchY = 850f),
)

fun DrawScope.drawFourthOfJulyFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        JulyFireworks.forEach { fw -> fw.poseAt(seconds)?.let { drawFirework(it, fw, seconds, Patriotic) } }
        drawPennantBunting(seconds, listOf(Color(0xFFC9483F), Color(0xFFFBF6EC), Color(0xFF3A5A92)), y = 650f)
        listOf(300f, 360f, 1560f, 1620f).forEachIndexed { i, x -> drawHandFlag(x, 876f, seconds, i * 1.2f) }
    }
}
