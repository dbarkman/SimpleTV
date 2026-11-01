package com.redmesastudios.simpletv

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// The Movies page: every movie out to watch at home in the US, streaming or to rent or buy,
// on any service. TMDB lists them and says where each one is; OMDb adds the Rotten
// Tomatoes, IMDb and Metacritic scores.

private const val Tag = "SimpleTV.Movies"

// minVotes keeps out titles too obscure for TMDB to know anything about; Top rated asks
// for more, so a handful of perfect scores can't top the list.
enum class MovieSort(val label: String, val minVotes: Int) {
    Newest("Newest", 10),
    Popular("Popular", 10),
    TopRated("Top rated", 300),
}

data class Genre(val id: Int, val name: String)

// A movie in the list, with the day it came out to watch at home.
data class HomeMovie(val title: Title, val homeDate: LocalDate?)

data class MoviePage(val movies: List<HomeMovie>, val pages: Int)

data class MovieFacts(
    // The year it first came out, anywhere.
    val year: String,
    // When it came out to stream, rent or buy in the US.
    val homeDate: LocalDate?,
    // Minutes.
    val runtime: Int?,
    val genres: List<String>,
    // The US rating, like "PG-13"; empty when unknown.
    val rated: String,
    val imdbId: String?,
    val offers: List<Offer>,
)

// Each score is null when the site has none yet.
data class MovieScores(val rottenTomatoes: Int?, val imdb: String?, val metacritic: Int?) {
    val line: String
        get() = listOfNotNull(
            rottenTomatoes?.let { "🍅 $it%" },
            imdb?.let { "IMDb $it" },
            metacritic?.let { "Metacritic $it" },
        ).joinToString("  ·  ")
}

data class MovieInfo(val facts: MovieFacts, val scores: MovieScores?)

// Hands out the list a page at a time, as the screen scrolls toward its end.
class MoviePager(private val sort: MovieSort, private val genre: Int?, private val today: LocalDate) {
    private var page = 0
    private var pages = 1
    val finished: Boolean get() = page >= pages

    suspend fun next(): List<HomeMovie> {
        if (finished) return emptyList()
        val result = Tmdb.homeMovies(sort, genre, page + 1, today)
        page += 1
        pages = result.pages
        return result.movies
    }
}

object Omdb {
    suspend fun scores(imdbId: String): MovieScores? {
        if (BuildConfig.OMDB_KEY.isEmpty()) return null
        val json = JSONObject(httpGet("https://www.omdbapi.com/?i=$imdbId&apikey=${BuildConfig.OMDB_KEY}"))
        // A daily limit or a bad ID comes back as Response "False"; that isn't kept, so it's
        // tried again later.
        check(json.text("Response") == "True") { "OMDb: ${json.text("Error")}" }
        return parseOmdbScores(json)
    }
}

fun parseOmdbScores(json: JSONObject): MovieScores {
    fun known(value: String) = value.trim().takeIf { it.isNotEmpty() && it != "N/A" }
    val tomatoes = json.optJSONArray("Ratings")?.objects().orEmpty()
        .firstOrNull { it.text("Source") == "Rotten Tomatoes" }
        ?.text("Value")?.removeSuffix("%")?.toIntOrNull()
    return MovieScores(
        rottenTomatoes = tomatoes,
        imdb = known(json.text("imdbRating")),
        metacritic = known(json.text("Metascore"))?.toIntOrNull(),
    )
}

// Movie details and scores, kept on disk: where a movie streams changes often, so that's
// looked up again after 3 days; scores settle, and OMDb allows 1,000 lookups a day, so
// they're kept 2 weeks. Movies nobody has looked at for a month are dropped.
object MovieInfoStore {
    private const val CacheFile = "movies.json"
    private val FactsFor = Duration.ofDays(3)
    private val ScoresFor = Duration.ofDays(14)
    private val ForgetAfter = Duration.ofDays(30)

    private class Entry(val facts: MovieFacts, val factsAt: Instant, val scores: MovieScores?, val scoresAt: Instant?)

    private val entries = HashMap<Int, Entry>()
    private val lock = Mutex()
    private val lookups = Semaphore(6)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loaded = false
    private var saving: Job? = null

