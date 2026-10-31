package com.redmesastudios.simpletv

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

private const val FallingLeafCount = 16
private const val RestingLeafCount = 30

// The fall backdrop: hills, trees at the edges, a leaf pile, and leaves drifting down,
// dressed up for whichever holiday is near.
@Composable
fun FallScene(modifier: Modifier = Modifier) {
    val falling = remember { fallingLeaves(FallingLeafCount) }
    val resting = remember { restingLeaves(RestingLeafCount) }
    val seconds = rememberSceneClock()
    val holiday = holidayOn(rememberToday())

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.width / SceneWidth
            if (holiday == Holiday.Halloween) drawHarvestMoon(unit)
            drawHillsAndTrees(unit)
            resting.forEach { drawLeaf(it.x + it.size / 2, it.y + it.size / 2, it.rotationDegrees, 1f, it.size, it.colorIndex, 0.85f, unit) }
        }
        // Its own layer, so each animation frame redraws only the leaves.
        Canvas(Modifier.fillMaxSize().graphicsLayer()) {
            val unit = size.width / SceneWidth
            val now = seconds.floatValue
            if (holiday == Holiday.Halloween) drawHalloweenFigures(unit, now)
            falling.forEach { leaf ->
                val pose = leaf.poseAt(now)
                drawLeaf(pose.centerX, pose.centerY, pose.rotationDegrees, pose.turnScaleX, leaf.size, leaf.colorIndex, 0.9f, unit)
            }
        }
    }
}

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
    colorIndex: Int,
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
        drawPath(LeafShape, LeafColors[colorIndex % LeafColors.size], alpha = alpha)
        drawLine(LeafVein, Offset(12f, 5f), Offset(12f, 21f), strokeWidth = 0.9f, alpha = alpha * 0.5f)
    }
}

private fun DrawScope.drawHillsAndTrees(unit: Float) {
    fun hill(startY: Float, c1: Offset, c2: Offset, mid: Offset, c3: Offset, c4: Offset, endY: Float) = Path().apply {
        moveTo(0f, startY * unit)
        cubicTo(c1.x * unit, c1.y * unit, c2.x * unit, c2.y * unit, mid.x * unit, mid.y * unit)
        cubicTo(c3.x * unit, c3.y * unit, c4.x * unit, c4.y * unit, SceneWidth * unit, endY * unit)
        lineTo(SceneWidth * unit, SceneHeight * unit)
        lineTo(0f, SceneHeight * unit)
        close()
    }
    drawPath(hill(880f, Offset(320f, 820f), Offset(640f, 870f), Offset(980f, 836f), Offset(1320f, 802f), Offset(1620f, 800f), 850f), HillFar)

    fun trunk(x: Float, y: Float, height: Float) = drawRoundRect(
        Trunk, Offset(x * unit, y * unit), Size(16f * unit, height * unit), CornerRadius(6f * unit),
    )
    fun crown(x: Float, y: Float, radius: Float, color: Color) = drawCircle(color, radius * unit, Offset(x * unit, y * unit))
    // The trees fade back as one shape, so the trunk doesn't show through the crowns.
    drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = 0.7f }) }
    trunk(58f, 760f, 170f)
    crown(66f, 740f, 74f, TreeCrowns[0])
    crown(18f, 790f, 56f, TreeCrowns[1])
    crown(120f, 800f, 50f, TreeCrowns[2])
    trunk(1852f, 740f, 190f)
    crown(1860f, 720f, 80f, TreeCrowns[1])
    crown(1806f, 780f, 54f, TreeCrowns[0])
    crown(1912f, 790f, 60f, TreeCrowns[2])
    drawIntoCanvas { it.restore() }

    drawPath(hill(990f, Offset(420f, 948f), Offset(820f, 1004f), Offset(1220f, 974f), Offset(1620f, 944f), Offset(1700f, 946f), 984f), HillNear)
}
