package com.redmesastudios.simpletv

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// What the TV button's panel says over a playing app. A show started from SimpleTV gets its
// details; anything else gets the app's name. Either way: the time and the weather.
data class PanelInfo(val heading: String, val meta: String, val overview: String, val time: String, val date: String, val weather: String)

// How long a show started from SimpleTV is assumed to still be the one playing in its app,
// as a backstop; leaving the app or pressing Back in it forgets the show sooner.
private val StillWatching = Duration.ofHours(4)

fun panelInfo(
    foreground: String,
    appLabel: String,
    started: NowPlaying.Started?,
    weather: WeatherNow?,
    now: LocalDateTime,
    nowInstant: Instant,
): PanelInfo {
    val title = started?.takeIf { it.packageName == foreground && Duration.between(it.at, nowInstant) < StillWatching }?.title
    return PanelInfo(
        heading = title?.name ?: "Watching $appLabel",
        meta = title?.let(::titleMeta) ?: "",
        overview = title?.overview.orEmpty(),
        time = now.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)),
        date = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)),
        weather = weather?.let { "${it.temperature}° · ${it.summary}" } ?: "",
    )
}

// "2015 · Movie · ★ 6.8", "2019 · 4 seasons · ★ 8.1", or a video's channel and age.
fun titleMeta(title: Title): String = when (title.kind) {
    TitleKind.Video -> title.note
    else -> listOfNotNull(
        title.year.ifEmpty { null },
        if (title.kind == TitleKind.Movie) "Movie" else title.seasons?.let { if (it == 1) "1 season" else "$it seasons" },
        title.rating.takeIf { it > 0 }?.let { "★ " + String.format(Locale.US, "%.1f", it) },
    ).joinToString(" · ")
}

// The panel itself: a wide card along the bottom of the screen, in the fall palette.
fun infoPanelView(context: Context, info: PanelInfo): View {
    fun dp(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics)
    val font = ResourcesCompat.getFont(context, R.font.manrope)
    fun text(value: String, sizeSp: Float, weight: Int, color: Int, lines: Int = 1) = TextView(context).apply {
        text = value
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        typeface = Typeface.create(font, weight, false)
        maxLines = lines
        ellipsize = android.text.TextUtils.TruncateAt.END
        visibility = if (value.isEmpty()) View.GONE else View.VISIBLE
    }
    val details = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(info.heading, 24f, 800, PanelInk, lines = 2))
        addView(text(info.meta, 13f, 600, PanelInkSoft).apply { setPadding(0, dp(4f).toInt(), 0, 0) })
        addView(text(info.overview, 13.5f, 500, PanelInk, lines = 3).apply { setPadding(0, dp(8f).toInt(), 0, 0) })
    }
    val clock = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.END
        addView(text(info.time, 30f, 600, PanelInk).apply { gravity = Gravity.END })
        addView(text(info.date, 13f, 600, PanelInkSoft).apply { gravity = Gravity.END })
        addView(text(info.weather, 14f, 700, PanelInk).apply { gravity = Gravity.END; setPadding(0, dp(10f).toInt(), 0, 0) })
    }
    return LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(28f).toInt(), dp(20f).toInt(), dp(28f).toInt(), dp(20f).toInt())
        background = GradientDrawable().apply {
            setColor(PanelFill)
            cornerRadius = dp(16f)
            setStroke(dp(1f).toInt(), PanelEdge)
        }
        elevation = dp(12f)
        addView(details, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(clock, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            marginStart = dp(32f).toInt()
        })
    }
}

// The fall palette (ui/theme/Color.kt), as plain colors for a View.
private const val PanelFill = 0xF2F4ECE1.toInt()
private const val PanelEdge = 0xFF9C8C7E.toInt()
private const val PanelInk = 0xFF2A211B.toInt()
private const val PanelInkSoft = 0xFF6B5D52.toInt()