    suspend fun get(context: Context, id: Int): MovieInfo? = lookups.withPermit {
        val now = Instant.now()
        val cached = lock.withLock {
            if (!loaded) load(context, now)
            entries[id]
        }
        val freshFacts = cached?.takeIf { Duration.between(it.factsAt, now) < FactsFor }
        val facts = freshFacts?.facts
            ?: runCatching { Tmdb.movieFacts(id) }.onFailure { Log.w(Tag, "No details for movie $id", it) }.getOrNull()
            ?: return@withPermit cached?.let { MovieInfo(it.facts, it.scores) }
        val scoresFresh = cached?.scoresAt?.let { Duration.between(it, now) < ScoresFor } == true
        val looked = if (scoresFresh || facts.imdbId == null) null else runCatching { Omdb.scores(facts.imdbId) }.getOrNull()
        val entry = Entry(
            facts = facts,
            factsAt = if (freshFacts != null) freshFacts.factsAt else now,
            scores = looked ?: cached?.scores,
            scoresAt = if (looked != null) now else cached?.scoresAt,
        )
        if (freshFacts == null || looked != null) {
            lock.withLock { entries[id] = entry }
            saveSoon(context)
        }
        MovieInfo(entry.facts, entry.scores)
    }

    // A screenful of lookups lands at once, so the file is written once they settle.
    private fun saveSoon(context: Context) {
        saving?.cancel()
        saving = scope.launch {
            delay(2_000)
            val text = lock.withLock { toJson().toString() }
            File(context.filesDir, CacheFile).writeText(text)
        }
    }

    private suspend fun load(context: Context, now: Instant) = withContext(Dispatchers.IO) {
        loaded = true
        val file = File(context.filesDir, CacheFile)
        if (!file.exists()) return@withContext
        runCatching {
            val json = JSONObject(file.readText())
            json.keys().forEach { key ->
                val entry = entryFromJson(json.getJSONObject(key))
                if (Duration.between(entry.factsAt, now) < ForgetAfter) entries[key.toInt()] = entry
            }
        }.onFailure { Log.w(Tag, "Couldn't read the movie cache", it) }
    }

    private fun toJson() = JSONObject().apply {
        entries.forEach { (id, entry) ->
            put(
                id.toString(),
                JSONObject()
                    .put("facts", factsToJson(entry.facts))
                    .put("factsAt", entry.factsAt.toString())
                    .put("scores", entry.scores?.let(::scoresToJson))
                    .put("scoresAt", entry.scoresAt?.toString()),
            )
        }
    }

    private fun entryFromJson(json: JSONObject) = Entry(
        facts = factsFromJson(json.getJSONObject("facts")),
        factsAt = Instant.parse(json.getString("factsAt")),
        scores = json.optJSONObject("scores")?.let(::scoresFromJson),
        scoresAt = json.text("scoresAt").ifEmpty { null }?.let(Instant::parse),
    )
}

fun factsToJson(facts: MovieFacts): JSONObject = JSONObject()
    .put("year", facts.year)
    .put("homeDate", facts.homeDate?.toString())
    .put("runtime", facts.runtime)
    .put("genres", JSONArray(facts.genres))
    .put("rated", facts.rated)
    .put("imdbId", facts.imdbId)
    .put("offers", JSONArray(facts.offers.map { JSONObject().put("id", it.providerId).put("name", it.providerName).put("rent", it.rentOrBuy) }))

fun factsFromJson(json: JSONObject) = MovieFacts(
    year = json.text("year"),
    homeDate = json.text("homeDate").ifEmpty { null }?.let(LocalDate::parse),
    runtime = json.optInt("runtime").takeIf { it > 0 },
    genres = json.optJSONArray("genres")?.let { array -> List(array.length()) { array.getString(it) } }.orEmpty(),
    rated = json.text("rated"),
    imdbId = json.text("imdbId").ifEmpty { null },
    offers = json.optJSONArray("offers")?.objects().orEmpty().map { Offer(it.getInt("id"), it.text("name"), it.optBoolean("rent")) },
)

