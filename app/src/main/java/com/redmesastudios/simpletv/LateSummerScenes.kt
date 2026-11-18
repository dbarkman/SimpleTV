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
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.sin

// The late-summer scenes, each on its own backdrop (SceneChoice.kt):
// Emoji Day (July 17, among chat bubbles): five emoji faces bouncing, hearts floating up.
// Ice Cream Day (third Sunday of July, in the parlor): a sundae, cones in a stand with a
//   drip, a cone on its own and a jar of sprinkles.
// S'mores Day (August 10, at the campsite): a campfire with sparks, a marshmallow toasting
//   on a stick, a s'more on a plate and a lantern.
// Dog Day (August 26, at the dog park): a dog chasing a bouncing ball across, a doghouse, a
//   water bowl and a bone.
// Labor Day weekend (at the building site; "Happy Labor Day!" on the Monday): hard hats on
//   a sawhorse, a toolbox, a lunch pail and thermos, and small flags.
// Grandparents Day weekend (in the living room; greets on the Sunday): a rocking chair
//   rocking, a basket of yarn, cookies and tea.
// Drawn in design px (1920x1080).

// ---- Emoji Day ----

private val Face = Color(0xFFF7CF4E)
private val FaceShade = Color(0xFFE9B93A)
private val Feature = Color(0xFF5A3E28)
private val HeartRed = Color(0xFFE8517A)
private val Tear = Color(0xFF7FB7D9)

fun DrawScope.drawEmojiDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        repeat(6) { i ->
            val p = ((seconds * 0.12f + i / 6f) % 1f)
            drawHeart(Offset(620f + i * 140f + 20f * sin(seconds + i), 860f - 220f * p), 18f, HeartRed, alpha = (1f - p) * 0.8f)
        }
        listOf(560f, 760f, 960f, 1160f, 1360f).forEachIndexed { i, x ->
            val bounce = abs(sin(seconds * 2.2f + i * 0.9f))
            drawEmoji(Offset(x, 818f - 46f * bounce), i)
            drawOval(Color(0xFF6E6A86), Offset(x - 34f + 8f * bounce, 862f), Size(68f - 16f * bounce, 8f), alpha = 0.18f)
        }
    }
}

