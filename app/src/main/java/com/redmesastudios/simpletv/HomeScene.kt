package com.redmesastudios.simpletv

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.redmesastudios.simpletv.ui.theme.HillFar
import com.redmesastudios.simpletv.ui.theme.HillNear
import com.redmesastudios.simpletv.ui.theme.LeafColors
import com.redmesastudios.simpletv.ui.theme.LeafVein
import com.redmesastudios.simpletv.ui.theme.TreeCrowns
import com.redmesastudios.simpletv.ui.theme.Trunk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

// A date to show the scene for instead of today, to see a holiday ahead of time:
//   adb shell am start -n com.redmesastudios.simpletv/.MainActivity --es scene_date 2026-12-25
// Pressing Home goes back to today.
object ScenePreview {
    val date = MutableStateFlow<LocalDate?>(null)
}

// The day the home screen decorates for: today (moving on at midnight) or a preview date.
@Composable
fun rememberSceneDay(): LocalDate {
    val preview by ScenePreview.date.collectAsState()
    val today = rememberToday()
    return preview ?: today
}

// The backdrop: hills, trees at the edges and something drifting down, all in the season's
// colors, dressed up for a family birthday or anniversary or whichever holiday is on.
@Composable
fun HomeScene(day: LocalDate = rememberSceneDay(), modifier: Modifier = Modifier) {
    val house by HouseSettings.state.collectAsState()
    val family = familyDaysOn(day, house.familyDays.map { it.day })
    val holiday = if (family.isEmpty()) holidayOn(day, house.packs, DrawnHolidays) else null
    // Christmas brings the snow, even in the weeks before winter starts.
    val season = if (holiday == Holiday.Christmas) Season.Winter else seasonOn(day)
    val look = SeasonLooks.getValue(season)
    val drifting = remember(season) { fallingLeaves(look.driftingCount) }
    val resting = remember { restingLeaves(RestingLeafCount) }
    val seconds = rememberSceneClock()
    val anniversary = family.isNotEmpty() && family.none { it.kind == FamilyDayKind.Birthday }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.width / SceneWidth
            if (holiday == Holiday.Halloween) drawHarvestMoon(unit)
            if (holiday == Holiday.MlkDay) drawRisingSun(unit)
            drawHillsAndTrees(look, unit)
            if (look.restingColors != null) {
                resting.forEach { drawLeaf(it.x + it.size / 2, it.y + it.size / 2, it.rotationDegrees, 1f, it.size * look.driftingScale, look.restingColors[it.colorIndex % look.restingColors.size], look.veins, 0.85f, unit) }
            }
        }
        // Its own layer, so each animation frame redraws only what moves.
        Canvas(Modifier.fillMaxSize().graphicsLayer()) {
            val unit = size.width / SceneWidth
            val now = seconds.floatValue
            when (holiday) {
                Holiday.Halloween -> drawHalloweenFigures(unit, now)
                Holiday.ElectionDay -> drawElectionFigures(unit, now)
                Holiday.VeteransDay -> drawVeteransDayFigures(unit, now)
                Holiday.Thanksgiving -> drawThanksgivingFigures(unit, now)
                Holiday.Christmas -> drawChristmasFigures(unit, now)
                Holiday.NewYear -> drawNewYearFigures(unit, now, newYearShown(day))
                Holiday.MlkDay -> drawMlkDayFigures(unit, now)
                Holiday.SuperBowl -> drawGameDayFigures(unit, now)
                Holiday.Valentines -> drawValentinesFigures(unit, now)
                Holiday.PresidentsDay -> drawPresidentsDayFigures(unit, now)
                Holiday.Epiphany -> drawEpiphanyFigures(unit, now)
                Holiday.LunarNewYear -> drawLunarNewYearFigures(unit, now)
                Holiday.PopcornDay -> drawPopcornDayFigures(unit, now)
                Holiday.GroundhogDay -> drawGroundhogDayFigures(unit, now)
                else -> {}
            }
            if (family.isNotEmpty()) drawCelebration(unit, now, anniversary)
            drifting.forEach { item ->
                val pose = item.poseAt(now)
                val color = look.driftingColors[item.colorIndex % look.driftingColors.size]
                when (look.drifting) {
                    Drifting.Leaves, Drifting.Petals ->
                        drawLeaf(pose.centerX, pose.centerY, pose.rotationDegrees, pose.turnScaleX, item.size * look.driftingScale, color, look.veins, 0.9f, unit)
                    Drifting.Snow -> drawSnowflake(pose.centerX, pose.centerY, pose.rotationDegrees * 3, item.size * look.driftingScale, color, unit)
                    Drifting.Seeds -> drawSeed(pose.centerX, pose.centerY, pose.rotationDegrees, item.size * look.driftingScale, color, unit)
                }
            }
        }
    }
}

private const val RestingLeafCount = 30

