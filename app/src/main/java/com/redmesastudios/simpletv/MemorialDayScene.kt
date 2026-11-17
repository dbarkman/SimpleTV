package com.redmesastudios.simpletv

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

// Memorial Day weekend (Saturday through the last Monday of May) in a national cemetery: a
// small flag beside each headstone in the front row, a tall flag waving in the middle, a
// wreath on an easel and red poppies in the grass. The greeting says "Honoring the fallen"
// on the Monday. Drawn in design px (1920x1080).

private val Poppy = Color(0xFFD2403A)
private val PoppyDark = Color(0xFF2A211B)
private val PoppyStem = Color(0xFF6E9C6D)

private val Poppies = listOf(870f, 905f, 1020f, 1050f, 280f, 480f, 680f, 1240f, 1440f, 1640f)

fun DrawScope.drawMemorialDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        FrontHeadstones.forEachIndexed { i, x -> drawHandFlag(x + 24f, 868f, seconds, i * 0.9f) }
        drawWavingFlag(960f, 866f, 650f, 128f, seconds)
        drawWreath(1080f, 866f, seconds)
        Poppies.forEachIndexed { i, x -> drawPoppy(Offset(x, 852f + (i % 3) * 6f), seconds, i) }
    }
}

// A red poppy on a thin stem, nodding.
private fun DrawScope.drawPoppy(base: Offset, seconds: Float, index: Int) {
    val nod = 2f * sin(seconds * 1.1f + index)
    val head = Offset(base.x + nod, base.y - 30f)
    drawLine(PoppyStem, Offset(base.x, base.y + 14f), head, strokeWidth = 2f)
    repeat(4) { k ->
        val a = k * 1.571f + 0.4f
        drawCircle(Poppy, 7f, Offset(head.x + 6f * cos(a), head.y + 6f * sin(a)))
    }
    drawCircle(PoppyDark, 3f, head)
}
