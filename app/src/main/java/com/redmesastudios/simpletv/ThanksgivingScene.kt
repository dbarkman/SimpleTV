package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.abs
import kotlin.math.sin

// Thanksgiving on the fall scene: corn stalks at both ends of the hill, a hay bale piled
// with pumpkins and gourds, a cornucopia spilling fruit, and now and then a turkey strutting
// across the hill, stopping in the middle to fan its tail and gobble. Drawn in design px
// (1920x1080).

private val TurkeyBody = Color(0xFF7A4E33)
private val TurkeyWing = Color(0xFF66412B)
private val TurkeyHead = Color(0xFFC9C2D2)
private val Wattle = Color(0xFFC2493B)
private val Beak = Color(0xFFE0A84A)
private val TurkeyLegs = Color(0xFFC9873E)
private val FeatherBands = listOf(Color(0xFFE2B66E), Color(0xFFA8703F), Color(0xFF8A5A3B), Color(0xFF6E4630))
private val Wicker = Color(0xFFC9A26B)
private val WickerShade = Color(0xFFA9824F)
private val HarvestOrange = listOf(Color(0xFFD9772E), Color(0xFFE48A3E), Color(0xFFCC6A28))
private val PumpkinRib = Color(0xFFB85F22)
private val Stem = Color(0xFF5E5A2E)
private val Grape = Color(0xFF7B4E7A)
private val Apple = Color(0xFFC0503A)
private val Pear = Color(0xFFD9B54A)
private val GourdColors = listOf(Color(0xFFE3C45A), Color(0xFF9AA35A), Color(0xFFEFD9A0))
private val FruitLeaf = Color(0xFF8A9A5B)
private val TurkeyEye = Color(0xFF2A211B)

val Turkey = Marcher(cycleSeconds = 70f, walkSeconds = 20f, pauseSeconds = 6f, stopX = 990f, offsetSeconds = 5f, baseY = 870f, strideSeconds = 0.8f)

// The moving parts, redrawn with the falling leaves.
fun DrawScope.drawThanksgivingFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawCornStalks(190f, 862f)
        drawCornStalks(1690f, 856f)
        drawHayBale(430f, 860f)
        drawHarvestPumpkin(404f, 806f, 58f, 0)
        drawHarvestPumpkin(458f, 808f, 42f, 1)
        drawHarvestPumpkin(532f, 864f, 70f, 2)
        drawGourd(352f, 866f, 0, -20f)
        drawGourd(600f, 866f, 1, 14f)
        drawCornucopia(1420f, 858f)
        drawHarvestPumpkin(1560f, 866f, 64f, 0)
        drawGourd(1612f, 868f, 2, -10f)
        Turkey.poseAt(seconds)?.let { drawTurkey(it, seconds) }
    }
}

private fun DrawScope.drawHarvestPumpkin(x: Float, baseY: Float, width: Float, colorIndex: Int) {
    val h = width * 0.76f
    val color = HarvestOrange[colorIndex % HarvestOrange.size]
    drawOval(color, Offset(x - width / 2, baseY - h), Size(width, h))
    // Ribs: two inner lobes outlined.
    drawOval(PumpkinRib, Offset(x - width * 0.26f, baseY - h), Size(width * 0.52f, h), style = Stroke(1.6f), alpha = 0.55f)
    drawOval(PumpkinRib, Offset(x - width * 0.08f, baseY - h), Size(width * 0.16f, h), style = Stroke(1.4f), alpha = 0.45f)
    drawLine(Stem, Offset(x, baseY - h + 2f), Offset(x + 4f, baseY - h - width * 0.16f), strokeWidth = width * 0.09f, cap = StrokeCap.Round)
}

