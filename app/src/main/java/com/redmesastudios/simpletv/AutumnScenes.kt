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
import kotlin.math.cos
import kotlin.math.sin

// The early-fall scenes, each on its own backdrop (SceneChoice.kt) except the first day of
// fall, which keeps the season's:
// Cheeseburger Day (September 18, in the diner): a tall cheeseburger, fries, a milkshake and
//   ketchup and mustard.
// Talk Like a Pirate Day (September 19, in the cove; "Ahoy, matey!"): an open treasure chest
//   glinting, a parrot on a post, a pirate flag waving and a barrel.
// First day of fall: a crate of apples, a rake by a leaf pile and pots of mums.
// Oktoberfest (September 24-30, in the beer garden): blue-and-white bunting, pretzels, steins
//   and gingerbread hearts swaying.
// Coffee Day (September 29, in the café): a latte with leaf art, a sack of beans, a
//   croissant and a French press.
// Taco Day (October 4, at the taco truck): a plate of tacos, salsa, limes and hot sauce.
// Cat Day (October 29, in the cozy room): a cat asleep on the window seat, breathing, and
//   another batting a ball of yarn.
// Drawn in design px (1920x1080).

private fun DrawScope.drawSteam(x: Float, y: Float, seconds: Float, count: Int = 2) {
    repeat(count) { i ->
        val p = (seconds * 0.35f + i / count.toFloat()) % 1f
        val sx = x - 8f * (count - 1) + i * 16f + 5f * sin(seconds * 1.5f + i)
        val sy = y - 60f * p
        drawPath(Path().apply { moveTo(sx, sy + 14f); quadraticTo(sx + 7f, sy + 7f, sx, sy); quadraticTo(sx - 7f, sy - 7f, sx, sy - 14f) },
            Color(0xFFD6DCE2), style = Stroke(3.5f, cap = StrokeCap.Round), alpha = 0.7f * sin(Math.PI.toFloat() * p))
    }
}

// ---- Cheeseburger Day ----

private val BunTop = Color(0xFFDDA368)
private val Sesame = Color(0xFFFBF2DC)
private val Lettuce = Color(0xFF8FC36A)
private val Tomato = Color(0xFFD9534F)
private val CheeseSlice = Color(0xFFF2C14E)
private val Patty = Color(0xFF6E4630)
private val Fry = Color(0xFFF2CC6A)

fun DrawScope.drawCheeseburgerDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        // The burger, layer on layer.
        val x = 960f
        val b = 866f
        drawOval(Color(0xFFDCD6CC), Offset(x - 120f, b - 14f), Size(240f, 18f))
        drawRoundRect(BunTop, Offset(x - 90f, b - 34f), Size(180f, 26f), CornerRadius(12f))
        drawRoundRect(Patty, Offset(x - 96f, b - 62f), Size(192f, 30f), CornerRadius(14f))
        drawPath(Path().apply { moveTo(x - 96f, b - 66f); lineTo(x + 96f, b - 66f); lineTo(x + 70f, b - 46f); lineTo(x + 40f, b - 66f); lineTo(x - 20f, b - 40f); lineTo(x - 50f, b - 66f); close() }, CheeseSlice)
        drawRoundRect(Tomato, Offset(x - 86f, b - 78f), Size(172f, 14f), CornerRadius(7f))
        drawPath(Path().apply {
            moveTo(x - 100f, b - 84f)
            var lx = x - 100f
            var up = true
            while (lx < x + 100f) { lx += 20f; lineTo(lx, b - 84f + if (up) 10f else -4f); up = !up }
            lineTo(x + 100f, b - 92f); lineTo(x - 100f, b - 92f); close()
        }, Lettuce)
        drawArc(BunTop, 180f, 180f, useCenter = true, topLeft = Offset(x - 96f, b - 170f), size = Size(192f, 160f))
        listOf(-50f to -140f, -10f to -150f, 30f to -142f, 60f to -124f, -70f to -118f, 10f to -122f).forEach { (dx, dy) -> drawOval(Sesame, Offset(x + dx - 4f, b + dy - 2f), Size(8f, 5f)) }
        drawSteam(x, b - 190f, seconds, 3)
        // Fries in a red carton.
        listOf(-24f, -12f, 0f, 12f, 24f, -18f, 18f).forEachIndexed { i, dx -> drawRect(Fry, Offset(700f + dx - 4f, 866f - 110f - (i % 3) * 10f), Size(9f, 70f)) }
        drawPath(Path().apply { moveTo(660f, 790f); lineTo(740f, 790f); lineTo(728f, 866f); lineTo(672f, 866f); close() }, Tomato)
        // A milkshake with a straw and a cherry.
        drawPath(Path().apply { moveTo(1150f, 740f); lineTo(1210f, 740f); lineTo(1196f, 850f); lineTo(1164f, 850f); close() }, Color(0xFFF7C6CF))
        drawRect(Color(0xFFDDEBF0), Offset(1170f, 850f), Size(20f, 16f))
        repeat(3) { k -> drawCircle(Color(0xFFFDFCF8), 20f - k * 4f, Offset(1180f, 734f - k * 12f)) }
        drawLine(Color(0xFFE8517A), Offset(1190f, 740f), Offset(1210f, 670f), strokeWidth = 6f)
        drawCircle(Color(0xFFC0303A), 9f, Offset(1180f, 694f))
        // Ketchup and mustard.
        drawRoundRect(Tomato, Offset(1300f, 780f), Size(36f, 86f), CornerRadius(10f))
        drawPath(Path().apply { moveTo(1306f, 780f); lineTo(1318f, 756f); lineTo(1330f, 780f); close() }, Tomato)
        drawRoundRect(CheeseSlice, Offset(1350f, 780f), Size(36f, 86f), CornerRadius(10f))
        drawPath(Path().apply { moveTo(1356f, 780f); lineTo(1368f, 756f); lineTo(1380f, 780f); close() }, CheeseSlice)
    }
}

