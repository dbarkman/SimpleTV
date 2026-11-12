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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.cos
import kotlin.math.sin

// National Pizza Day (February 9th, inside Valentine's week, which it takes for the day): a
// big pepperoni pizza on a wooden board in the middle of the hill, steam rising off it, a
// slice lifted out and set back now and then with its cheese stretching, a stack of pizza
// boxes and a parmesan shaker. The greeting says "Happy Pizza Day!". Drawn in design px
// (1920x1080).

private val Board = Color(0xFFB98A5E)
private val BoardDark = Color(0xFF8C6A4F)
private val Crust = Color(0xFFE2A85E)
private val CrustDark = Color(0xFFC98A44)
private val Sauce = Color(0xFFD0573E)
private val Cheese = Color(0xFFF7D27A)
private val CheeseLight = Color(0xFFFBE3A1)
private val Pepperoni = Color(0xFFB8432E)
private val PepperoniDark = Color(0xFF953623)
private val Basil = Color(0xFF5E8C61)
private val Steam = Color(0xFFD6DCE2)
private val BoxBrown = Color(0xFFD9B98A)
private val BoxEdge = Color(0xFFB8956A)
private val BoxRed = Color(0xFFC0503A)
private val Shaker = Color(0xFFE6EEF2)
private val ShakerLid = Color(0xFF8C9AA6)

private val BoxText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xC0, 0x50, 0x3A)
    textSize = 20f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.18f
}

private val PizzaCenter = Offset(960f, 826f)
private const val PizzaRx = 130f
private const val PizzaRy = 44f
private const val SliceFrom = 10f
private const val SliceTo = 60f

// Pepperoni spots, as fractions of the pizza's radii.
private val Spots = listOf(
    -0.55f to -0.25f, -0.15f to -0.5f, 0.3f to -0.45f, -0.35f to 0.3f, 0.05f to 0.05f,
    0.6f to -0.05f, -0.7f to 0.15f, 0.25f to 0.55f, 0.55f to 0.35f, -0.05f to 0.6f,
)

fun DrawScope.drawPizzaDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawPizzaBoxes(560f, 866f)
        drawShaker(1250f, 866f)
        drawPizza(seconds)
    }
}

private fun pointOn(angleDegrees: Float, scale: Float = 1f): Offset {
    val a = Math.toRadians(angleDegrees.toDouble())
    return Offset(PizzaCenter.x + PizzaRx * scale * cos(a).toFloat(), PizzaCenter.y + PizzaRy * scale * sin(a).toFloat())
}

private fun wedge(from: Float, to: Float, scale: Float = 1f) = Path().apply {
    moveTo(PizzaCenter.x, PizzaCenter.y)
    var a = from
    while (a <= to) { val p = pointOn(a, scale); lineTo(p.x, p.y); a += 4f }
    val end = pointOn(to, scale); lineTo(end.x, end.y)
    close()
}

private fun DrawScope.drawPizzaTop(path: Path) {
    drawPath(path, Crust)
    clipPath(path) {
        drawOval(Sauce, Offset(PizzaCenter.x - PizzaRx * 0.88f, PizzaCenter.y - PizzaRy * 0.88f), Size(PizzaRx * 1.76f, PizzaRy * 1.76f))
        drawOval(Cheese, Offset(PizzaCenter.x - PizzaRx * 0.84f, PizzaCenter.y - PizzaRy * 0.84f), Size(PizzaRx * 1.68f, PizzaRy * 1.68f))
        Spots.forEachIndexed { i, (fx, fy) ->
            val at = Offset(PizzaCenter.x + fx * PizzaRx, PizzaCenter.y + fy * PizzaRy)
            drawOval(PepperoniDark, Offset(at.x - 11f, at.y - 4.5f), Size(22f, 10f))
            drawOval(Pepperoni, Offset(at.x - 10f, at.y - 5f), Size(20f, 8.5f))
            if (i % 3 == 0) drawOval(Basil, Offset(at.x + 12f, at.y - 8f), Size(10f, 5f))
            drawOval(CheeseLight, Offset(at.x - 20f, at.y + 4f), Size(10f, 4f), alpha = 0.7f)
        }
    }
}

