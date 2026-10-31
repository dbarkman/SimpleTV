package com.redmesastudios.simpletv

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import android.content.Context
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

// What the channel pages show: a fuller look at the weather, news and markets than the
// home screen's cards.

enum class Channel { Weather, News, Markets }

// The kinds of sky the pages draw an icon for.
enum class Sky { Sun, PartlyCloudy, Cloud, Rain, Storm }

fun skyFor(summary: String): Sky {
    val text = summary.lowercase(Locale.US)
    return when {
        "thunder" in text || "storm" in text -> Sky.Storm
        "rain" in text || "shower" in text || "drizzle" in text -> Sky.Rain
        "partly" in text || "mostly sunny" in text || "mostly clear" in text -> Sky.PartlyCloudy
        "sunny" in text || "clear" in text -> Sky.Sun
        else -> Sky.Cloud
    }
}

data class DayForecast(val name: String, val high: Int, val low: Int?, val summary: String, val rain: Int)

data class WeatherReport(
    val place: String,
    val temperature: Int,
    val summary: String,
    // "82% chance of rain this afternoon"
    val rainLine: String?,
    // "Tonight 62° · Mostly clear"
    val nextLine: String?,
    val days: List<DayForecast>,
    val alerts: List<String>,
)

data class NewsStory(val title: String, val summary: String, val newsrooms: Int, val lastSeen: Instant, val world: Boolean)

data class IndexDay(
    val name: String,
    // As Yahoo takes it in a URL, e.g. %5EGSPC.
    val symbol: String,
    val price: Double,
    val previousClose: Double,
    // Today's prices, oldest first, for the little chart.
    val prices: List<Double>,
    val asOf: Instant,
    val open: Boolean,
) {
    val change: Double get() = price - previousClose
    val percent: Double get() = change / previousClose * 100
}

// A fuller look at one index or stock, for its detail page.
data class MarketDetail(
    val day: IndexDay,
    val exchange: String,
    val currency: String,
    val dayLow: Double?,
    val dayHigh: Double?,
    val openPrice: Double?,
    val volume: Long?,
    val yearLow: Double?,
    val yearHigh: Double?,
    // ("1 week", +1.2) and so on; null where there isn't enough history.
    val performance: List<Pair<String, Double?>>,
    // A year of daily closes, oldest first, for the big chart.
    val yearPrices: List<Double>,
)

// Change from the last close on or before each look-back date to today's price.
fun performance(times: List<Instant>, closes: List<Double>, price: Double, now: Instant, zone: ZoneId): List<Pair<String, Double?>> {
    val today = now.atZone(zone).toLocalDate()
    fun since(date: java.time.LocalDate): Double? {
        val cutoff = date.plusDays(1).atStartOfDay(zone).toInstant()
        // History that starts within a few days after the date (a weekend, a holiday) still counts.
        val i = times.indexOfLast { it < cutoff }.takeIf { it >= 0 }
            ?: 0.takeIf { times.isNotEmpty() && times.first() < date.plusDays(4).atStartOfDay(zone).toInstant() }
            ?: return null
        return (price - closes[i]) / closes[i] * 100
    }
    return listOf(
        "1 week" to since(today.minusWeeks(1)),
        "1 month" to since(today.minusMonths(1)),
        "3 months" to since(today.minusMonths(3)),
        "Year to date" to since(today.withDayOfYear(1).minusDays(1)),
        "1 year" to since(today.minusYears(1)),
    )
}

// Big counts in words: 67,842,804 -> "67.8 million".
fun bigNumber(value: Long): String = when {
    value >= 1_000_000_000 -> String.format(Locale.US, "%.2f billion", value / 1e9)
    value >= 1_000_000 -> String.format(Locale.US, "%.1f million", value / 1e6)
    else -> String.format(Locale.US, "%,d", value)
}

// One NWS forecast period: a 12-hour half, day or night.
data class NwsPeriod(val start: String, val daytime: Boolean, val temperature: Int, val summary: String, val rain: Int)

