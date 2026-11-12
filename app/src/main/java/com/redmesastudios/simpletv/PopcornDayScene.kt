package com.redmesastudios.simpletv

import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas

// National Popcorn Day (January 19th) on the winter scene: a big striped bucket heaped with
// popcorn in the middle of the hill, kernels popping up out of it and tumbling back in, two
// smaller buckets, a couple of movie tickets stuck in the snow and a few stray kernels. The
// greeting says "Happy Popcorn Day!". Drawn in design px (1920x1080).

private val Stripe = Color(0xFFC9483F)
private val BucketWhite = Color(0xFFFBF6EC)
private val BucketEdge = Color(0xFFD9CDBB)
private val Corn = Color(0xFFFFF4DC)
private val CornShade = Color(0xFFF3DFB0)
private val Butter = Color(0xFFF2C14E)
private val Ticket = Color(0xFFF2C14E)
private val TicketEdge = Color(0xFFC9A23A)
private val Stake = Color(0xFF9C7A55)

private val TicketText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0x7A, 0x4E, 0x1E)
    textSize = 15f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.12f
}

private const val KernelCount = 8

fun DrawScope.drawPopcornDayFigures(unit: Float, seconds: Float) {
    withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        drawTicket(560f, 866f, -8f)
        drawTicket(1380f, 866f, 7f)
        listOf(420f to 864f, 470f to 868f, 1240f to 866f, 1520f to 862f, 700f to 868f).forEachIndexed { i, (x, y) -> drawKernel(Offset(x, y), 9f, i * 50f) }
        drawBucket(760f, 866f, 74f, 84f)
        drawBucket(1170f, 866f, 66f, 76f)
        // The big bucket's kernels fly behind its heap, then the bucket over them.
        val mouth = Offset(960f, 866f - 150f)
        repeat(KernelCount) { i -> poppedKernel(seconds, i)?.let { drawKernel(Offset(mouth.x + it.dx, mouth.y - 18f + it.dy), 12f, it.spinDegrees) } }
        drawBucket(960f, 866f, 130f, 150f)
    }
}

// A puffy kernel: a few overlapping cream lumps with a buttery middle.
private fun DrawScope.drawKernel(center: Offset, size: Float, degrees: Float) {
    withTransform({ rotate(degrees, pivot = center) }) {
        drawCircle(CornShade, size * 0.62f, Offset(center.x + size * 0.18f, center.y + size * 0.2f))
        drawCircle(Corn, size * 0.6f, Offset(center.x - size * 0.3f, center.y))
        drawCircle(Corn, size * 0.55f, Offset(center.x + size * 0.28f, center.y - size * 0.18f))
        drawCircle(Corn, size * 0.5f, Offset(center.x, center.y + size * 0.3f))
        drawCircle(Butter, size * 0.18f, Offset(center.x + size * 0.05f, center.y + size * 0.05f), alpha = 0.8f)
    }
}

// A tapered red-and-white striped bucket with popcorn heaped over the rim.
private fun DrawScope.drawBucket(x: Float, baseY: Float, topWidth: Float, height: Float) {
    val bottomWidth = topWidth * 0.74f
    val top = baseY - height
    // The heap first, so the bucket's rim sits in front of its lower edge.
    val heap = topWidth * 0.13f
    val lumps = 9
    repeat(lumps) { i ->
        val along = i / (lumps - 1f)
        val dx = (along - 0.5f) * topWidth * 0.9f
        val rise = heap * (1.1f - kotlin.math.abs(along - 0.5f) * 1.4f)
        drawKernel(Offset(x + dx, top - rise), heap, i * 41f)
    }
    repeat(4) { i -> drawKernel(Offset(x + (i - 1.5f) * topWidth * 0.18f, top - heap * 1.9f), heap * 0.95f, i * 67f) }
    val body = Path().apply {
        moveTo(x - topWidth / 2, top)
        lineTo(x + topWidth / 2, top)
        lineTo(x + bottomWidth / 2, baseY)
        lineTo(x - bottomWidth / 2, baseY)
        close()
    }
    drawPath(body, BucketWhite)
    clipPath(body) {
        val stripes = 7
        repeat(stripes) { i ->
            if (i % 2 == 0) {
                val t0 = i / stripes.toFloat() - 0.5f
                val t1 = (i + 1) / stripes.toFloat() - 0.5f
                drawPath(Path().apply {
                    moveTo(x + t0 * topWidth, top); lineTo(x + t1 * topWidth, top)
                    lineTo(x + t1 * bottomWidth, baseY); lineTo(x + t0 * bottomWidth, baseY); close()
                }, Stripe)
            }
        }
    }
    drawRoundRect(BucketEdge, Offset(x - topWidth / 2 - 3f, top - 3f), Size(topWidth + 6f, 7f), CornerRadius(3f))
}

// An "ADMIT ONE" ticket on a little stake, tilted.
private fun DrawScope.drawTicket(x: Float, baseY: Float, tilt: Float) {
    val w = 110f
    val h = 50f
    val top = baseY - 34f - h
    withTransform({ rotate(tilt, pivot = Offset(x, baseY)) }) {
        drawLine(Stake, Offset(x, baseY), Offset(x, top + h), strokeWidth = 4f)
        drawRoundRect(Ticket, Offset(x - w / 2, top), Size(w, h), CornerRadius(5f))
        drawRoundRect(TicketEdge, Offset(x - w / 2 + 4f, top + 4f), Size(w - 8f, h - 8f), CornerRadius(3f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
        drawIntoCanvas { it.nativeCanvas.drawText("ADMIT ONE", x, top + h / 2 + 5f, TicketText) }
    }
}
