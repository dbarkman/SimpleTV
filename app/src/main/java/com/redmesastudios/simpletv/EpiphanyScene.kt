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

// Epiphany (January 6th) on the winter scene: a bright star shining over the hill with a long
// ray of light, smaller stars twinkling, and now and then three wise men on camels walking
// across, each carrying a gift. The greeting says "Happy Epiphany". Drawn in design px
// (1920x1080).

private val StarLight = Color(0xFFF7D27A)
private val StarCore = Color(0xFFFFF6DC)
private val Camel = Color(0xFFC9A16E)
private val CamelShade = Color(0xFFAE8656)
private val CamelHoof = Color(0xFF6E5440)
private val Saddle = Color(0xFFC0503A)
private val Robes = listOf(Color(0xFF8C5A9C), Color(0xFF3A5A92), Color(0xFF5E8C61))
private val Crown = Color(0xFFE9C14A)
private val Skins = listOf(Color(0xFFF1C9A5), Color(0xFF8A5A3B), Color(0xFFC98E62))
private val Gifts = listOf(Color(0xFFE9C14A), Color(0xFFC0503A), Color(0xFF7FB7D9))

private val EpiphanyTwinkles = listOf(260f to 720f, 520f to 760f, 760f to 700f, 1180f to 708f, 1420f to 752f, 1680f to 716f)

private const val CamelScale = 1.1f

fun DrawScope.drawEpiphanyFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        EpiphanyTwinkles.forEachIndexed { i, (x, y) -> drawTwinkle(x, y, seconds, i) }
        drawGuidingStar(960f, 700f, seconds)
        Caravan.forEachIndexed { i, walker -> walker.poseAt(seconds)?.let { drawCamelRider(it, i, seconds) } }
    }
}

// A big four-pointed star with a soft glow, its long lower ray reaching toward the hill.
private fun DrawScope.drawGuidingStar(x: Float, y: Float, seconds: Float) {
    val glow = 0.8f + 0.2f * sin(seconds * 1.3f)
    drawCircle(StarLight, 70f, Offset(x, y), alpha = 0.12f * glow)
    drawCircle(StarLight, 40f, Offset(x, y), alpha = 0.2f * glow)
    // The beam down toward the hill.
    drawPath(Path().apply {
        moveTo(x - 6f, y); lineTo(x + 6f, y); lineTo(x + 1.5f, y + 150f); lineTo(x - 1.5f, y + 150f); close()
    }, StarLight, alpha = 0.45f * glow)
    fun ray(angle: Float, length: Float, width: Float) {
        withTransform({ rotate(angle, pivot = Offset(x, y)) }) {
            drawPath(Path().apply {
                moveTo(x, y - length); lineTo(x + width, y); lineTo(x, y + length); lineTo(x - width, y); close()
            }, StarLight)
        }
    }
    ray(0f, 46f * glow, 9f)
    ray(90f, 34f * glow, 8f)
    ray(45f, 18f, 5f)
    ray(-45f, 18f, 5f)
    drawCircle(StarCore, 7f, Offset(x, y))
}

// A camel walking with a wise man riding between its humps, holding his gift. Feet at the
// pose's x, y, facing the way it walks.
private fun DrawScope.drawCamelRider(pose: WalkerPose, index: Int, seconds: Float) {
    withTransform({
        translate(pose.x, pose.y)
        scale(if (pose.leftToRight) CamelScale else -CamelScale, CamelScale, pivot = Offset.Zero)
    }) {
        val swing = pose.stride * 0.35f
        fun leg(hip: Offset, angle: Float, color: Color) {
            val knee = Offset(hip.x + 22f * sin(angle), hip.y + 22f * cos(angle))
            val foot = Offset(knee.x + 20f * sin(angle * 0.4f), knee.y + 20f)
            drawLine(color, hip, knee, strokeWidth = 7f, cap = StrokeCap.Round)
            drawLine(color, knee, foot, strokeWidth = 6f, cap = StrokeCap.Round)
            drawOval(CamelHoof, Offset(foot.x - 5f, foot.y - 3f), Size(11f, 5f))
        }
        // Far legs, darker, then the body, then the near legs.
        leg(Offset(-30f, -46f), -swing, CamelShade)
        leg(Offset(26f, -46f), swing, CamelShade)
        drawOval(Camel, Offset(-46f, -72f), Size(92f, 34f))
        drawOval(Camel, Offset(-34f, -92f), Size(30f, 32f))
        drawOval(Camel, Offset(2f, -90f), Size(28f, 30f))
        // Neck curving up to the head, a little bob as it walks.
        val bob = 2f * pose.stride
        drawPath(Path().apply {
            moveTo(34f, -66f)
            quadraticTo(60f, -64f, 62f, -92f + bob)
            lineTo(72f, -96f + bob)
            quadraticTo(70f, -54f, 40f, -48f)
            close()
        }, Camel)
        drawOval(Camel, Offset(58f, -106f + bob), Size(28f, 15f))
        drawCircle(CamelHoof, 1.8f, Offset(76f, -101f + bob))
        drawLine(Camel, Offset(-44f, -60f), Offset(-52f, -40f + 4f * pose.stride), strokeWidth = 3f, cap = StrokeCap.Round)
        leg(Offset(-26f, -44f), swing, Camel)
        leg(Offset(30f, -44f), -swing, Camel)
        // Saddle blanket and the rider.
        drawRoundRect(Saddle, Offset(-14f, -90f), Size(26f, 22f), CornerRadius(4f))
        val robe = Robes[index % Robes.size]
        drawPath(Path().apply {
            moveTo(-10f, -88f); lineTo(10f, -88f); lineTo(6f, -128f); lineTo(-6f, -128f); close()
        }, robe)
        drawCircle(Skins[index % Skins.size], 9f, Offset(0f, -136f))
        // A crown.
        drawPath(Path().apply {
            moveTo(-8f, -142f); lineTo(-8f, -152f); lineTo(-4f, -147f); lineTo(0f, -154f); lineTo(4f, -147f); lineTo(8f, -152f); lineTo(8f, -142f); close()
        }, Crown)
        // The gift, held out in front.
        val gift = Offset(14f, -116f + 1.5f * sin(seconds * 2f + index))
        drawLine(robe, Offset(4f, -122f), gift, strokeWidth = 5f, cap = StrokeCap.Round)
        drawRoundRect(Gifts[index % Gifts.size], Offset(gift.x - 2f, gift.y - 10f), Size(14f, 12f), CornerRadius(2f))
        drawRect(StarCore, Offset(gift.x + 4f, gift.y - 10f), Size(2f, 12f))
    }
}