// Periods folded into days, each daytime high paired with the night after it.
fun foldDays(periods: List<NwsPeriod>, zone: ZoneId): List<DayForecast> {
    val days = mutableListOf<DayForecast>()
    periods.forEachIndexed { i, period ->
        if (!period.daytime) return@forEachIndexed
        val night = periods.getOrNull(i + 1)?.takeIf { !it.daytime }
        days += DayForecast(
            name = ZonedDateTime.parse(period.start).withZoneSameInstant(zone).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US),
            high = period.temperature,
            low = night?.temperature,
            summary = period.summary,
            rain = period.rain,
        )
    }
    return days
}

private fun nwsPeriod(json: JSONObject) = NwsPeriod(
    start = json.getString("startTime"),
    daytime = json.getBoolean("isDaytime"),
    temperature = json.getInt("temperature"),
    summary = json.getString("shortForecast"),
    rain = rainChance(json),
)

// Ticker symbols as typed on the remote's keyboard: "aapl, msft tsla" -> AAPL, MSFT, TSLA.
fun parseSymbols(text: String): List<String> = text.uppercase(Locale.US)
    .split(Regex("[\\s,;]+"))
    .filter { it.matches(Regex("[A-Z0-9.^=\\-]{1,12}")) }
    .distinct()

// The family's stock list, kept on the box.
object WatchList {
    private const val Prefs = "markets"
    private const val Key = "symbols"

    fun symbols(context: Context): List<String> =
        parseSymbols(context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString(Key, "").orEmpty())

    fun save(context: Context, symbols: List<String>) {
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().putString(Key, symbols.joinToString(" ")).apply()
    }
}

// NWS period names as they read mid-sentence: "this afternoon", "tonight", "on Saturday".
fun whenSaid(periodName: String): String {
    val lower = periodName.lowercase(Locale.US)
    return if (lower.startsWith("this") || lower.startsWith("to")) lower else "on $periodName"
}

private fun rainChance(period: JSONObject): Int =
    period.optJSONObject("probabilityOfPrecipitation")?.optInt("value", 0) ?: 0

object Channels {
    private val weatherHeaders = mapOf("User-Agent" to "SimpleTV (redmesastudios.com)", "Accept" to "application/geo+json")

    suspend fun weather(zone: ZoneId = ZoneId.systemDefault()): WeatherReport = coroutineScope {
        val point = JSONObject(httpGet("https://api.weather.gov/points/${Home.weatherPoint}", weatherHeaders)).getJSONObject("properties")
        val place = point.getJSONObject("relativeLocation").getJSONObject("properties").getString("city")
        val hourly = async { JSONObject(httpGet(point.getString("forecastHourly"), weatherHeaders)) }
        val daily = async { JSONObject(httpGet(point.getString("forecast"), weatherHeaders)) }
        val alerts = async {
            runCatching {
                JSONObject(httpGet("https://api.weather.gov/alerts/active?point=${Home.weatherPoint}", weatherHeaders))
                    .getJSONArray("features").objects().map { it.getJSONObject("properties").text("headline").ifEmpty { it.getJSONObject("properties").text("event") } }
            }.getOrDefault(emptyList())
        }
        val now = hourly.await().getJSONObject("properties").getJSONArray("periods").getJSONObject(0)
        val periods = daily.await().getJSONObject("properties").getJSONArray("periods").objects()
        val first = periods.first()
        val next = periods.getOrNull(1)
        val rain = rainChance(first)
        WeatherReport(
            place = place,
            temperature = now.getInt("temperature"),
            summary = now.getString("shortForecast"),
            rainLine = if (rain >= 10) "$rain% chance of rain ${whenSaid(first.getString("name"))}" else null,
            nextLine = next?.let { "${it.getString("name")} ${it.getInt("temperature")}° · ${it.getString("shortForecast")}" },
            days = foldDays(periods.map(::nwsPeriod), zone).drop(if (first.getBoolean("isDaytime")) 1 else 0).take(6),
            alerts = alerts.await(),
        )
    }

    // The day's biggest stories, by how many newsrooms cover them.
    suspend fun news(): List<NewsStory> = stories("window_hours=24&per_page=60")

    // Local stories over three days, since local news moves slower.
    suspend fun localNews(): List<NewsStory> = stories("window_hours=72&per_page=30&market=${Home.localNewsMarket}")