private fun DrawScope.drawPizza(seconds: Float) {
    // The board under it, with a handle to the left.
    drawRoundRect(BoardDark, Offset(PizzaCenter.x - 250f, PizzaCenter.y - 6f), Size(100f, 16f), CornerRadius(8f))
    drawOval(BoardDark, Offset(PizzaCenter.x - 160f, PizzaCenter.y - 46f), Size(320f, 106f))
    drawOval(Board, Offset(PizzaCenter.x - 160f, PizzaCenter.y - 52f), Size(320f, 100f))
    // The pizza's edge, then its top with the lifted slice's gap.
    drawOval(CrustDark, Offset(PizzaCenter.x - PizzaRx, PizzaCenter.y - PizzaRy + 6f), Size(PizzaRx * 2, PizzaRy * 2))
    val lift = sliceLift(seconds)
    val whole = Path().apply {
        moveTo(PizzaCenter.x, PizzaCenter.y)
        var a = SliceTo
        while (a <= SliceFrom + 360f) { val p = pointOn(a); lineTo(p.x, p.y); a += 4f }
        close()
    }
    drawPizzaTop(if (lift > 0f) whole else whole.also { it.addPath(wedge(SliceFrom, SliceTo)) })
    if (lift > 0f) drawPath(wedge(SliceFrom, SliceTo), BoardDark, alpha = 0.35f)
    drawPath(wedge(SliceFrom, SliceTo), CrustDark, style = Stroke(1.2f), alpha = 0.5f)
    // The slice, lifted and tipped, with strings of cheese back down to the pie.
    if (lift > 0f) {
        val raise = Offset(16f * lift, -70f * lift)
        val tipEnd = pointOn((SliceFrom + SliceTo) / 2, 0.15f)
        repeat(4) { i ->
            val from = Offset(tipEnd.x + 4f + i * 8f, tipEnd.y + 2f + i * 3f)
            val to = Offset(from.x + raise.x, from.y + raise.y)
            drawPath(Path().apply { moveTo(from.x, from.y); quadraticTo((from.x + to.x) / 2 + 6f, (from.y + to.y) / 2 + 8f, to.x, to.y) },
                Cheese, style = Stroke(3.2f * (1f - 0.6f * lift), cap = StrokeCap.Round), alpha = 1f - 0.4f * lift)
        }
        val sliceMiddle = pointOn((SliceFrom + SliceTo) / 2, 0.55f)
        withTransform({ translate(raise.x, raise.y); rotate(-8f * lift, pivot = sliceMiddle) }) {
            val slice = wedge(SliceFrom, SliceTo)
            drawPizzaTop(slice)
            drawPath(slice, CrustDark, style = Stroke(1.5f))
        }
    }
    // Steam curling up off the pie.
    repeat(3) { i ->
        val p = (seconds * 0.35f + i / 3f) % 1f
        val x = PizzaCenter.x - 60f + i * 60f + 10f * sin(seconds * 1.5f + i)
        val y = PizzaCenter.y - 40f - 90f * p
        drawPath(Path().apply { moveTo(x, y + 20f); quadraticTo(x + 10f, y + 10f, x, y); quadraticTo(x - 10f, y - 10f, x, y - 20f) },
            Steam, style = Stroke(4f, cap = StrokeCap.Round), alpha = 0.6f * sin(Math.PI.toFloat() * p))
    }
}

private fun DrawScope.drawPizzaBoxes(x: Float, baseY: Float) {
    val w = 150f
    listOf(0f, 3f, -4f).forEachIndexed { i, shift ->
        val bottom = baseY - i * 22f
        drawRect(BoxBrown, Offset(x - w / 2 + shift, bottom - 22f), Size(w, 22f))
        drawRect(BoxEdge, Offset(x - w / 2 + shift, bottom - 22f), Size(w, 22f), style = Stroke(1.5f))
    }
    val top = baseY - 66f
    drawPath(Path().apply { moveTo(x - w / 2 - 4f, top); lineTo(x + w / 2 - 4f, top); lineTo(x + w / 2 - 22f, top - 22f); lineTo(x - w / 2 + 14f, top - 22f); close() }, BoxBrown)
    drawCircle(BoxRed, 4f, Offset(x - 50f, top - 11f))
    drawIntoCanvas { it.nativeCanvas.drawText("PIZZA", x - 4f, top - 4f, BoxText) }
}

private fun DrawScope.drawShaker(x: Float, baseY: Float) {
    drawRoundRect(Shaker, Offset(x - 18f, baseY - 64f), Size(36f, 64f), CornerRadius(8f))
    drawRoundRect(ShakerLid, Offset(x - 19f, baseY - 76f), Size(38f, 16f), CornerRadius(5f))
    repeat(3) { i -> drawCircle(Shaker, 2f, Offset(x - 8f + i * 8f, baseY - 70f)) }
    drawRect(CheeseLight, Offset(x - 18f, baseY - 34f), Size(36f, 28f), alpha = 0.8f)
}
