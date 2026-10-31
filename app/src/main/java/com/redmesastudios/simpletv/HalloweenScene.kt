package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.sin

// Halloween on the fall scene: a harvest moon, bats crossing the sky, and on the hillside
// between the apps and the glance cards a graveyard, a hay bale with a black cat, corn
// stalks, a patch of jack-o'-lanterns with a friendly ghost, and now and then a skeleton or
// a mummy walking by. Drawn in design px (1920x1080).

private val MoonColor = Color(0xFFF3D3A0)
private val MoonShade = Color(0xFFE3BC86)
private val BatColor = Color(0xFF2A211B)
private val PumpkinColors = listOf(Color(0xFFD9772E), Color(0xFFE48A3E), Color(0xFFCC6A28))
private val PumpkinStem = Color(0xFF5E5A2E)
private val PumpkinFace = Color(0xFF3B2A21)
private val CandleLight = Color(0xFFF7C55A)
private val GhostColor = Color(0xFFFFFCF6)
private val GhostShadow = Color(0xFFE6D8C6)

// Behind everything else in the sky.
fun DrawScope.drawHarvestMoon(unit: Float) {
    val center = Offset(960f * unit, 118f * unit)
    drawCircle(MoonColor, 118f * unit, center, alpha = 0.10f)
    drawCircle(MoonColor, 88f * unit, center, alpha = 0.20f)
    drawCircle(MoonColor, 64f * unit, center)
    drawCircle(MoonShade, 13f * unit, Offset(940f * unit, 100f * unit), alpha = 0.55f)
    drawCircle(MoonShade, 8f * unit, Offset(984f * unit, 136f * unit), alpha = 0.5f)
    drawCircle(MoonShade, 6f * unit, Offset(972f * unit, 92f * unit), alpha = 0.45f)
}

// The moving parts, redrawn with the falling leaves.
fun DrawScope.drawHalloweenFigures(unit: Float, seconds: Float) {
    Bats.forEach { bat -> bat.poseAt(seconds)?.let { drawBat(it, unit) } }
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawCornStalks(150f, 860f)
        drawCornStalks(1090f, 852f)
        Tombstones.forEach { drawTombstone(it) }
        drawHayBale(800f, 858f)
        drawCat(812f, 806f, seconds)
    }
    drawGhost(1470f, 690f, seconds, unit)
    Pumpkins.forEachIndexed { i, p -> drawPumpkin(p, candleGlow(seconds, i * 1.9f), unit) }
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        Mummy.poseAt(seconds)?.let { drawMummy(it, seconds) }
        Skeleton.poseAt(seconds)?.let { drawSkeleton(it) }
    }
}

private data class Pumpkin(val x: Float, val baseY: Float, val width: Float, val colorIndex: Int)

private data class Tombstone(val x: Float, val baseY: Float, val width: Float, val height: Float, val tilt: Float)

private val Tombstones = listOf(
    Tombstone(x = 330f, baseY = 850f, width = 64f, height = 84f, tilt = -4f),
    Tombstone(x = 430f, baseY = 856f, width = 52f, height = 66f, tilt = 3f),
    Tombstone(x = 530f, baseY = 852f, width = 60f, height = 92f, tilt = -1f),
)

// On the hillside where the last row of apps leaves room.
private val Pumpkins = listOf(
    Pumpkin(x = 1270f, baseY = 830f, width = 96f, colorIndex = 0),
    Pumpkin(x = 1372f, baseY = 838f, width = 70f, colorIndex = 1),
    Pumpkin(x = 1590f, baseY = 834f, width = 120f, colorIndex = 2),
    Pumpkin(x = 1712f, baseY = 840f, width = 66f, colorIndex = 0),
)

private val WingShape = Path().apply {
    // One wing, root at the origin, reaching right; scalloped trailing edge.
    moveTo(0f, -4f)
    cubicTo(10f, -14f, 26f, -16f, 40f, -6f)
    quadraticTo(34f, 0f, 30f, 6f)
    quadraticTo(25f, 1f, 20f, 7f)
    quadraticTo(15f, 1f, 9f, 6f)
    quadraticTo(5f, 2f, 0f, 4f)
    close()
}