// ---- Talk Like a Pirate Day ----

private val ChestWood = Color(0xFF8C5A3A)
private val ChestBand = Color(0xFFC9963A)
private val Coin = Color(0xFFE9B949)
private val ParrotRed = Color(0xFFD9534F)
private val ParrotBlue = Color(0xFF5FA8D3)
private val ParrotYellow = Color(0xFFF2C14E)
private val FlagBlack = Color(0xFF2E2A28)

fun DrawScope.drawPirateDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        // An open treasure chest heaped with gold, coins glinting.
        val x = 960f
        val b = 866f
        drawPath(Path().apply { moveTo(x - 90f, b - 100f); lineTo(x - 76f, b - 160f); lineTo(x + 76f, b - 160f); lineTo(x + 90f, b - 100f); close() }, ChestWood)
        drawRect(ChestBand, Offset(x - 76f, b - 160f), Size(152f, 10f))
        repeat(16) { i ->
            val cx = x - 70f + (i * 37 % 140)
            val cy = b - 100f - (i * 13 % 26)
            drawOval(Color(0xFFC9963A), Offset(cx - 11f, cy - 4f), Size(22f, 10f))
            drawOval(Coin, Offset(cx - 10f, cy - 6f), Size(20f, 9f))
            val glint = (sin(seconds * 2.4f + i * 1.3f) - 0.8f).coerceAtLeast(0f) * 5f
            if (glint > 0f) drawLine(Color.White, Offset(cx - 6f * glint, cy - 2f), Offset(cx + 6f * glint, cy - 2f), strokeWidth = 2f, alpha = glint)
        }
        drawRect(ChestWood, Offset(x - 92f, b - 100f), Size(184f, 100f))
        listOf(-60f, 60f).forEach { dx -> drawRect(ChestBand, Offset(x + dx - 6f, b - 100f), Size(12f, 100f)) }
        drawRect(ChestBand, Offset(x - 16f, b - 76f), Size(32f, 26f))
        // A parrot on a post, bobbing.
        val bob = 3f * sin(seconds * 2f)
        drawRect(ChestWood, Offset(714f, 780f), Size(12f, 86f))
        drawRect(ChestWood, Offset(690f, 776f), Size(60f, 8f))
        val p = Offset(720f, 744f + bob)
        drawPath(Path().apply { moveTo(p.x - 6f, p.y + 24f); lineTo(p.x - 18f, p.y + 64f); lineTo(p.x + 4f, p.y + 26f); close() }, ParrotBlue)
        drawOval(ParrotRed, Offset(p.x - 20f, p.y - 10f), Size(40f, 44f))
        drawOval(ParrotYellow, Offset(p.x - 18f, p.y + 6f), Size(20f, 22f))
        drawCircle(ParrotRed, 15f, Offset(p.x + 6f, p.y - 18f))
        drawCircle(Color.White, 5f, Offset(p.x + 10f, p.y - 22f))
        drawCircle(FlagBlack, 2.5f, Offset(p.x + 11f, p.y - 22f))
        drawPath(Path().apply { moveTo(p.x + 18f, p.y - 22f); quadraticTo(p.x + 32f, p.y - 16f, p.x + 22f, p.y - 6f); lineTo(p.x + 18f, p.y - 14f); close() }, Color(0xFF4A4440))
        // A pirate flag rippling on a pole.
        drawLine(ChestWood, Offset(1180f, 866f), Offset(1180f, 660f), strokeWidth = 5f)
        repeat(12) { k ->
            val along = (k + 0.5f) / 12
            val lift = 5f * along * sin(along * 4f - seconds * 2.6f)
            drawRect(FlagBlack, Offset(1182f + k * 10f, 664f + lift), Size(10.6f, 76f))
        }
        val c = Offset(1240f, 694f + 5f * 0.5f * sin(0.5f * 4f - seconds * 2.6f))
        drawCircle(Color.White, 13f, c)
        drawRect(Color.White, Offset(c.x - 8f, c.y + 6f), Size(16f, 10f))
        drawCircle(FlagBlack, 3.5f, Offset(c.x - 5f, c.y - 2f)); drawCircle(FlagBlack, 3.5f, Offset(c.x + 5f, c.y - 2f))
        drawLine(Color.White, Offset(c.x - 22f, c.y + 20f), Offset(c.x + 22f, c.y + 34f), strokeWidth = 5f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(c.x + 22f, c.y + 20f), Offset(c.x - 22f, c.y + 34f), strokeWidth = 5f, cap = StrokeCap.Round)
        // A barrel.
        drawRoundRect(Color(0xFFA9845E), Offset(1330f, 786f), Size(80f, 80f), CornerRadius(20f))
        listOf(800f, 846f).forEach { y -> drawRect(Color(0xFF6E5440), Offset(1330f, y), Size(80f, 6f)) }
    }
}