private fun scoresToJson(scores: MovieScores) = JSONObject()
    .put("rottenTomatoes", scores.rottenTomatoes)
    .put("imdb", scores.imdb)
    .put("metacritic", scores.metacritic)

private fun scoresFromJson(json: JSONObject) = MovieScores(
    rottenTomatoes = json.optInt("rottenTomatoes", -1).takeIf { it >= 0 },
    imdb = json.text("imdb").ifEmpty { null },
    metacritic = json.optInt("metacritic", -1).takeIf { it >= 0 },
)

// How OK watches a movie on this TV.
sealed interface MovieWatch {
    // In one of your apps, as part of the service.
    data class Stream(val app: StreamingApp) : MovieWatch
    data class Rent(val app: StreamingApp) : MovieWatch
    // Streaming only on a service that isn't on this TV.
    data class Elsewhere(val service: String) : MovieWatch
    data object Nowhere : MovieWatch
}

// Your own services first. Otherwise Fandango at Home, which rents or sells nearly
// everything once it's out at home, even when the guide hasn't caught up yet.
fun movieWatch(offers: List<Offer>, installed: List<StreamingApp>): MovieWatch {
    val where = whereToWatch(offers, installed)
    val fandango = installed.firstOrNull { it.packageName == FandangoPackage }
    val rentable = offers.any { it.rentOrBuy }
    return when {
        where.here.isNotEmpty() -> MovieWatch.Stream(where.here.first())
        fandango != null && (rentable || offers.isEmpty()) -> MovieWatch.Rent(fandango)
        where.rentHere.isNotEmpty() -> MovieWatch.Rent(where.rentHere.first())
        where.elsewhere.isNotEmpty() -> MovieWatch.Elsewhere(where.elsewhere.first())
        else -> MovieWatch.Nowhere
    }
}

// The hero's last line, after the OK badge.
fun MovieWatch.sentence(): String = when (this) {
    is MovieWatch.Stream -> "Watch on ${app.label}"
    is MovieWatch.Rent -> "Rent or buy on ${app.label}"
    is MovieWatch.Elsewhere -> "Streams on $service, which isn't on this TV"
    MovieWatch.Nowhere -> "Not out to watch at home yet"
}

private fun MovieWatch.short(): String = when (this) {
    is MovieWatch.Stream -> app.label
    is MovieWatch.Rent -> "Rent"
    is MovieWatch.Elsewhere -> "Not on this TV"
    MovieWatch.Nowhere -> ""
}

private val CardDate = DateTimeFormatter.ofPattern("MMM d")

// The line under a movie's card: when it came out at home (the year for older ones), its
// Rotten Tomatoes score and where it is, as each becomes known.
fun movieNote(movie: HomeMovie, info: MovieInfo?, watch: MovieWatch?, today: LocalDate): String {
    val date = movie.homeDate?.let { if (it.year == today.year) it.format(CardDate) else it.year.toString() }
    return listOfNotNull(
        date,
        info?.scores?.rottenTomatoes?.let { "🍅 $it%" },
        watch?.short()?.ifEmpty { null },
    ).joinToString(" · ")
}

// "2025 · 1h 52m · PG-13 · Drama, Thriller"
fun movieMeta(info: MovieInfo?, movie: HomeMovie): String {
    val facts = info?.facts ?: return movie.title.year
    return listOfNotNull(
        facts.year.ifEmpty { null },
        facts.runtime?.let { if (it >= 60) "${it / 60}h ${it % 60}m" else "${it}m" },
        facts.rated.ifEmpty { null },
        facts.genres.take(3).joinToString(", ").ifEmpty { null },
    ).joinToString(" · ")
}

// Fandango at Home's TV app takes no web links (and its search intent drops the query), so
// a rental opens the app itself, where new releases lead its home page.
suspend fun Context.watchMovie(watch: MovieWatch, title: Title) {
    when (watch) {
        is MovieWatch.Stream -> openTitle(watch.app, title)
        is MovieWatch.Rent -> if (watch.app.packageName == FandangoPackage) {
            WatchHistory.record(this, watch.app.packageName, title)
            NowPlaying.record(this, watch.app.packageName, title)
            launchApp(watch.app)
        } else {
            openTitle(watch.app, title)
        }
        else -> Unit
    }
}