private fun DrawScope.drawBat(pose: BatPose, unit: Float) {
    val scale = pose.size / 80f * unit
    withTransform({
        translate(pose.x * unit, pose.y * unit)
        scale(if (pose.leftToRight) scale else -scale, scale, pivot = Offset.Zero)
    }) {
        val wing = 0.25f + 0.75f * pose.flap
        withTransform({ scale(1f, wing, pivot = Offset.Zero) }) { drawPath(WingShape, BatColor, alpha = 0.78f) }
        withTransform({ scale(-1f, wing, pivot = Offset.Zero) }) { drawPath(WingShape, BatColor, alpha = 0.78f) }
        drawOval(BatColor, Offset(-6f, -8f), Size(12f, 18f), alpha = 0.85f)
        // Ears.
        drawPath(Path().apply {
            moveTo(-5f, -6f); lineTo(-4f, -14f); lineTo(-1f, -7f)
            moveTo(5f, -6f); lineTo(4f, -14f); lineTo(1f, -7f)
        }, BatColor, alpha = 0.85f)
    }
}

private fun DrawScope.drawGhost(x: Float, y: Float, seconds: Float, unit: Float) {
    val bob = 14f * sin(seconds * 1.4f)
    val tilt = 5f * sin(seconds * 0.9f)
    withTransform({
        translate(x * unit, (y + bob) * unit)
        rotate(tilt, pivot = Offset.Zero)
        scale(unit, unit, pivot = Offset.Zero)
    }) {
        val body = Path().apply {
            moveTo(-38f, 30f)
            cubicTo(-40f, -52f, 40f, -52f, 38f, 30f)
            // A wavy hem that drifts.
            val wave = 5f * sin(seconds * 3f)
            quadraticTo(30f, 46f + wave, 19f, 32f)
            quadraticTo(10f, 46f - wave, 0f, 32f)
            quadraticTo(-10f, 46f + wave, -19f, 32f)
            quadraticTo(-30f, 46f - wave, -38f, 30f)
            close()
        }
        drawOval(GhostShadow, Offset(-28f, 70f - bob), Size(56f, 10f), alpha = 0.35f)
        drawPath(body, GhostColor, alpha = 0.92f)
        drawOval(PumpkinFace, Offset(-16f, -14f), Size(9f, 13f))
        drawOval(PumpkinFace, Offset(7f, -14f), Size(9f, 13f))
        drawOval(PumpkinFace, Offset(-5f, 4f), Size(10f, 11f), alpha = 0.85f)
    }
}

private fun DrawScope.drawPumpkin(p: Pumpkin, glow: Float, unit: Float) {
    val w = p.width
    val h = w * 0.78f
    val left = p.x - w / 2
    val top = p.baseY - h
    val color = PumpkinColors[p.colorIndex % PumpkinColors.size]
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        // Soft candlelight on the ground around it.
        drawOval(CandleLight, Offset(p.x - w * 0.9f, p.baseY - h * 0.25f), Size(w * 1.8f, h * 0.5f), alpha = 0.18f * glow)
        // Ribs: side lobes, then the middle one on top.
        drawOval(color, Offset(left, top + h * 0.06f), Size(w * 0.56f, h * 0.94f))
        drawOval(color, Offset(left + w * 0.44f, top + h * 0.06f), Size(w * 0.56f, h * 0.94f))
        drawOval(PumpkinColors[(p.colorIndex + 1) % PumpkinColors.size], Offset(left + w * 0.24f, top), Size(w * 0.52f, h))
        drawRoundRect(PumpkinStem, Offset(p.x - w * 0.04f, top - h * 0.16f), Size(w * 0.08f, h * 0.22f), CornerRadius(w * 0.03f))
        // The carved face, lit from inside.
        val face = Path().apply {
            // Eyes.
            moveTo(p.x - w * 0.26f, top + h * 0.46f); lineTo(p.x - w * 0.17f, top + h * 0.28f); lineTo(p.x - w * 0.08f, top + h * 0.46f); close()
            moveTo(p.x + w * 0.08f, top + h * 0.46f); lineTo(p.x + w * 0.17f, top + h * 0.28f); lineTo(p.x + w * 0.26f, top + h * 0.46f); close()
            // Toothy grin.
            moveTo(p.x - w * 0.30f, top + h * 0.58f)
            quadraticTo(p.x, top + h * 0.92f, p.x + w * 0.30f, top + h * 0.58f)
            lineTo(p.x + w * 0.18f, top + h * 0.64f)
            lineTo(p.x + w * 0.12f, top + h * 0.58f)
            lineTo(p.x + w * 0.04f, top + h * 0.66f)
            lineTo(p.x - w * 0.04f, top + h * 0.58f)
            lineTo(p.x - w * 0.12f, top + h * 0.66f)
            lineTo(p.x - w * 0.18f, top + h * 0.58f)
            close()
        }
        // A dark rim around each cut keeps the face readable against the orange.
        drawPath(face, PumpkinFace, style = Stroke(width = w * 0.05f, join = StrokeJoin.Round))
        drawPath(face, CandleLight, alpha = glow)
    }
}