// A small squash lying on its side.
private fun DrawScope.drawGourd(x: Float, baseY: Float, colorIndex: Int, tilt: Float) {
    withTransform({ rotate(tilt, pivot = Offset(x, baseY - 12f)) }) {
        val color = GourdColors[colorIndex % GourdColors.size]
        drawOval(color, Offset(x - 22f, baseY - 22f), Size(30f, 22f))
        drawOval(color, Offset(x + 2f, baseY - 18f), Size(22f, 14f))
        drawLine(Stem, Offset(x + 22f, baseY - 11f), Offset(x + 30f, baseY - 14f), strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

// A wicker horn lying on the hill, its open mouth to the right spilling fruit.
private fun DrawScope.drawCornucopia(x: Float, baseY: Float) {
    val horn = Path().apply {
        moveTo(x + 40f, baseY - 78f)
        // The top edge narrows toward the tip, which curls up and back over itself.
        cubicTo(x - 10f, baseY - 78f, x - 56f, baseY - 66f, x - 92f, baseY - 84f)
        cubicTo(x - 110f, baseY - 94f, x - 122f, baseY - 112f, x - 104f, baseY - 122f)
        cubicTo(x - 92f, baseY - 128f, x - 84f, baseY - 112f, x - 96f, baseY - 106f)
        cubicTo(x - 96f, baseY - 84f, x - 56f, baseY - 22f, x + 40f, baseY - 6f)
        close()
    }
    drawPath(horn, Wicker)
    // Woven bands around the horn, bowed like its cross-section.
    listOf(Triple(-62f, -74f, -50f), Triple(-32f, -72f, -28f), Triple(-2f, -75f, -14f), Triple(24f, -78f, -8f)).forEach { (dx, top, bottom) ->
        drawPath(Path().apply {
            moveTo(x + dx, baseY + top + 2f)
            quadraticTo(x + dx + 10f, baseY + (top + bottom) / 2, x + dx, baseY + bottom - 2f)
        }, WickerShade, style = Stroke(3f, cap = StrokeCap.Round), alpha = 0.8f)
    }
    drawPath(horn, WickerShade, style = Stroke(2f))
    // The mouth.
    drawOval(WickerShade, Offset(x + 26f, baseY - 78f), Size(28f, 74f))
    drawOval(Color(0xFF7A5A36), Offset(x + 30f, baseY - 72f), Size(20f, 62f))
    // Fruit tumbling out.
    drawCircle(FruitLeaf, 14f, Offset(x + 52f, baseY - 64f))
    listOf(Offset(x + 58f, baseY - 50f), Offset(x + 70f, baseY - 52f), Offset(x + 64f, baseY - 40f), Offset(x + 76f, baseY - 40f), Offset(x + 70f, baseY - 29f))
        .forEach { drawCircle(Grape, 8f, it) }
    drawCircle(Apple, 17f, Offset(x + 100f, baseY - 17f))
    drawLine(Stem, Offset(x + 100f, baseY - 33f), Offset(x + 103f, baseY - 40f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    drawOval(Pear, Offset(x + 40f, baseY - 34f), Size(26f, 32f))
    drawCircle(Pear, 9f, Offset(x + 53f, baseY - 36f))
    drawHarvestPumpkin(x + 136f, baseY - 2f, 34f, 1)
}

// pose.raise is how far the tail is fanned: folded while walking, open while it stops.
private fun DrawScope.drawTurkey(pose: MarcherPose, seconds: Float) {
    withTransform({ scale(TurkeyScale, TurkeyScale, pivot = Offset(pose.x, pose.y)) }) { drawTurkeyAtSize(pose, seconds) }
}

// Drawn about 120 px tall, then scaled up from its feet.
private const val TurkeyScale = 1.45f

private fun DrawScope.drawTurkeyAtSize(pose: MarcherPose, seconds: Float) {
    val x = pose.x
    val baseY = pose.y
    // Legs, swinging with the stride.
    listOf(1f, -1f).forEach { side ->
        val foot = Offset(x - 4f + side * 11f * pose.stride, baseY)
        drawLine(TurkeyLegs, Offset(x - 4f + side * 4f, baseY - 34f), foot, strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(TurkeyLegs, foot, Offset(foot.x + 9f, foot.y), strokeWidth = 3f, cap = StrokeCap.Round)
    }
    // The tail fan behind the body: bands of feathers, wider when fanned.
    val fan = Offset(x - 22f, baseY - 54f)
    val spread = 70f + 110f * pose.raise
    val radius = 34f + 26f * pose.raise
    FeatherBands.forEachIndexed { i, color ->
        val r = radius * (1f - i * 0.2f)
        drawArc(color, 270f - spread / 2 - 25f * (1f - pose.raise), spread, useCenter = true, topLeft = Offset(fan.x - r, fan.y - r), size = Size(2 * r, 2 * r))
    }
    // Feather lines across the fan.
    repeat(7) { i ->
        val angle = Math.toRadians((270.0 - spread / 2 - 25 * (1 - pose.raise) + spread * (i + 0.5) / 7))
        drawLine(FeatherBands[3], fan, Offset(fan.x + radius * kotlin.math.cos(angle).toFloat(), fan.y + radius * kotlin.math.sin(angle).toFloat()), strokeWidth = 1.2f, alpha = 0.5f)
    }
    drawOval(TurkeyBody, Offset(x - 32f, baseY - 76f), Size(58f, 46f))
    drawOval(TurkeyWing, Offset(x - 26f, baseY - 66f), Size(34f, 22f))
    // Neck and head: bobbing with each step, gobbling while it stops.
    val bob = 3f * abs(pose.stride) + 3f * pose.raise * sin(seconds * 14f)
    val head = Offset(x + 30f + bob * 0.6f, baseY - 92f + bob)
    drawLine(TurkeyHead, Offset(x + 16f, baseY - 64f), head, strokeWidth = 9f, cap = StrokeCap.Round)
    drawCircle(TurkeyHead, 9.5f, head)
    drawPath(Path().apply {
        moveTo(head.x + 8f, head.y - 3f); lineTo(head.x + 17f, head.y + 1f); lineTo(head.x + 8f, head.y + 3f); close()
    }, Beak)
    drawOval(Wattle, Offset(head.x + 4f, head.y + 2f), Size(7f, 16f + 3f * pose.raise))
    drawCircle(TurkeyEye, 1.8f, Offset(head.x + 3f, head.y - 3f))
}