private enum class Drifting { Leaves, Snow, Petals, Seeds }

private class SeasonLook(
    val hillFar: Color,
    val hillNear: Color,
    val trunk: Color,
    val crowns: List<Color>,
    val drifting: Drifting,
    val driftingCount: Int,
    val driftingScale: Float,
    val driftingColors: List<Color>,
    // What lies on the ground at the right, if anything.
    val restingColors: List<Color>?,
    val veins: Boolean,
    // Snow on the tree crowns.
    val snowCaps: Boolean = false,
)

private val SeasonLooks = mapOf(
    Season.Fall to SeasonLook(HillFar, HillNear, Trunk, TreeCrowns, Drifting.Leaves, 16, 1f, LeafColors, LeafColors, veins = true),
    Season.Winter to SeasonLook(
        hillFar = Color(0xFFEEF0F0), hillNear = Color(0xFFE3E8EB), trunk = Color(0xFFA8968A),
        crowns = listOf(Color(0xFF9FB3A6), Color(0xFFAFC2B3), Color(0xFF93A99B)),
        drifting = Drifting.Snow, driftingCount = 24, driftingScale = 0.6f,
        driftingColors = listOf(Color(0xFFB9C7D3), Color(0xFFC8D4DE), Color(0xFFAEBDCB)),
        restingColors = null, veins = false, snowCaps = true,
    ),
    Season.Spring to SeasonLook(
        hillFar = Color(0xFFE6EAD2), hillNear = Color(0xFFD9E3C1), trunk = Color(0xFFB49C88),
        crowns = listOf(Color(0xFFF1C9D0), Color(0xFFF5DADD), Color(0xFFE9B6C2)),
        drifting = Drifting.Petals, driftingCount = 16, driftingScale = 0.55f,
        driftingColors = listOf(Color(0xFFE8A3B3), Color(0xFFF0BCC6), Color(0xFFDD8FA3), Color(0xFFF4CCD3)),
        restingColors = listOf(Color(0xFFE8A3B3), Color(0xFFF0BCC6), Color(0xFFDD8FA3)), veins = false,
    ),
    Season.Summer to SeasonLook(
        hillFar = Color(0xFFE1E8CB), hillNear = Color(0xFFD2DFB5), trunk = Color(0xFFAE9580),
        crowns = listOf(Color(0xFFB8D29F), Color(0xFFC9DEB2), Color(0xFFA6C58C)),
        drifting = Drifting.Seeds, driftingCount = 10, driftingScale = 1f,
        driftingColors = listOf(Color(0xFF9C8C7E), Color(0xFFAA9B8C)),
        restingColors = null, veins = false,
    ),
)

// Seconds of animation, advancing only while the home screen is in front:
// leaves hold still (and cost nothing) while another app is playing.
// Steps at about 30 fps: the leaves drift slowly enough that it looks the same,
// and it halves the stick's work on a screen that sits idle all day.
@Composable
private fun rememberSceneClock(): MutableFloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = withFrameNanos { it }
            while (true) {
                delay(SceneFrameMillis)
                withFrameNanos { frame ->
                    seconds.floatValue += (frame - last) / 1_000_000_000f
                    last = frame
                }
            }
        }
    }
    return seconds
}

private const val SceneFrameMillis = 25L

// Today's date, moving on at midnight, so decorations come and go while the TV stays on.
@Composable
private fun rememberToday(): LocalDate {
    val today = remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val untilMidnight = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay())
            delay(untilMidnight.toMillis() + 1_000)
            today.value = LocalDate.now()
        }
    }
    return today.value
}

private val LeafShape = Path().apply {
    moveTo(12f, 1.5f)
    cubicTo(17.5f, 5.5f, 19.5f, 12f, 12f, 22.5f)
    cubicTo(4.5f, 12f, 6.5f, 5.5f, 12f, 1.5f)
    close()
}

private fun DrawScope.drawLeaf(
    centerX: Float,
    centerY: Float,
    rotationDegrees: Float,
    turnScaleX: Float,
    size: Float,
    color: Color,
    veins: Boolean,
    alpha: Float,
    unit: Float,
) {
    val scale = size / 24f * unit
    withTransform({
        translate(centerX * unit, centerY * unit)
        rotate(rotationDegrees, pivot = Offset.Zero)
        scale(turnScaleX * scale, scale, pivot = Offset.Zero)
        translate(-12f, -12f)
    }) {
        drawPath(LeafShape, color, alpha = alpha)
        if (veins) drawLine(LeafVein, Offset(12f, 5f), Offset(12f, 21f), strokeWidth = 0.9f, alpha = alpha * 0.5f)
    }
}