private const val WalkerScale = 1.35f
private val Bone = Color(0xFFF6F0E6)
private val BoneEdge = Color(0xFF3B2A21)
private val Bandage = Color(0xFFE7DCC4)
private val BandageShade = Color(0xFFC9BB9C)
private val Stone = Color(0xFFB9AFA4)
private val StoneShade = Color(0xFF8F857A)
private val Hay = Color(0xFFD9B567)
private val HayShade = Color(0xFFB8923F)
private val CatColor = Color(0xFF231B16)
private val CatEyes = Color(0xFFE8C24A)
private val Stalk = Color(0xFFC9A86A)
private val StalkShade = Color(0xFF9E7F48)

// A bone, as a light line with a dark edge so it reads against the pale hill.
private fun DrawScope.bone(from: Offset, to: Offset, width: Float = 4.5f) {
    drawLine(BoneEdge, from, to, strokeWidth = width + 2.5f, cap = StrokeCap.Round, alpha = 0.55f)
    drawLine(Bone, from, to, strokeWidth = width, cap = StrokeCap.Round)
}

private fun limb(from: Offset, length: Float, angle: Float) =
    Offset(from.x + length * kotlin.math.sin(angle), from.y + length * kotlin.math.cos(angle))

// Feet at the pose's x, y; faces the way it walks.
private fun DrawScope.drawSkeleton(pose: WalkerPose) {
    withTransform({
        translate(pose.x, pose.y)
        scale(if (pose.leftToRight) WalkerScale else -WalkerScale, WalkerScale, pivot = Offset.Zero)
    }) {
        val hip = Offset(0f, -58f)
        val neck = Offset(2f, -98f)
        val shoulder = Offset(2f, -92f)
        val legSwing = pose.stride * 0.42f
        val armSwing = -pose.stride * 0.5f
        // Back limbs first, front limbs over the body.
        bone(shoulder, limb(shoulder, 38f, -armSwing), 4f)
        bone(hip, limb(hip, 58f, -legSwing))
        bone(hip, neck, 5f)
        listOf(-88f, -80f, -72f).forEach { y ->
            drawArc(BoneEdge, 200f, 140f, false, Offset(-10f, y - 4f), androidx.compose.ui.geometry.Size(24f, 12f),
                style = Stroke(5.5f, cap = StrokeCap.Round), alpha = 0.55f)
            drawArc(Bone, 200f, 140f, false, Offset(-10f, y - 4f), androidx.compose.ui.geometry.Size(24f, 12f),
                style = Stroke(3.5f, cap = StrokeCap.Round))
        }
        drawOval(BoneEdge, Offset(-11f, -64f), androidx.compose.ui.geometry.Size(22f, 12f), alpha = 0.55f)
        drawOval(Bone, Offset(-9.5f, -62.5f), androidx.compose.ui.geometry.Size(19f, 9f))
        val frontFoot = limb(hip, 58f, legSwing)
        bone(hip, frontFoot)
        bone(frontFoot, Offset(frontFoot.x + 9f, frontFoot.y), 4f)
        bone(shoulder, limb(shoulder, 38f, armSwing), 4f)
        // Skull, with a jaw and dark eyes.
        drawCircle(BoneEdge, 13f, Offset(4f, -110f), alpha = 0.55f)
        drawCircle(Bone, 11.5f, Offset(4f, -110f))
        drawRoundRect(Bone, Offset(0f, -102f), androidx.compose.ui.geometry.Size(12f, 8f), CornerRadius(3f))
        drawCircle(BoneEdge, 3f, Offset(8f, -112f))
        drawCircle(BoneEdge, 2.6f, Offset(1f, -112f))
    }
}