// ---- First day of fall ----

private val Apple = Color(0xFFC9483F)
private val Crate = Color(0xFFC9A57E)
private val MumColors = listOf(Color(0xFFE2A85E), Color(0xFFC9483F), Color(0xFFF2C14E))
private val LeafPile = listOf(Color(0xFFE2A85E), Color(0xFFD9775A), Color(0xFFC9A04E), Color(0xFFB8653E))

fun DrawScope.drawFallDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        // A leaf pile with a rake leaning on it.
        repeat(40) { i ->
            val a = i * 0.5f
            drawOval(LeafPile[i % 4], Offset(600f + 60f * cos(a) * (i % 5) / 4f - 10f, 850f - 30f * abs(sin(a)) * (i % 4) / 3f), Size(22f, 12f))
        }
        drawLine(Color(0xFF9C7A55), Offset(660f, 860f), Offset(560f, 680f), strokeWidth = 5f)
        drawLine(Color(0xFF8C9AA6), Offset(640f, 870f), Offset(700f, 850f), strokeWidth = 4f)
        // A crate of apples.
        drawRect(Crate, Offset(860f, 800f), Size(200f, 66f))
        listOf(818f, 840f).forEach { y -> drawRect(Color(0xFFB08A62), Offset(860f, y), Size(200f, 4f)) }
        repeat(7) { i -> drawCircle(Apple, 18f, Offset(880f + i * 27f, 796f - (i % 2) * 6f)); drawLine(Color(0xFF6E5440), Offset(880f + i * 27f, 780f - (i % 2) * 6f), Offset(883f + i * 27f, 772f - (i % 2) * 6f), strokeWidth = 2f) }
        // Pots of mums.
        listOf(1180f, 1300f).forEachIndexed { i, x ->
            drawPath(Path().apply { moveTo(x - 36f, 822f); lineTo(x + 36f, 822f); lineTo(x + 26f, 866f); lineTo(x - 26f, 866f); close() }, Color(0xFFD9895E))
            repeat(9) { k -> drawCircle(MumColors[(i + k) % 3], 14f, Offset(x - 32f + (k % 5) * 16f, 806f - (k / 5) * 18f + 2f * sin(seconds + k))) }
        }
    }
}

// ---- Oktoberfest ----

private val BavarianBlue = Color(0xFF6FA0D8)
private val Pretzel = Color(0xFFB8753E)
private val Stein = Color(0xFFDDEBF0)
private val Beer = Color(0xFFE9B949)
private val Foam = Color(0xFFFDFCF8)
private val Gingerbread = Color(0xFFB8753E)

