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
import kotlin.math.sin

// April Fools' Day on the spring scene: a jack-in-the-box whose crank turns until the lid
// flies open and a clown springs out, bobbing; a sign hung upside down; a banana peel on the
// path; and a whoopee cushion that slowly fills and flattens. The greeting says "Happy April
// Fools' Day!". Drawn in design px (1920x1080).

private val BoxColors = listOf(Color(0xFFE8796B), Color(0xFF7FB7D9), Color(0xFFF2C14E), Color(0xFF9BCB8C))
private val BoxEdge = Color(0xFFFBF6EC)
private val Crank = Color(0xFF8C9AA6)
private val Spring = Color(0xFF8C9AA6)
private val ClownFace = Color(0xFFFBF1E4)
private val ClownNose = Color(0xFFD9534F)
private val ClownHair = Color(0xFFF2A93B)
private val Banana = Color(0xFFF7D24E)
private val BananaDark = Color(0xFFC9A23A)
private val Cushion = Color(0xFFE88AA2)
private val CushionDark = Color(0xFFC2477A)
private val SignWood = Color(0xFFA9845E)
private val SignDark = Color(0xFF7A5A40)

private val SignText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xFB, 0xF6, 0xEC)
    textSize = 30f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.06f
}

fun DrawScope.drawAprilFoolsFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawUpsideDownSign(1340f, 866f, seconds)
        drawBananaPeel(620f, 866f)
        drawWhoopeeCushion(1620f, 866f, seconds)
        drawJackInTheBox(940f, 866f, jackAt(seconds), seconds)
    }
}