private fun DrawScope.drawEmoji(c: Offset, kind: Int) {
    val r = 44f
    drawCircle(FaceShade, r, Offset(c.x + 2f, c.y + 3f))
    drawCircle(Face, r, c)
    when (kind) {
        // Smiling.
        0 -> {
            drawCircle(Feature, 5f, Offset(c.x - 15f, c.y - 10f)); drawCircle(Feature, 5f, Offset(c.x + 15f, c.y - 10f))
            drawArc(Feature, 20f, 140f, useCenter = false, topLeft = Offset(c.x - 22f, c.y - 10f), size = Size(44f, 34f), style = Stroke(4f, cap = StrokeCap.Round))
        }
        // Heart eyes.
        1 -> {
            drawHeart(Offset(c.x - 16f, c.y - 8f), 20f, HeartRed); drawHeart(Offset(c.x + 16f, c.y - 8f), 20f, HeartRed)
            drawArc(Feature, 0f, 180f, useCenter = true, topLeft = Offset(c.x - 18f, c.y + 4f), size = Size(36f, 26f))
        }
        // Laughing, with tears.
        2 -> {
            drawArc(Feature, 200f, 140f, useCenter = false, topLeft = Offset(c.x - 24f, c.y - 18f), size = Size(18f, 14f), style = Stroke(4f, cap = StrokeCap.Round))
            drawArc(Feature, 200f, 140f, useCenter = false, topLeft = Offset(c.x + 6f, c.y - 18f), size = Size(18f, 14f), style = Stroke(4f, cap = StrokeCap.Round))
            drawArc(Feature, 0f, 180f, useCenter = true, topLeft = Offset(c.x - 22f, c.y), size = Size(44f, 30f))
            drawOval(Tear, Offset(c.x - 44f, c.y - 6f), Size(12f, 20f)); drawOval(Tear, Offset(c.x + 32f, c.y - 6f), Size(12f, 20f))
        }
        // Winking.
        3 -> {
            drawCircle(Feature, 5f, Offset(c.x - 15f, c.y - 10f))
            drawLine(Feature, Offset(c.x + 8f, c.y - 10f), Offset(c.x + 24f, c.y - 10f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawArc(Feature, 20f, 140f, useCenter = false, topLeft = Offset(c.x - 20f, c.y - 6f), size = Size(40f, 28f), style = Stroke(4f, cap = StrokeCap.Round))
        }
        // Sunglasses.
        else -> {
            drawRoundRect(Feature, Offset(c.x - 34f, c.y - 20f), Size(30f, 18f), CornerRadius(6f))
            drawRoundRect(Feature, Offset(c.x + 4f, c.y - 20f), Size(30f, 18f), CornerRadius(6f))
            drawLine(Feature, Offset(c.x - 6f, c.y - 14f), Offset(c.x + 6f, c.y - 14f), strokeWidth = 3f)
            drawArc(Feature, 20f, 140f, useCenter = false, topLeft = Offset(c.x - 18f, c.y - 2f), size = Size(36f, 26f), style = Stroke(4f, cap = StrokeCap.Round))
        }
    }
}

// ---- Ice Cream Day ----

private val Scoops = listOf(Color(0xFFF7C6CF), Color(0xFF8C5A3A), Color(0xFFFBF2EA), Color(0xFFB8E0C8), Color(0xFFF7D27A))
private val Cone = Color(0xFFE2A85E)
private val ConeLine = Color(0xFFC98A44)
private val Glass = Color(0xFFDDEBF0)
private val Whip = Color(0xFFFDFCF8)
private val Cherry = Color(0xFFC0303A)
private val Fudge = Color(0xFF6E4630)
private val SprinkleColors = listOf(Color(0xFFE8517A), Color(0xFF5FA8D3), Color(0xFFF2D24E), Color(0xFF6DBE7B))

fun DrawScope.drawIceCreamDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawConeStand(700f, 866f, seconds)
        drawSundae(960f, 866f, seconds)
        drawCone(Offset(1210f, 866f), 0f, listOf(3, 4), seconds, 0.6f)
        drawSprinkleJar(1390f, 866f)
    }
}

private fun DrawScope.drawCone(tipBase: Offset, tilt: Float, scoops: List<Int>, seconds: Float, phase: Float) {
    withTransform({ rotate(tilt, pivot = tipBase) }) {
        val top = tipBase.y - 90f
        drawPath(Path().apply { moveTo(tipBase.x - 30f, top); lineTo(tipBase.x + 30f, top); lineTo(tipBase.x, tipBase.y); close() }, Cone)
        listOf(-14f, 0f, 14f).forEach { dx -> drawLine(ConeLine, Offset(tipBase.x + dx - 12f, top + 4f), Offset(tipBase.x + dx + 10f, top + 50f), strokeWidth = 1.5f) }
        scoops.forEachIndexed { i, s ->
            val c = Offset(tipBase.x, top - 18f - i * 34f)
            drawCircle(Scoops[s % Scoops.size], 30f, c)
            drawOval(Scoops[s % Scoops.size], Offset(c.x - 34f, c.y + 8f), Size(68f, 18f))
        }
        // A drip running down from the lowest scoop.
        val drip = 10f + 8f * (sin(seconds * 0.8f + phase) * 0.5f + 0.5f)
        drawRoundRect(Scoops[scoops.first() % Scoops.size], Offset(tipBase.x + 18f, top), Size(8f, drip), CornerRadius(4f))
    }
}

private fun DrawScope.drawConeStand(x: Float, baseY: Float, seconds: Float) {
    drawRect(Color(0xFFB98A5E), Offset(x - 110f, baseY - 40f), Size(220f, 40f))
    listOf(-70f, 0f, 70f).forEachIndexed { i, dx ->
        drawCone(Offset(x + dx, baseY - 4f), 0f, listOf(i, (i + 2) % 5), seconds, i.toFloat())
    }
    drawRect(Color(0xFF9C7A55), Offset(x - 110f, baseY - 44f), Size(220f, 8f))
}

