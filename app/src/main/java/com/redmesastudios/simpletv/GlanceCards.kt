package com.redmesastudios.simpletv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import com.redmesastudios.simpletv.ui.theme.Ink
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redmesastudios.simpletv.ui.theme.SimpleTVType
import kotlin.math.cos
import kotlin.math.sin

private val CardFill = Color(0xFFFBF6EF)
private val CardEdge = Color(0xFFE2D2BE)
private val IconInk = Color(0xFF8A5A3B)
private val CardShape = RoundedCornerShape(10.dp)
private val LabelStyle = SimpleTVType.hint.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
private val MainStyle = SimpleTVType.serviceName.copy(fontSize = 14.sp, lineHeight = 17.sp)

// At a glance: weather, news and markets along the bottom of the home screen. Each
// card fades to its new reading when it changes, and opens its channel page.
@Composable
fun GlanceCards(glance: Glance, focus: Map<Channel, FocusRequester>, onOpen: (Channel) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val weather = glance.weather
            GlanceCard(
                focus = focus.getValue(Channel.Weather),
                onOpen = { onOpen(Channel.Weather) },
                label = weather?.let { "Weather · ${it.place}" } ?: "Weather",
                main = weather?.let { "${it.temperature}° · ${it.summary}" } ?: if (Home.hasWeather) "Checking the forecast…" else "Set your location to see the weather",
                icon = { if (weather != null && isClear(weather.summary)) drawSun() else drawCloud() },
            )
            GlanceCard(
                focus = focus.getValue(Channel.News),
                onOpen = { onOpen(Channel.News) },
                label = "News",
                main = glance.news ?: if (Home.hasNews) "Checking the news…" else "Add a SignalPost key to see the news",
                icon = { drawNewspaper() },
            )
            GlanceCard(
                focus = focus.getValue(Channel.Markets),
                onOpen = { onOpen(Channel.Markets) },
                label = "Markets",
                main = glance.markets.takeIf { it.isNotEmpty() }
                    ?.joinToString(" · ") { "${it.name}\u00A0${percentText(it.percent)}" }
                    ?: "Checking the markets…",
                icon = { drawTrend() },
            )
        }
    }
}

private fun isClear(summary: String) = listOf("Sunny", "Clear").any { summary.contains(it, ignoreCase = true) }

@Composable
private fun RowScope.GlanceCard(
    focus: FocusRequester,
    onOpen: () -> Unit,
    label: String,
    main: String,
    icon: DrawScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, tween(200), label = "glanceScale")
    val edge by animateColorAsState(if (focused) Ink else CardEdge, tween(200), label = "glanceEdge")
    Row(
        Modifier
            .weight(1f)
            .height(66.dp)
            .focusRequester(focus)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(CardFill, CardShape)
            .border(if (focused) 2.dp else 1.dp, edge, CardShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(28.dp)) { icon() }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(label, style = LabelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            AnimatedContent(
                targetState = main,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "glance",
            ) { text ->
                BasicText(text, style = MainStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// Icons drawn on a 24-unit grid, in the cards' warm brown line.
internal fun DrawScope.line(path: Path) {
    val unit = size.width / 24f
    drawPath(path, IconInk, style = Stroke(width = 1.6f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

internal fun DrawScope.scaled(build: Path.(Float) -> Unit) = Path().apply { build(size.width / 24f) }

internal fun DrawScope.drawCloud() = line(scaled { u ->
    moveTo(7f * u, 17f * u)
    cubicTo(4.2f * u, 17f * u, 2.5f * u, 14.6f * u, 3.4f * u, 12.2f * u)
    cubicTo(4f * u, 10.6f * u, 5.3f * u, 9.4f * u, 6.6f * u, 9.1f * u)
    cubicTo(7.4f * u, 5.6f * u, 11.6f * u, 4f * u, 14.6f * u, 5.7f * u)
    cubicTo(16.2f * u, 6.6f * u, 17.1f * u, 7.9f * u, 17.4f * u, 9.6f * u)
    cubicTo(20.2f * u, 9.8f * u, 21.8f * u, 12.8f * u, 20.6f * u, 15f * u)
    cubicTo(19.9f * u, 16.3f * u, 18.8f * u, 17f * u, 17.5f * u, 17f * u)
    close()
})

internal fun DrawScope.drawSun() {
    val unit = size.width / 24f
    val center = Offset(12f * unit, 12f * unit)
    drawCircle(IconInk, 4.2f * unit, center, style = Stroke(1.6f * unit))
    line(scaled { u ->
        repeat(8) { i ->
            val angle = i * Math.PI / 4
            moveTo((12 + 7 * cos(angle)).toFloat() * u, (12 + 7 * sin(angle)).toFloat() * u)
            lineTo((12 + 9.5 * cos(angle)).toFloat() * u, (12 + 9.5 * sin(angle)).toFloat() * u)
        }
    })
}

private fun DrawScope.drawNewspaper() {
    val unit = size.width / 24f
    drawRoundRect(IconInk, Offset(3f * unit, 5f * unit), Size(18f * unit, 14f * unit), CornerRadius(2f * unit), style = Stroke(1.6f * unit))
    line(scaled { u ->
        moveTo(7f * u, 9f * u); lineTo(13f * u, 9f * u)
        moveTo(7f * u, 12f * u); lineTo(17f * u, 12f * u)
        moveTo(7f * u, 15f * u); lineTo(17f * u, 15f * u)
    })
}

private fun DrawScope.drawTrend() = line(scaled { u ->
    moveTo(3f * u, 17f * u); lineTo(8f * u, 12f * u); lineTo(12f * u, 16f * u); lineTo(20f * u, 8f * u)
    moveTo(15f * u, 8f * u); lineTo(20f * u, 8f * u); lineTo(20f * u, 13f * u)
})