fun DrawScope.drawOktoberfestFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawPennantBunting(seconds, listOf(BavarianBlue, Color(0xFFFBF6EC)), y = 650f, anchors = listOf(260f, 620f, 960f, 1300f, 1660f))
        // Gingerbread hearts hanging, swaying.
        listOf(800f, 1120f).forEachIndexed { i, x ->
            val sway = 6f * sin(seconds * 1.2f + i)
            withTransform({ rotate(sway, pivot = Offset(x, 680f)) }) {
                drawLine(Color(0xFFC0503A), Offset(x, 680f), Offset(x, 712f), strokeWidth = 2f)
                drawHeart(Offset(x, 742f), 70f, Gingerbread)
                drawPath(Path().apply { moveTo(x - 24f, 734f); quadraticTo(x, 760f, x + 24f, 734f) }, Color.White, style = Stroke(3f))
            }
        }
        // Pretzels on a peg rack.
        drawLine(Color(0xFF9C7A55), Offset(620f, 866f), Offset(620f, 740f), strokeWidth = 6f)
        listOf(756f, 806f).forEach { y -> drawLine(Color(0xFF9C7A55), Offset(560f, y), Offset(680f, y), strokeWidth = 4f) }
        listOf(Offset(590f, 760f), Offset(650f, 760f), Offset(620f, 810f)).forEach { drawPretzel(it) }
        // Two steins with foam heads.
        listOf(930f, 1010f).forEach { x ->
            drawRoundRect(Stein, Offset(x - 28f, 786f), Size(56f, 80f), CornerRadius(8f), alpha = 0.9f)
            drawRect(Beer, Offset(x - 24f, 800f), Size(48f, 62f))
            repeat(3) { k -> drawCircle(Foam, 13f, Offset(x - 16f + k * 16f, 788f)) }
            drawArc(Stein, -80f, 160f, useCenter = false, topLeft = Offset(x + 18f, 802f), size = Size(28f, 40f), style = Stroke(7f))
        }
    }
}

private fun DrawScope.drawPretzel(center: Offset) {
    drawCircle(Pretzel, 20f, Offset(center.x - 11f, center.y), style = Stroke(9f))
    drawCircle(Pretzel, 20f, Offset(center.x + 11f, center.y), style = Stroke(9f))
    repeat(5) { k -> drawCircle(Color.White, 1.6f, Offset(center.x - 22f + k * 11f, center.y - 18f + (k % 2) * 4f)) }
}

// ---- Coffee Day ----

private val Cup = Color(0xFFFDFCF8)
private val Latte = Color(0xFFD9B48A)
private val Burlap = Color(0xFFC9B48A)
private val Bean = Color(0xFF6E4630)
private val Croissant = Color(0xFFE2A85E)

fun DrawScope.drawCoffeeDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        // A sack of beans, a few spilling out.
        drawPath(Path().apply { moveTo(660f, 866f); quadraticTo(640f, 800f, 670f, 760f); lineTo(780f, 760f); quadraticTo(810f, 800f, 790f, 866f); close() }, Burlap)
        drawLine(Color(0xFF9C8A5E), Offset(668f, 774f), Offset(782f, 774f), strokeWidth = 4f)
        repeat(9) { i -> drawOval(Bean, Offset(680f + (i * 23 % 90), 748f - (i % 3) * 6f), Size(14f, 10f)) }
        repeat(5) { i -> drawOval(Bean, Offset(800f + i * 14f, 856f + (i % 2) * 4f), Size(14f, 10f)) }
        // The latte, with a leaf poured in the foam.
        val x = 960f
        drawOval(Color(0xFFDCD6CC), Offset(x - 70f, 852f), Size(140f, 18f))
        drawPath(Path().apply { moveTo(x - 52f, 790f); lineTo(x + 52f, 790f); quadraticTo(x + 50f, 852f, x, 852f); quadraticTo(x - 50f, 852f, x - 52f, 790f); close() }, Cup)
        drawArc(Cup, -70f, 160f, useCenter = false, topLeft = Offset(x + 38f, 798f), size = Size(34f, 34f), style = Stroke(7f))
        drawOval(Latte, Offset(x - 48f, 780f), Size(96f, 22f))
        drawLine(Cup, Offset(x - 26f, 791f), Offset(x + 26f, 791f), strokeWidth = 2f)
        repeat(4) { k -> drawArc(Cup, 200f, 140f, useCenter = false, topLeft = Offset(x - 22f + k * 10f, 784f), size = Size(12f, 12f), style = Stroke(2f)) }
        drawSteam(x, 760f, seconds, 3)
        // A croissant and a French press.
        drawOval(Color(0xFFFDFCF8), Offset(1110f, 850f), Size(120f, 16f))
        drawArc(Croissant, 180f, 180f, useCenter = true, topLeft = Offset(1120f, 816f), size = Size(100f, 70f))
        listOf(1140f, 1170f, 1200f).forEach { cx -> drawLine(Color(0xFFC98A44), Offset(cx, 822f), Offset(cx - 6f, 850f), strokeWidth = 3f) }
        drawRoundRect(Stein, Offset(1290f, 760f), Size(60f, 106f), CornerRadius(6f), alpha = 0.9f)
        drawRect(Bean, Offset(1294f, 800f), Size(52f, 62f), alpha = 0.85f)
        drawRect(Color(0xFF8C9AA6), Offset(1286f, 752f), Size(68f, 10f))
        drawLine(Color(0xFF8C9AA6), Offset(1320f, 752f), Offset(1320f, 726f), strokeWidth = 4f)
        drawCircle(Color(0xFF8C9AA6), 6f, Offset(1320f, 724f))
    }
}