private fun DrawScope.drawSundae(x: Float, baseY: Float, seconds: Float) {
    // A tall glass on a stem, three scoops, fudge, whipped cream and a cherry.
    drawRect(Glass, Offset(x - 8f, baseY - 50f), Size(16f, 44f))
    drawOval(Glass, Offset(x - 40f, baseY - 12f), Size(80f, 14f))
    drawPath(Path().apply { moveTo(x - 60f, baseY - 140f); lineTo(x + 60f, baseY - 140f); lineTo(x + 20f, baseY - 50f); lineTo(x - 20f, baseY - 50f); close() }, Glass, alpha = 0.9f)
    listOf(-34f to 0, 34f to 1, 0f to 2).forEachIndexed { i, (dx, s) ->
        drawCircle(Scoops[s], 34f, Offset(x + dx, baseY - 150f - (if (i == 2) 34f else 0f)))
    }
    drawPath(Path().apply { moveTo(x - 66f, baseY - 150f); quadraticTo(x - 40f, baseY - 120f, x - 30f, baseY - 156f); quadraticTo(x, baseY - 120f, x + 20f, baseY - 160f); quadraticTo(x + 50f, baseY - 120f, x + 66f, baseY - 150f) }, Fudge, style = Stroke(10f, cap = StrokeCap.Round))
    repeat(3) { k -> drawCircle(Whip, 22f - k * 5f, Offset(x, baseY - 206f - k * 16f)) }
    val bob = 2f * sin(seconds * 2f)
    drawLine(Color(0xFF6E9C6D), Offset(x, baseY - 262f + bob), Offset(x + 10f, baseY - 288f + bob), strokeWidth = 2.5f)
    drawCircle(Cherry, 12f, Offset(x, baseY - 252f + bob))
    drawCircle(Color.White, 3f, Offset(x - 4f, baseY - 256f + bob), alpha = 0.6f)
}

private fun DrawScope.drawSprinkleJar(x: Float, baseY: Float) {
    drawRoundRect(Glass, Offset(x - 34f, baseY - 90f), Size(68f, 90f), CornerRadius(12f), alpha = 0.9f)
    repeat(40) { i ->
        val sx = x - 26f + (i * 13 % 52)
        val sy = baseY - 10f - (i * 7 % 60)
        drawLine(SprinkleColors[i % SprinkleColors.size], Offset(sx, sy), Offset(sx + 4f, sy + 2f), strokeWidth = 3f, cap = StrokeCap.Round)
    }
    drawRoundRect(Color(0xFFF4A6B8), Offset(x - 38f, baseY - 104f), Size(76f, 18f), CornerRadius(6f))
}

// ---- S'mores Day ----

private val Log = Color(0xFF7A5A40)
private val LogEnd = Color(0xFFC9A57E)
private val Ember = Color(0xFFE8793A)
private val FlameCore = Color(0xFFF7D27A)
private val Stick = Color(0xFF8C6A4F)
private val Graham = Color(0xFFD9A05E)
private val Chocolate = Color(0xFF6E4630)
private val Marshmallow = Color(0xFFFDFCF8)

fun DrawScope.drawSmoresDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawLantern(680f, 866f, seconds)
        drawCampfire(960f, 866f, seconds)
        // A marshmallow on a stick held over the fire, browning and starting over.
        val toast = (seconds % 12f) / 12f
        drawLine(Stick, Offset(760f, 790f), Offset(930f, 760f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawRoundRect(lerp(Marshmallow, Color(0xFFD9A05E), toast), Offset(918f, 742f), Size(30f, 30f), CornerRadius(10f))
        drawSmore(1200f, 866f)
    }
}

