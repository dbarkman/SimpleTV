package com.redmesastudios.simpletv

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val Tag = "SimpleTV.Glance"
private val RefreshEvery = Duration.ofMinutes(15)

// The household's own settings, baked in from ~/.config/simpletv/home.txt at build time
// (see README). Anything left blank turns its feature off with a note on screen.
object Home {
    // "latitude,longitude" for the National Weather Service (US only).
    val weatherPoint: String = BuildConfig.WEATHER_POINT
    // A SignalPost market id for the local news column, and the name to show for it.
    val localNewsMarket: String = BuildConfig.LOCAL_NEWS_MARKET
    val localNewsName: String = BuildConfig.LOCAL_NEWS_NAME.ifEmpty { "Local" }
    val hasWeather: Boolean get() = weatherPoint.isNotEmpty()
    val hasNews: Boolean get() = BuildConfig.SIGNALPOST_KEY.isNotEmpty()
    val hasLocalNews: Boolean get() = hasNews && localNewsMarket.isNotEmpty()
}

data class WeatherNow(val place: String, val temperature: Int, val unit: String, val summary: String)

data class IndexMove(val name: String, val percent: Double)

data class Glance(val weather: WeatherNow? = null, val news: String? = null, val markets: List<IndexMove> = emptyList())

// "3 new stories in the last 6 hours"; in the morning, what came in overnight.
fun newsLine(firstSeen: List<Instant>, now: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): String {
    val morning = now.hour in 5..9
    val since = if (morning) now.toLocalDate().minusDays(1).atTime(22, 0) else now.minusHours(6)
    val sinceInstant = since.atZone(zone).toInstant()
    val count = firstSeen.count { it > sinceInstant }
    val stories = if (count == 1) "1 new story" else "$count new stories"
    return when {
        count == 0 && morning -> "Nothing new overnight"
        count == 0 -> "No new stories in the last 6 hours"
        morning -> "$stories overnight"
        else -> "$stories in the last 6 hours"
    }
}

// "+0.4%" / "−1.2%" with a true minus sign, one decimal.
fun percentText(percent: Double): String {
    val rounded = (percent * 10).roundToInt() / 10.0
    val sign = if (rounded < 0) "−" else "+"
    return sign + String.format(Locale.US, "%.1f%%", abs(rounded))
}

// Weather, news and markets for the home screen's At a glance cards, refreshed at most
// every 15 minutes while the home screen is up. A source that fails keeps its last value.
object GlanceStore {
    private val state = MutableStateFlow(Glance())
    val glance: StateFlow<Glance> = state.asStateFlow()
    private val refreshing = Mutex()
    private var fetchedAt: Instant? = null

    suspend fun refreshIfStale() = refreshing.withLock {
        if (fetchedAt?.let { Duration.between(it, Instant.now()) < RefreshEvery } == true) return@withLock
        coroutineScope {
            val weather = async { if (Home.hasWeather) attempt("weather") { weather() } else null }
            val news = async { if (Home.hasNews) attempt("news") { news() } else null }
            val markets = async { attempt("markets") { markets() } }
            val previous = state.value
            state.value = Glance(
                weather = weather.await() ?: previous.weather,
                news = news.await() ?: previous.news,
                markets = markets.await() ?: previous.markets,
            )
        }
        fetchedAt = Instant.now()
    }

    private suspend fun <T> attempt(what: String, fetch: suspend () -> T): T? =
        runCatching { fetch() }.onFailure { Log.w(Tag, "Couldn't refresh $what", it) }.getOrNull()

    private val weatherHeaders = mapOf("User-Agent" to "SimpleTV (redmesastudios.com)", "Accept" to "application/geo+json")

    private suspend fun weather(): WeatherNow {
        val point = JSONObject(httpGet("https://api.weather.gov/points/${Home.weatherPoint}", weatherHeaders)).getJSONObject("properties")
        val place = point.getJSONObject("relativeLocation").getJSONObject("properties").getString("city")
        val hour = JSONObject(httpGet(point.getString("forecastHourly"), weatherHeaders))
            .getJSONObject("properties").getJSONArray("periods").getJSONObject(0)
        return WeatherNow(place, hour.getInt("temperature"), hour.getString("temperatureUnit"), hour.getString("shortForecast"))
    }

    private suspend fun news(): String {
        val reply = JSONObject(httpGet(
            "https://signalpost.redmesastudios.com/api/v1/stories?window_hours=24&per_page=1000",
            mapOf("Authorization" to "Bearer ${BuildConfig.SIGNALPOST_KEY}", "Accept" to "application/json"),
        ))
        val firstSeen = reply.getJSONArray("results").objects().map { Instant.parse(it.getString("first_seen")) }
        return newsLine(firstSeen, LocalDateTime.now())
    }

    private suspend fun markets(): List<IndexMove> = listOf("S&P" to "%5EGSPC", "Dow" to "%5EDJI", "Nasdaq" to "%5EIXIC").map { (name, symbol) ->
        val meta = JSONObject(httpGet(
            "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=1d&interval=1d",
            mapOf("User-Agent" to "Mozilla/5.0"),
        )).getJSONObject("chart").getJSONArray("result").getJSONObject(0).getJSONObject("meta")
        val price = meta.getDouble("regularMarketPrice")
        val previous = meta.getDouble("chartPreviousClose")
        IndexMove(name, (price - previous) / previous * 100)
    }
}
