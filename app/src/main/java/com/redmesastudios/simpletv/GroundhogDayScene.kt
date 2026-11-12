package com.redmesastudios.simpletv

import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.sin

// Groundhog Day (February 2nd) on the winter scene: a low winter sun, a burrow in the snow
// with a groundhog that pops up, looks left and right, sees its shadow stretch out beside it,
// gives a start and ducks back down; a wooden sign asks "6 MORE WEEKS?". The greeting says
// "Happy Groundhog Day!". Drawn in design px (1920x1080).

private val WinterSun = Color(0xFFF6CF7A)
private val Dirt = Color(0xFF8C6A4F)
private val DirtDark = Color(0xFF5E4534)
private val Fur = Color(0xFF8A6446)
private val FurLight = Color(0xFFB08A66)
private val Nose = Color(0xFF2A211B)
private val Tooth = Color(0xFFFBF6EC)
private val ShadowGray = Color(0xFF7C8796)
private val Wood = Color(0xFFA9845E)
private val WoodDark = Color(0xFF7A5A40)

private val SignText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xFB, 0xF6, 0xEC)
    textSize = 24f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.06f
}

private const val BurrowX = 960f
private const val BurrowY = 866f

fun DrawScope.drawGroundhogDayFigures(unit: Float, seconds: Float) {
    val pose = groundhogAt(seconds)
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawCircle(WinterSun, 46f, Offset(560f, 720f), alpha = 0.85f)
        drawCircle(WinterSun, 76f, Offset(560f, 720f), alpha = 0.18f)
        drawSign(1300f, BurrowY)
        // The burrow: a dirt mound with a dark hole.
        drawOval(Dirt, Offset(BurrowX - 90f, BurrowY - 22f), Size(180f, 34f))
        drawOval(DirtDark, Offset(BurrowX - 44f, BurrowY - 16f), Size(88f, 20f))
        // Its shadow, cast away from the sun, once it's mostly out.
        if (pose.rise > 0.5f) {
            val reach = (pose.rise - 0.5f) * 2f
            drawGroundhogShadow(reach)
        }
        // The groundhog shows only above the hole's rim.
        clipRect(0f, 0f, SceneWidth, BurrowY - 6f) {
            drawGroundhog(pose, seconds)
        }
        drawOval(Dirt, Offset(BurrowX - 52f, BurrowY - 10f), Size(104f, 16f))
    }
}

private fun DrawScope.drawGroundhogShadow(reach: Float) {
    withTransform({ scale(1f, 0.32f, pivot = Offset(BurrowX + 30f, BurrowY)) }) {
        drawPath(Path().apply {
            moveTo(BurrowX + 20f, BurrowY)
            quadraticTo(BurrowX + 70f * reach + 40f, BurrowY - 60f, BurrowX + 150f * reach + 30f, BurrowY - 10f)
            lineTo(BurrowX + 150f * reach + 30f, BurrowY + 10f)
            close()
        }, ShadowGray, alpha = 0.45f * reach)
        drawCircle(ShadowGray, 30f * reach, Offset(BurrowX + 150f * reach + 20f, BurrowY - 18f), alpha = 0.45f * reach)
    }
}

// Upright, paws held up in front. Drawn standing in the hole; rise lifts it out.
private fun DrawScope.drawGroundhog(pose: GroundhogPose, seconds: Float) {
    val sink = (1f - pose.rise) * 130f + if (pose.startled) -6f else 0f
    withTransform({ translate(BurrowX, BurrowY - 10f + sink) }) {
        val turn = pose.look * 6f
        drawOval(Fur, Offset(-34f, -86f), Size(68f, 100f))
        drawOval(FurLight, Offset(-20f, -66f), Size(40f, 66f))
        // Paws.
        drawOval(Fur, Offset(-22f, -64f), Size(16f, 12f))
        drawOval(Fur, Offset(6f, -64f), Size(16f, 12f))
        // Head, ears, face.
        val head = Offset(turn, -100f)
        drawCircle(Fur, 9f, Offset(head.x - 22f, head.y - 18f))
        drawCircle(Fur, 9f, Offset(head.x + 22f, head.y - 18f))
        drawOval(Fur, Offset(head.x - 30f, head.y - 28f), Size(60f, 52f))
        drawOval(FurLight, Offset(head.x - 16f + turn * 0.6f, head.y - 2f), Size(32f, 22f))
        val eyeSize = if (pose.startled) 4.4f else 3.2f
        drawCircle(Nose, eyeSize, Offset(head.x - 11f + turn * 0.6f, head.y - 8f))
        drawCircle(Nose, eyeSize, Offset(head.x + 11f + turn * 0.6f, head.y - 8f))
        if (pose.startled) {
            drawCircle(Tooth, 1.4f, Offset(head.x - 10f + turn * 0.6f, head.y - 9f))
            drawCircle(Tooth, 1.4f, Offset(head.x + 12f + turn * 0.6f, head.y - 9f))
        }
        // A twitching nose and two front teeth.
        val twitch = 0.6f * sin(seconds * 14f)
        drawOval(Nose, Offset(head.x - 6f + turn * 0.7f, head.y + 2f + twitch), Size(12f, 8f))
        drawRoundRect(Tooth, Offset(head.x - 4f + turn * 0.7f, head.y + 12f), Size(8f, 7f), CornerRadius(1.5f))
        drawLine(Nose, Offset(head.x + turn * 0.7f, head.y + 12f), Offset(head.x + turn * 0.7f, head.y + 19f), strokeWidth = 0.8f)
    }
}

private fun DrawScope.drawSign(x: Float, baseY: Float) {
    val w = 224f
    val h = 54f
    val top = baseY - 120f
    drawLine(WoodDark, Offset(x, baseY), Offset(x, top + h), strokeWidth = 8f, cap = StrokeCap.Round)
    withTransform({ rotate(-4f, pivot = Offset(x, top + h / 2)) }) {
        drawRoundRect(Wood, Offset(x - w / 2, top), Size(w, h), CornerRadius(6f))
        drawLine(WoodDark, Offset(x - w / 2 + 10f, top + h - 12f), Offset(x + w / 2 - 30f, top + h - 12f), strokeWidth = 1.5f, alpha = 0.5f)
        // A little point on the right, like a signpost arrow.
        drawPath(Path().apply { moveTo(x + w / 2, top); lineTo(x + w / 2 + 22f, top + h / 2); lineTo(x + w / 2, top + h); close() }, Wood)
        // A cap of snow along the top.
        drawRoundRect(Color(0xFFFAFBFC), Offset(x - w / 2 + 2f, top - 6f), Size(w + 6f, 10f), CornerRadius(5f))
        drawIntoCanvas { it.nativeCanvas.drawText("6 MORE WEEKS?", x, top + h / 2 + 9f, SignText) }
    }
}