private fun DrawScope.drawCampfire(x: Float, baseY: Float, seconds: Float) {
    drawCircle(Ember, 120f, Offset(x, baseY - 30f), alpha = 0.12f * candleGlow(seconds, 0.3f))
    listOf(-1f, 1f).forEach { side ->
        withTransform({ rotate(side * 18f, pivot = Offset(x, baseY - 10f)) }) {
            drawRoundRect(Log, Offset(x - 80f, baseY - 22f), Size(160f, 22f), CornerRadius(11f))
            drawCircle(LogEnd, 11f, Offset(x + side * 80f, baseY - 11f))
        }
    }
    listOf(-24f to 0f, 0f to 1.7f, 22f to 3.1f).forEach { (dx, phase) ->
        val h = 70f + 18f * sin(seconds * 6.5f + phase)
        drawPath(Path().apply { moveTo(x + dx - 20f, baseY - 16f); quadraticTo(x + dx - 18f, baseY - 16f - h * 0.6f, x + dx + 3f * sin(seconds * 5f + phase), baseY - 16f - h); quadraticTo(x + dx + 18f, baseY - 16f - h * 0.6f, x + dx + 20f, baseY - 16f); close() }, Ember, alpha = 0.92f)
        drawPath(Path().apply { moveTo(x + dx - 9f, baseY - 16f); quadraticTo(x + dx - 7f, baseY - 16f - h * 0.35f, x + dx, baseY - 16f - h * 0.55f); quadraticTo(x + dx + 7f, baseY - 16f - h * 0.35f, x + dx + 9f, baseY - 16f); close() }, FlameCore)
    }
    repeat(5) { i ->
        val p = (seconds * 0.6f + i / 5f) % 1f
        drawCircle(FlameCore, 3f, Offset(x - 20f + i * 10f + 16f * sin(seconds * 2f + i), baseY - 90f - 130f * p), alpha = 1f - p)
    }
}

private fun DrawScope.drawSmore(x: Float, baseY: Float) {
    drawOval(Color(0xFFDCD6CC), Offset(x - 70f, baseY - 12f), Size(140f, 16f))
    drawRoundRect(Graham, Offset(x - 46f, baseY - 26f), Size(92f, 16f), CornerRadius(3f))
    drawRoundRect(Chocolate, Offset(x - 40f, baseY - 36f), Size(80f, 10f), CornerRadius(2f))
    drawRoundRect(Marshmallow, Offset(x - 42f, baseY - 64f), Size(84f, 30f), CornerRadius(12f))
    drawRoundRect(Color(0xFFE6C9A0), Offset(x - 42f, baseY - 64f), Size(84f, 10f), CornerRadius(6f))
    drawRoundRect(Graham, Offset(x - 46f, baseY - 80f), Size(92f, 16f), CornerRadius(3f))
    repeat(4) { i -> drawCircle(Color(0xFFC98A44), 2f, Offset(x - 30f + i * 20f, baseY - 72f)) }
}

private fun DrawScope.drawLantern(x: Float, baseY: Float, seconds: Float) {
    val glow = candleGlow(seconds, 2f)
    drawCircle(FlameCore, 50f, Offset(x, baseY - 50f), alpha = 0.15f * glow)
    drawRoundRect(Color(0xFF4A4440), Offset(x - 26f, baseY - 14f), Size(52f, 14f), CornerRadius(4f))
    drawRoundRect(Color(0xFFF7E3A6), Offset(x - 22f, baseY - 80f), Size(44f, 66f), CornerRadius(6f), alpha = 0.6f + 0.4f * glow)
    drawRoundRect(Color(0xFF4A4440), Offset(x - 26f, baseY - 92f), Size(52f, 14f), CornerRadius(4f))
    drawArc(Color(0xFF4A4440), 180f, 180f, useCenter = false, topLeft = Offset(x - 16f, baseY - 112f), size = Size(32f, 30f), style = Stroke(3f))
}

// ---- Dog Day ----

private val DogFur = Color(0xFFC98E5A)
private val DogDark = Color(0xFF8C5A3A)
private val Ball = Color(0xFFD7E04E)
private val Doghouse = Color(0xFFC0503A)
private val DoghouseRoof = Color(0xFF6E5440)

// The dog runs across after its ball every 40 seconds or so, right to left.
val Dog = Walker(cycleSeconds = 40f, walkSeconds = 14f, offsetSeconds = 6f, baseY = 878f, leftToRight = false, strideSeconds = 0.45f)

fun DrawScope.drawDogDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawDoghouse(560f, 866f)
        drawOval(Color(0xFF5FA8D3), Offset(700f, 852f), Size(70f, 18f))
        drawOval(Color(0xFF8FC3E6), Offset(706f, 852f), Size(58f, 10f))
        drawBone(1280f, 856f)
        Dog.poseAt(seconds)?.let { pose ->
            val dir = if (pose.leftToRight) 1f else -1f
            val ballX = pose.x + dir * 120f
            val ballHop = abs(sin(seconds * 6f))
            drawCircle(Ball, 14f, Offset(ballX, 850f - 50f * ballHop))
            drawDog(pose)
        }
    }
}