private fun DrawScope.drawHillsAndTrees(look: SeasonLook, unit: Float) {
    fun hill(startY: Float, c1: Offset, c2: Offset, mid: Offset, c3: Offset, c4: Offset, endY: Float) = Path().apply {
        moveTo(0f, startY * unit)
        cubicTo(c1.x * unit, c1.y * unit, c2.x * unit, c2.y * unit, mid.x * unit, mid.y * unit)
        cubicTo(c3.x * unit, c3.y * unit, c4.x * unit, c4.y * unit, SceneWidth * unit, endY * unit)
        lineTo(SceneWidth * unit, SceneHeight * unit)
        lineTo(0f, SceneHeight * unit)
        close()
    }
    drawPath(hill(880f, Offset(320f, 820f), Offset(640f, 870f), Offset(980f, 836f), Offset(1320f, 802f), Offset(1620f, 800f), 850f), look.hillFar)

    fun trunk(x: Float, y: Float, height: Float) = drawRoundRect(
        look.trunk, Offset(x * unit, y * unit), Size(16f * unit, height * unit), CornerRadius(6f * unit),
    )
    fun crown(x: Float, y: Float, radius: Float, color: Color) {
        drawCircle(color, radius * unit, Offset(x * unit, y * unit))
        // Snow settled along the top of the crown.
        if (look.snowCaps) {
            drawArc(
                SnowCap, 208f, 124f, useCenter = false,
                topLeft = Offset((x - radius) * unit, (y - radius) * unit),
                size = Size(2 * radius * unit, 2 * radius * unit),
            )
            // A soft, lumpy lower edge rather than a straight cut.
            val edgeY = y - radius * 0.47f
            listOf(-0.55f, -0.18f, 0.2f, 0.56f).forEach { dx ->
                drawCircle(SnowCap, radius * 0.17f * unit, Offset((x + dx * radius) * unit, edgeY * unit))
            }
        }
    }
    // The trees fade back as one shape, so the trunk doesn't show through the crowns.
    drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = 0.7f }) }
    trunk(58f, 760f, 170f)
    crown(66f, 740f, 74f, look.crowns[0])
    crown(18f, 790f, 56f, look.crowns[1])
    crown(120f, 800f, 50f, look.crowns[2])
    trunk(1852f, 740f, 190f)
    crown(1860f, 720f, 80f, look.crowns[1])
    crown(1806f, 780f, 54f, look.crowns[0])
    crown(1912f, 790f, 60f, look.crowns[2])
    drawIntoCanvas { it.restore() }

    drawPath(hill(990f, Offset(420f, 948f), Offset(820f, 1004f), Offset(1220f, 974f), Offset(1620f, 944f), Offset(1700f, 946f), 984f), look.hillNear)
}

private val SnowCap = Color(0xFFFAFBFC)

// A six-armed snowflake, size across in design px.
private fun DrawScope.drawSnowflake(centerX: Float, centerY: Float, rotationDegrees: Float, size: Float, color: Color, unit: Float) {
    val arm = size / 2 * unit
    withTransform({
        translate(centerX * unit, centerY * unit)
        rotate(rotationDegrees, pivot = Offset.Zero)
    }) {
        repeat(3) { i ->
            withTransform({ rotate(i * 60f, pivot = Offset.Zero) }) {
                drawLine(color, Offset(-arm, 0f), Offset(arm, 0f), strokeWidth = 2.2f * unit, cap = StrokeCap.Round)
                // Little branches near each tip.
                for (side in listOf(-1f, 1f)) {
                    val base = Offset(side * arm * 0.55f, 0f)
                    drawLine(color, base, Offset(side * arm * 0.8f, -arm * 0.25f), strokeWidth = 1.6f * unit, cap = StrokeCap.Round)
                    drawLine(color, base, Offset(side * arm * 0.8f, arm * 0.25f), strokeWidth = 1.6f * unit, cap = StrokeCap.Round)
                }
            }
        }
        drawCircle(color, 2.2f * unit, Offset.Zero)
    }
}

// A dandelion seed floating on its fluff.
private fun DrawScope.drawSeed(centerX: Float, centerY: Float, tiltDegrees: Float, size: Float, color: Color, unit: Float) {
    val r = size / 2 * unit
    withTransform({
        translate(centerX * unit, centerY * unit)
        rotate(tiltDegrees * 0.5f, pivot = Offset.Zero)
    }) {
        val top = Offset(0f, -r * 0.2f)
        repeat(9) { i ->
            val angle = Math.toRadians((200.0 + i * 17.5))
            drawLine(color, top, Offset(top.x + r * kotlin.math.cos(angle).toFloat(), top.y + r * 0.8f * kotlin.math.sin(angle).toFloat()), strokeWidth = 1f * unit, alpha = 0.7f)
        }
        drawLine(color, top, Offset(0f, r * 0.9f), strokeWidth = 1.3f * unit, alpha = 0.8f)
        drawOval(color, Offset(-1.6f * unit, r * 0.85f), Size(3.2f * unit, 6f * unit), alpha = 0.9f)
    }
}