private fun DrawScope.drawJackInTheBox(x: Float, baseY: Float, pose: JackPose, seconds: Float) {
    val size = 96f
    val top = baseY - size
    // The clown on its spring, behind the box front.
    if (pose.pop > 0.02f) {
        val rise = 110f * pose.pop
        val head = Offset(x + 4f * sin(seconds * 3f) * pose.pop, top - rise)
        val coils = 7
        val spring = Path().apply {
            moveTo(x, top + 10f)
            repeat(coils) { k ->
                val y0 = top + 10f - (k + 0.5f) * (rise + 10f) / coils
                val y1 = top + 10f - (k + 1f) * (rise + 10f) / coils
                quadraticTo(x + if (k % 2 == 0) 22f else -22f, y0, x + (head.x - x) * (k + 1f) / coils, y1)
            }
        }
        drawPath(spring, Spring, style = Stroke(3.5f, cap = StrokeCap.Round))
        // Ruff collar, face, hair tufts, nose, smile, hat.
        repeat(6) { k -> drawCircle(BoxColors[k % BoxColors.size], 9f, Offset(head.x - 25f + k * 10f, head.y + 26f)) }
        drawCircle(ClownHair, 13f, Offset(head.x - 24f, head.y + 2f))
        drawCircle(ClownHair, 13f, Offset(head.x + 24f, head.y + 2f))
        drawCircle(ClownFace, 26f, head)
        drawCircle(ClownNose, 7f, Offset(head.x, head.y + 4f))
        drawCircle(Color(0xFF2A211B), 3f, Offset(head.x - 9f, head.y - 7f))
        drawCircle(Color(0xFF2A211B), 3f, Offset(head.x + 9f, head.y - 7f))
        drawArc(ClownNose, 20f, 140f, useCenter = false, topLeft = Offset(head.x - 13f, head.y + 2f), size = Size(26f, 16f), style = Stroke(3f, cap = StrokeCap.Round))
        drawPath(Path().apply { moveTo(head.x - 16f, head.y - 20f); lineTo(head.x + 4f, head.y - 58f); lineTo(head.x + 16f, head.y - 18f); close() }, BoxColors[1])
        drawCircle(BoxColors[2], 6f, Offset(head.x + 4f, head.y - 58f))
    }
    // The box, its sides in four colors, with a question mark.
    drawRect(BoxColors[0], Offset(x - size / 2, top), Size(size, size))
    drawRect(BoxEdge, Offset(x - size / 2, top), Size(size, size), style = Stroke(3f))
    drawPath(Path().apply {
        moveTo(x - 12f, top + 34f); quadraticTo(x - 12f, top + 18f, x, top + 18f); quadraticTo(x + 14f, top + 18f, x + 12f, top + 34f)
        quadraticTo(x + 10f, top + 44f, x, top + 50f); lineTo(x, top + 60f)
    }, BoxEdge, style = Stroke(6f, cap = StrokeCap.Round))
    drawCircle(BoxEdge, 4f, Offset(x, top + 74f))
    // The lid, hinged at the back left, swinging open.
    withTransform({ rotate(-120f * pose.lid, pivot = Offset(x - size / 2, top)) }) {
        drawRect(BoxColors[3], Offset(x - size / 2, top - 10f), Size(size, 10f))
        drawRect(BoxEdge, Offset(x - size / 2, top - 10f), Size(size, 10f), style = Stroke(2f))
    }
    // The crank on the right side.
    val hub = Offset(x + size / 2 + 6f, top + size / 2)
    drawCircle(Crank, 5f, hub)
    withTransform({ rotate(pose.crankDegrees, pivot = hub) }) {
        drawLine(Crank, hub, Offset(hub.x + 20f, hub.y), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(Crank, Offset(hub.x + 20f, hub.y), Offset(hub.x + 28f, hub.y), strokeWidth = 7f, cap = StrokeCap.Round)
    }
}

// A sign that says APRIL FOOLS!, hung upside down, swinging a little.
private fun DrawScope.drawUpsideDownSign(x: Float, baseY: Float, seconds: Float) {
    val w = 210f
    val h = 60f
    val top = baseY - 130f
    drawLine(SignDark, Offset(x, baseY), Offset(x, top + h), strokeWidth = 8f, cap = StrokeCap.Round)
    withTransform({ rotate(180f + 3f * sin(seconds * 1.2f), pivot = Offset(x, top + h / 2)) }) {
        drawRoundRect(SignWood, Offset(x - w / 2, top), Size(w, h), CornerRadius(6f))
        drawRoundRect(SignDark, Offset(x - w / 2 + 5f, top + 5f), Size(w - 10f, h - 10f), CornerRadius(4f), style = Stroke(1.5f))
        drawIntoCanvas { it.nativeCanvas.drawText("APRIL FOOLS!", x, top + h / 2 + 11f, SignText) }
    }
}

private fun DrawScope.drawBananaPeel(x: Float, baseY: Float) {
    val y = baseY - 8f
    listOf(-150f, -30f, 90f).forEach { angle ->
        withTransform({ rotate(angle, pivot = Offset(x, y)) }) {
            drawPath(Path().apply { moveTo(x - 6f, y); quadraticTo(x - 14f, y - 34f, x, y - 44f); quadraticTo(x + 10f, y - 30f, x + 6f, y); close() }, Banana)
            drawPath(Path().apply { moveTo(x, y); quadraticTo(x - 6f, y - 26f, x, y - 40f) }, BananaDark, style = Stroke(1.2f))
        }
    }
    drawOval(Banana, Offset(x - 12f, y - 10f), Size(24f, 16f))
    drawRoundRect(BananaDark, Offset(x - 3f, y - 18f), Size(6f, 10f), CornerRadius(2f))
}

// A whoopee cushion that slowly puffs up and flattens again.
private fun DrawScope.drawWhoopeeCushion(x: Float, baseY: Float, seconds: Float) {
    val puff = 0.5f + 0.5f * sin(seconds * 0.9f)
    val h = 18f + 22f * puff
    drawOval(CushionDark, Offset(x - 50f, baseY - h), Size(100f, h))
    drawOval(Cushion, Offset(x - 48f, baseY - h - 3f), Size(96f, h))
    drawOval(Color.White, Offset(x - 30f, baseY - h + 2f), Size(22f, 6f), alpha = 0.35f)
    drawPath(Path().apply { moveTo(x + 46f, baseY - h / 2 - 6f); lineTo(x + 66f, baseY - h / 2 - 2f); lineTo(x + 66f, baseY - h / 2 + 6f); lineTo(x + 46f, baseY - h / 2 + 4f); close() }, CushionDark)
}