private fun DrawScope.drawDog(pose: WalkerPose) {
    withTransform({ translate(pose.x, pose.y); scale(if (pose.leftToRight) 1.2f else -1.2f, 1.2f, pivot = Offset.Zero) }) {
        val run = pose.stride
        listOf(-24f to run, 22f to -run).forEach { (hx, swing) ->
            drawLine(DogDark, Offset(hx, -30f), Offset(hx + 14f * swing, -2f), strokeWidth = 7f, cap = StrokeCap.Round)
            drawLine(DogFur, Offset(hx + 4f, -30f), Offset(hx + 4f - 14f * swing, -2f), strokeWidth = 7f, cap = StrokeCap.Round)
        }
        drawOval(DogFur, Offset(-38f, -54f), Size(76f, 32f))
        drawLine(DogFur, Offset(-36f, -44f), Offset(-56f, -60f + 6f * run), strokeWidth = 6f, cap = StrokeCap.Round)
        drawCircle(DogFur, 18f, Offset(40f, -62f))
        drawOval(DogFur, Offset(46f, -64f), Size(26f, 16f))
        drawCircle(Color(0xFF2A211B), 3.5f, Offset(70f, -58f))
        drawCircle(Color(0xFF2A211B), 2.5f, Offset(44f, -68f))
        drawOval(DogDark, Offset(26f, -76f + 4f * run), Size(14f, 26f))
        drawRoundRect(Color(0xFFE8517A), Offset(56f, -50f), Size(8f, 10f), CornerRadius(4f))
    }
}

private fun DrawScope.drawDoghouse(x: Float, baseY: Float) {
    drawRect(Doghouse, Offset(x - 80f, baseY - 100f), Size(160f, 100f))
    drawPath(Path().apply { moveTo(x - 100f, baseY - 96f); lineTo(x, baseY - 170f); lineTo(x + 100f, baseY - 96f); close() }, DoghouseRoof)
    drawRoundRect(Color(0xFF3A2620), Offset(x - 32f, baseY - 70f), Size(64f, 70f), CornerRadius(32f, 32f))
}

private fun DrawScope.drawBone(x: Float, y: Float) {
    drawRoundRect(Color(0xFFFBF6EC), Offset(x - 30f, y - 6f), Size(60f, 12f), CornerRadius(6f))
    listOf(-30f, 30f).forEach { dx -> drawCircle(Color(0xFFFBF6EC), 9f, Offset(x + dx, y - 6f)); drawCircle(Color(0xFFFBF6EC), 9f, Offset(x + dx, y + 6f)) }
}

// ---- Labor Day ----

private val HardHat = Color(0xFFF2C14E)
private val Sawhorse = Color(0xFFB98A5E)
private val Toolbox = Color(0xFFC9483F)
private val Metal = Color(0xFF8C9AA6)

fun DrawScope.drawLaborDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        // A sawhorse with two hard hats on it.
        drawRect(Sawhorse, Offset(660f, 800f), Size(200f, 14f))
        listOf(680f, 840f).forEach { x -> drawLine(Sawhorse, Offset(x, 812f), Offset(x - 16f, 866f), strokeWidth = 8f); drawLine(Sawhorse, Offset(x, 812f), Offset(x + 16f, 866f), strokeWidth = 8f) }
        listOf(720f, 800f).forEach { x ->
            drawArc(HardHat, 180f, 180f, useCenter = true, topLeft = Offset(x - 34f, 766f), size = Size(68f, 64f))
            drawRoundRect(HardHat, Offset(x - 44f, 792f), Size(88f, 10f), CornerRadius(5f))
            drawRect(Color(0xFFE2A93B), Offset(x - 4f, 768f), Size(8f, 26f))
        }
        // Toolbox with a hammer and wrench sticking out.
        drawLine(Metal, Offset(990f, 800f), Offset(1030f, 760f), strokeWidth = 7f, cap = StrokeCap.Round)
        drawLine(Color(0xFF8C6A4F), Offset(930f, 800f), Offset(920f, 750f), strokeWidth = 7f, cap = StrokeCap.Round)
        drawRect(Metal, Offset(906f, 742f), Size(30f, 14f))
        drawRoundRect(Toolbox, Offset(890f, 800f), Size(160f, 66f), CornerRadius(6f))
        drawRect(Color(0xFFA03E2C), Offset(890f, 820f), Size(160f, 6f))
        drawRoundRect(Metal, Offset(950f, 786f), Size(40f, 14f), CornerRadius(4f), style = Stroke(4f))
        // A lunch pail and thermos.
        drawRoundRect(Color(0xFF5E8C61), Offset(1110f, 810f), Size(110f, 56f), CornerRadius(8f))
        drawArc(Color(0xFF4A7350), 180f, 180f, useCenter = true, topLeft = Offset(1110f, 790f), size = Size(110f, 40f))
        drawRoundRect(Color(0xFF3A5A92), Offset(1240f, 790f), Size(36f, 76f), CornerRadius(10f))
        drawRoundRect(Metal, Offset(1236f, 780f), Size(44f, 16f), CornerRadius(6f))
        listOf(1340f, 1380f).forEachIndexed { i, x -> drawHandFlag(x, 870f, seconds, i * 1.3f) }
    }
}