// Shuffles slowly with its arms out in front, a loose bandage trailing behind.
private fun DrawScope.drawMummy(pose: WalkerPose, seconds: Float) {
    withTransform({
        translate(pose.x, pose.y)
        scale(if (pose.leftToRight) WalkerScale else -WalkerScale, WalkerScale, pivot = Offset.Zero)
    }) {
        val shuffle = pose.stride * 6f
        drawRoundRect(BandageShade, Offset(-12f - shuffle, -40f), androidx.compose.ui.geometry.Size(11f, 40f), CornerRadius(4f))
        drawRoundRect(Bandage, Offset(1f + shuffle, -40f), androidx.compose.ui.geometry.Size(11f, 40f), CornerRadius(4f))
        // A loose end of bandage waving behind.
        val wave = 6f * kotlin.math.sin(seconds * 3f)
        drawPath(Path().apply {
            moveTo(-14f, -80f)
            quadraticTo(-34f, -74f + wave, -46f, -66f - wave)
            lineTo(-44f, -61f - wave)
            quadraticTo(-32f, -68f + wave, -14f, -73f)
            close()
        }, Bandage)
        drawRoundRect(Bandage, Offset(-15f, -96f), androidx.compose.ui.geometry.Size(30f, 60f), CornerRadius(10f))
        listOf(-90f, -80f, -70f, -60f, -50f).forEachIndexed { i, y ->
            drawLine(BandageShade, Offset(-15f, y + (i % 2) * 3f), Offset(15f, y - 4f + (i % 2) * 3f), strokeWidth = 2f)
        }
        // Arms straight out, bobbing as it walks.
        val reach = 2f * pose.stride
        drawRoundRect(BandageShade, Offset(4f, -90f + reach), androidx.compose.ui.geometry.Size(36f, 9f), CornerRadius(4f))
        drawRoundRect(Bandage, Offset(6f, -84f - reach), androidx.compose.ui.geometry.Size(36f, 9f), CornerRadius(4f))
        drawCircle(Bandage, 13f, Offset(2f, -108f))
        drawLine(BandageShade, Offset(-10f, -114f), Offset(14f, -110f), strokeWidth = 2f)
        drawLine(BandageShade, Offset(-11f, -103f), Offset(13f, -99f), strokeWidth = 2f)
        drawOval(PumpkinFace, Offset(1f, -111f), androidx.compose.ui.geometry.Size(10f, 5f))
        drawCircle(CandleLight, 1.6f, Offset(4f, -108.5f))
        drawCircle(CandleLight, 1.6f, Offset(8.5f, -108.5f))
    }
}