// ---- Taco Day ----

private val Shell = Color(0xFFF2CC6A)
private val Cheddar = Color(0xFFF2A93B)
private val Lime = Color(0xFF8FC36A)
private val Salsa = Color(0xFFD0573E)

fun DrawScope.drawTacoDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawOval(Color(0xFFDCD6CC), Offset(810f, 846f), Size(300f, 24f))
        drawOval(Color(0xFFFDFCF8), Offset(814f, 842f), Size(292f, 22f))
        listOf(880f, 960f, 1040f).forEachIndexed { i, x ->
            drawTaco(Offset(x, 852f), i)
        }
        // Salsa bowls, limes and hot sauce.
        listOf(Triple(1200f, Salsa, 0), Triple(1290f, Color(0xFF8FC36A), 1)).forEach { (x, c, _) ->
            drawArc(Color(0xFFE8B86A), 0f, 180f, useCenter = true, topLeft = Offset(x - 40f, 830f), size = Size(80f, 50f))
            drawOval(c, Offset(x - 36f, 846f), Size(72f, 14f))
        }
        listOf(700f, 740f).forEach { x -> drawArc(Lime, 180f, 180f, useCenter = true, topLeft = Offset(x - 20f, 848f), size = Size(40f, 36f)); drawArc(Color(0xFFDCEFB8), 180f, 180f, useCenter = true, topLeft = Offset(x - 14f, 854f), size = Size(28f, 24f)) }
        drawRoundRect(Color(0xFFC9483F), Offset(1380f, 780f), Size(36f, 86f), CornerRadius(8f))
        drawRect(Color(0xFF6DBE7B), Offset(1384f, 760f), Size(28f, 22f))
        drawRect(Color(0xFFFBF6EC), Offset(1380f, 806f), Size(36f, 26f))
    }
}

private fun DrawScope.drawTaco(base: Offset, index: Int) {
    withTransform({ rotate((index - 1) * 6f, pivot = base) }) {
        drawArc(Lettuce, 180f, 180f, useCenter = true, topLeft = Offset(base.x - 40f, base.y - 80f), size = Size(80f, 70f))
        repeat(4) { k -> drawCircle(Tomato, 6f, Offset(base.x - 24f + k * 16f, base.y - 50f - (k % 2) * 6f)) }
        repeat(6) { k -> drawLine(Cheddar, Offset(base.x - 28f + k * 11f, base.y - 56f), Offset(base.x - 22f + k * 11f, base.y - 44f), strokeWidth = 3f, cap = StrokeCap.Round) }
        drawArc(Shell, 0f, 180f, useCenter = true, topLeft = Offset(base.x - 44f, base.y - 70f), size = Size(88f, 80f))
        drawArc(Color(0xFFE2B85A), 10f, 160f, useCenter = false, topLeft = Offset(base.x - 38f, base.y - 64f), size = Size(76f, 70f), style = Stroke(2f))
    }
}

// ---- Cat Day ----

private val CatOrange = Color(0xFFE2A060)
private val CatStripe = Color(0xFFC98042)
private val CatGray = Color(0xFF9C9CA6)
private val CatGrayDark = Color(0xFF7E7E88)
private val Yarn = Color(0xFFC0503A)