// ---- Grandparents Day ----

private val Chair = Color(0xFF9C6E4A)
private val ChairDark = Color(0xFF7A5438)
private val Cushion = Color(0xFFC98E7A)
private val YarnColors = listOf(Color(0xFFC0503A), Color(0xFF5FA8D3), Color(0xFFF2C14E))

fun DrawScope.drawGrandparentsDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        val rock = 6f * sin(seconds * 1.1f)
        withTransform({ rotate(rock, pivot = Offset(760f, 866f)) }) {
            drawArc(ChairDark, 20f, 140f, useCenter = false, topLeft = Offset(670f, 820f), size = Size(180f, 60f), style = Stroke(7f))
            drawLine(Chair, Offset(700f, 856f), Offset(700f, 790f), strokeWidth = 7f)
            drawLine(Chair, Offset(820f, 856f), Offset(820f, 790f), strokeWidth = 7f)
            drawRoundRect(Chair, Offset(690f, 784f), Size(140f, 14f), CornerRadius(4f))
            drawRoundRect(Cushion, Offset(696f, 772f), Size(128f, 14f), CornerRadius(6f))
            drawLine(Chair, Offset(700f, 786f), Offset(690f, 650f), strokeWidth = 7f)
            drawLine(Chair, Offset(760f, 786f), Offset(756f, 650f), strokeWidth = 5f)
            drawLine(Chair, Offset(820f, 786f), Offset(824f, 650f), strokeWidth = 7f)
            drawRoundRect(Chair, Offset(680f, 640f), Size(156f, 18f), CornerRadius(8f))
        }
        // Yarn in a basket, with knitting needles.
        drawRoundRect(Color(0xFFC9A16E), Offset(880f, 820f), Size(90f, 46f), CornerRadius(8f))
        YarnColors.forEachIndexed { i, c -> drawCircle(c, 20f, Offset(898f + i * 28f, 818f - (i % 2) * 8f)) }
        drawLine(Color(0xFF8C9AA6), Offset(910f, 812f), Offset(960f, 770f), strokeWidth = 3f)
        drawLine(Color(0xFF8C9AA6), Offset(930f, 812f), Offset(980f, 776f), strokeWidth = 3f)
        // A little table with cookies and tea.
        drawRect(ChairDark, Offset(1100f, 820f), Size(10f, 46f)); drawRect(ChairDark, Offset(1240f, 820f), Size(10f, 46f))
        drawRoundRect(Chair, Offset(1086f, 808f), Size(178f, 14f), CornerRadius(4f))
        drawOval(Color(0xFFFDFCF8), Offset(1110f, 796f), Size(80f, 14f))
        repeat(3) { i -> drawCircle(Color(0xFFD9A05E), 13f, Offset(1130f + i * 20f, 794f - (i % 2) * 6f)) }
        drawRoundRect(Color(0xFF7FB7D9), Offset(1206f, 762f), Size(44f, 44f), CornerRadius(14f))
        drawLine(Color(0xFF7FB7D9), Offset(1206f, 776f), Offset(1190f, 768f), strokeWidth = 5f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF7FB7D9), 5f, Offset(1228f, 758f))
    }
}