private fun DrawScope.drawTombstone(t: Tombstone) {
    withTransform({ rotate(t.tilt, pivot = Offset(t.x, t.baseY)) }) {
        val left = t.x - t.width / 2
        val top = t.baseY - t.height
        val shape = Path().apply {
            moveTo(left, t.baseY)
            lineTo(left, top + t.width / 2)
            arcTo(androidx.compose.ui.geometry.Rect(left, top, left + t.width, top + t.width), 180f, 180f, false)
            lineTo(left + t.width, t.baseY)
            close()
        }
        drawPath(shape, Stone)
        drawPath(shape, StoneShade, style = Stroke(2.5f))
        // A carved cross and a crack.
        drawLine(StoneShade, Offset(t.x, top + t.height * 0.28f), Offset(t.x, top + t.height * 0.62f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(StoneShade, Offset(t.x - t.width * 0.18f, top + t.height * 0.38f), Offset(t.x + t.width * 0.18f, top + t.height * 0.38f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawPath(Path().apply {
            moveTo(left + t.width * 0.78f, top + t.height * 0.7f)
            lineTo(left + t.width * 0.66f, top + t.height * 0.8f)
            lineTo(left + t.width * 0.74f, top + t.height * 0.9f)
        }, StoneShade, style = Stroke(1.6f))
    }
}

private fun DrawScope.drawHayBale(x: Float, baseY: Float) {
    val w = 120f
    val h = 54f
    drawRoundRect(Hay, Offset(x - w / 2, baseY - h), androidx.compose.ui.geometry.Size(w, h), CornerRadius(10f))
    repeat(7) { i ->
        val y = baseY - h + 8f + i * 6.5f
        drawLine(HayShade, Offset(x - w / 2 + 6f + (i % 3) * 5f, y), Offset(x + w / 2 - 8f - (i % 2) * 7f, y + 1.5f), strokeWidth = 1.6f, alpha = 0.7f)
    }
    drawLine(HayShade, Offset(x - w / 4, baseY - h), Offset(x - w / 4, baseY), strokeWidth = 3f)
    drawLine(HayShade, Offset(x + w / 4, baseY - h), Offset(x + w / 4, baseY), strokeWidth = 3f)
}

// Sitting on the hay bale, tail swishing.
private fun DrawScope.drawCat(x: Float, baseY: Float, seconds: Float) {
    val swish = 10f * kotlin.math.sin(seconds * 1.7f)
    drawPath(Path().apply {
        moveTo(x + 14f, baseY - 6f)
        cubicTo(x + 40f, baseY + 2f, x + 46f, baseY - 26f, x + 38f + swish, baseY - 40f)
    }, CatColor, style = Stroke(5f, cap = StrokeCap.Round))
    drawOval(CatColor, Offset(x - 18f, baseY - 40f), androidx.compose.ui.geometry.Size(36f, 42f))
    drawCircle(CatColor, 14f, Offset(x - 2f, baseY - 48f))
    drawPath(Path().apply {
        moveTo(x - 14f, baseY - 54f); lineTo(x - 13f, baseY - 70f); lineTo(x - 4f, baseY - 60f); close()
        moveTo(x + 10f, baseY - 54f); lineTo(x + 9f, baseY - 70f); lineTo(x, baseY - 60f); close()
    }, CatColor)
    drawOval(CatEyes, Offset(x - 9f, baseY - 52f), androidx.compose.ui.geometry.Size(5f, 4f))
    drawOval(CatEyes, Offset(x + 1f, baseY - 52f), androidx.compose.ui.geometry.Size(5f, 4f))
}

// A tied bundle of dried corn stalks.
private fun DrawScope.drawCornStalks(x: Float, baseY: Float) {
    val tops = listOf(-30f to -150f, -14f to -170f, 0f to -178f, 14f to -166f, 30f to -148f)
    tops.forEachIndexed { i, (dx, dy) ->
        drawLine(if (i % 2 == 0) Stalk else StalkShade, Offset(x + dx * 0.3f, baseY), Offset(x + dx, baseY + dy), strokeWidth = 5f, cap = StrokeCap.Round)
        drawPath(Path().apply {
            moveTo(x + dx * 0.6f, baseY + dy * 0.55f)
            quadraticTo(x + dx * 0.6f + (if (dx < 0) -26f else 26f), baseY + dy * 0.62f, x + dx * 0.6f + (if (dx < 0) -34f else 34f), baseY + dy * 0.45f)
        }, Stalk, style = Stroke(3f, cap = StrokeCap.Round))
    }
    drawLine(StalkShade, Offset(x - 12f, baseY - 52f), Offset(x + 12f, baseY - 52f), strokeWidth = 5f, cap = StrokeCap.Round)
}