fun DrawScope.drawCatDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        // An orange cat curled up asleep on the window seat, breathing slowly.
        val breathe = 1f + 0.04f * sin(seconds * 1.6f)
        val cx = 900f
        val cy = 818f
        withTransform({ scale(1f, breathe, pivot = Offset(cx, cy + 14f)) }) {
            drawOval(CatOrange, Offset(cx - 70f, cy - 30f), Size(140f, 50f))
            listOf(-30f, 0f, 30f).forEach { dx -> drawLine(CatStripe, Offset(cx + dx - 6f, cy - 28f), Offset(cx + dx + 6f, cy - 10f), strokeWidth = 5f, cap = StrokeCap.Round) }
        }
        drawCircle(CatOrange, 24f, Offset(cx + 56f, cy - 8f))
        drawPath(Path().apply { moveTo(cx + 40f, cy - 24f); lineTo(cx + 44f, cy - 44f); lineTo(cx + 56f, cy - 30f); close() }, CatOrange)
        drawPath(Path().apply { moveTo(cx + 60f, cy - 30f); lineTo(cx + 72f, cy - 44f); lineTo(cx + 76f, cy - 24f); close() }, CatOrange)
        drawArc(Color(0xFF5A3E28), 20f, 140f, useCenter = false, topLeft = Offset(cx + 44f, cy - 14f), size = Size(10f, 6f), style = Stroke(2f))
        drawArc(Color(0xFF5A3E28), 20f, 140f, useCenter = false, topLeft = Offset(cx + 60f, cy - 14f), size = Size(10f, 6f), style = Stroke(2f))
        drawPath(Path().apply { moveTo(cx - 66f, cy + 6f); quadraticTo(cx - 20f, cy + 28f, cx + 30f, cy + 14f) }, CatOrange, style = Stroke(12f, cap = StrokeCap.Round))
        // "Zz" drifting up from the sleeper.
        val z = (seconds * 0.3f) % 1f
        drawLine(Color(0xFF9C8C7E), Offset(cx + 90f, cy - 50f - 40f * z), Offset(cx + 100f, cy - 50f - 40f * z), strokeWidth = 2f, alpha = 1f - z)
        // A gray cat on the floor batting a ball of yarn.
        val bat = sin(seconds * 3f).coerceAtLeast(0f)
        val gx = 1320f
        val ball = Offset(gx + 90f + 14f * bat, 852f)
        drawCircle(Yarn, 18f, ball)
        repeat(3) { k -> drawArc(Color(0xFFA03E2C), 30f + k * 40f, 120f, useCenter = false, topLeft = Offset(ball.x - 14f, ball.y - 14f), size = Size(28f, 28f), style = Stroke(2f)) }
        drawPath(Path().apply { moveTo(ball.x - 18f, ball.y + 6f); quadraticTo(ball.x - 60f, ball.y + 14f, ball.x - 100f, ball.y + 12f) }, Yarn, style = Stroke(2f))
        drawOval(CatGray, Offset(gx - 50f, 806f), Size(100f, 50f))
        drawPath(Path().apply { moveTo(gx - 46f, 826f); quadraticTo(gx - 90f, 790f, gx - 76f, 760f) }, CatGray, style = Stroke(10f, cap = StrokeCap.Round))
        drawCircle(CatGray, 24f, Offset(gx + 40f, 800f))
        drawPath(Path().apply { moveTo(gx + 24f, 786f); lineTo(gx + 26f, 764f); lineTo(gx + 40f, 778f); close() }, CatGray)
        drawPath(Path().apply { moveTo(gx + 44f, 778f); lineTo(gx + 58f, 764f); lineTo(gx + 60f, 786f); close() }, CatGray)
        drawCircle(Color(0xFF6DBE7B), 4f, Offset(gx + 34f, 798f)); drawCircle(Color(0xFF6DBE7B), 4f, Offset(gx + 50f, 798f))
        drawLine(CatGrayDark, Offset(gx + 40f, 840f), Offset(gx + 60f + 30f * bat, 836f - 20f * bat), strokeWidth = 9f, cap = StrokeCap.Round)
        drawLine(CatGrayDark, Offset(gx - 30f, 840f), Offset(gx - 30f, 862f), strokeWidth = 9f, cap = StrokeCap.Round)
        drawLine(CatGrayDark, Offset(gx + 10f, 840f), Offset(gx + 10f, 862f), strokeWidth = 9f, cap = StrokeCap.Round)
        // A food bowl.
        drawArc(Color(0xFF5FA8D3), 0f, 180f, useCenter = true, topLeft = Offset(500f, 836f), size = Size(80f, 50f))
        drawOval(Color(0xFF8C5A3A), Offset(506f, 852f), Size(68f, 12f))
    }
}