    private suspend fun stories(query: String): List<NewsStory> = JSONObject(httpGet(
        "https://signalpost.redmesastudios.com/api/v1/stories?$query",
        mapOf("Authorization" to "Bearer ${BuildConfig.SIGNALPOST_KEY}", "Accept" to "application/json"),
    )).getJSONArray("results").objects().map {
        NewsStory(
            title = it.text("title"),
            summary = it.text("summary"),
            newsrooms = it.optInt("outlets_in_window"),
            lastSeen = Instant.parse(it.getString("last_seen")),
            world = it.text("topic") == "World",
        )
    }

    suspend fun markets(): List<IndexDay> = coroutineScope {
        listOf("S&P 500" to "%5EGSPC", "Dow Jones" to "%5EDJI", "Nasdaq" to "%5EIXIC").map { (name, symbol) ->
            async { index(name, symbol) }
        }.map { it.await() }
    }

    // The family's own ticker symbols, in the order they were typed. A symbol Yahoo
    // doesn't know comes back null.
    suspend fun quotes(symbols: List<String>): List<IndexDay?> = coroutineScope {
        symbols.map { symbol ->
            async { runCatching { index(null, URLEncoder.encode(symbol, "UTF-8")) }.getOrNull() }
        }.map { it.await() }
    }

    suspend fun detail(day: IndexDay): MarketDetail {
        val result = JSONObject(httpGet(
            "https://query1.finance.yahoo.com/v8/finance/chart/${day.symbol}?range=1y&interval=1d",
            mapOf("User-Agent" to "Mozilla/5.0"),
        )).getJSONObject("chart").getJSONArray("result").getJSONObject(0)
        val meta = result.getJSONObject("meta")
        val stamps = result.optJSONArray("timestamp")
        val quote = result.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0)
        val closeArray = quote?.optJSONArray("close")
        val bars = List(stamps?.length() ?: 0) { i -> Instant.ofEpochSecond(stamps!!.getLong(i)) to (closeArray?.optDouble(i) ?: Double.NaN) }
            .filterNot { it.second.isNaN() }
        val opens = quote?.optJSONArray("open")
        val lastOpen = opens?.let { array -> (array.length() - 1 downTo 0).map { array.optDouble(it) }.firstOrNull { !it.isNaN() } }
        fun number(name: String) = meta.optDouble(name).takeIf { !it.isNaN() }
        return MarketDetail(
            day = day,
            exchange = meta.text("fullExchangeName").ifEmpty { meta.text("exchangeName") },
            currency = meta.text("currency"),
            dayLow = number("regularMarketDayLow"),
            dayHigh = number("regularMarketDayHigh"),
            openPrice = lastOpen,
            volume = meta.optLong("regularMarketVolume").takeIf { it > 0 },
            yearLow = number("fiftyTwoWeekLow"),
            yearHigh = number("fiftyTwoWeekHigh"),
            performance = performance(bars.map { it.first }, bars.map { it.second }, day.price, Instant.now(), ZoneId.of("America/New_York")),
            yearPrices = bars.map { it.second },
        )
    }

    private suspend fun index(name: String?, symbol: String): IndexDay {
        val result = JSONObject(httpGet(
            "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=1d&interval=5m",
            mapOf("User-Agent" to "Mozilla/5.0"),
        )).getJSONObject("chart").getJSONArray("result").getJSONObject(0)
        val meta = result.getJSONObject("meta")
        val closes = result.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0)?.optJSONArray("close")
        val prices = closes?.let { array -> List(array.length()) { array.optDouble(it) }.filterNot { it.isNaN() } }.orEmpty()
        val regular = meta.optJSONObject("currentTradingPeriod")?.optJSONObject("regular")
        val now = Instant.now().epochSecond
        return IndexDay(
            name = name ?: listOf(meta.text("symbol"), meta.text("shortName")).filter { it.isNotEmpty() }.joinToString(" · "),
            symbol = symbol,
            price = meta.getDouble("regularMarketPrice"),
            previousClose = meta.getDouble("chartPreviousClose"),
            prices = prices,
            asOf = Instant.ofEpochSecond(meta.getLong("regularMarketTime")),
            open = regular != null && now in regular.getLong("start")..regular.getLong("end"),
        )
    }
}
