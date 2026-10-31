package com.redmesastudios.simpletv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// The show guides behind the folders: TMDB (titles and artwork), Watchmode (what's new
// on each service, and links into the apps) and Netflix's own Top 10 pages.

// Rate limits and server hiccups are retried with backoff before giving up.
suspend fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
    var wait = 1_000L
    repeat(4) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        try {
            val code = connection.responseCode
            if (code in 200..299) return@withContext connection.inputStream.bufferedReader().use { it.readText() }
            if (code != 429 && code < 500) throw IOException("HTTP $code from ${url.substringBefore('?')}")
        } finally {
            connection.disconnect()
        }
        delay(wait)
        wait *= 2
    }
    throw IOException("Gave up on ${url.substringBefore('?')} after retries")
}

internal fun JSONObject.text(name: String): String = if (isNull(name)) "" else optString(name)

internal fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

fun tmdbImage(path: String, size: String) = "https://image.tmdb.org/t/p/$size$path"

object Tmdb {
    private const val Api = "https://api.themoviedb.org/3"

    private suspend fun get(path: String): JSONObject = JSONObject(
        httpGet(Api + path, mapOf("Authorization" to "Bearer ${BuildConfig.TMDB_TOKEN}", "Accept" to "application/json")),
    )

    private fun TitleKind.path() = if (this == TitleKind.Show) "tv" else "movie"

    suspend fun details(kind: TitleKind, id: Int): Title? = title(get("/${kind.path()}/$id?language=en-US"), kind)

    // Every US streaming service TMDB tracks, for shows and movies.
    suspend fun services(): List<GuideService> = listOf("tv", "movie").flatMap { kind ->
        get("/watch/providers/$kind?watch_region=US").getJSONArray("results").objects()
            .map { GuideService(it.getInt("provider_id"), it.getString("provider_name")) }
    }.distinctBy { it.id }

    // What's popular on a service right now; enough votes to skip obscure titles.
    suspend fun popular(kind: TitleKind, providers: Set<Int>): List<Title> =
        get("/discover/${kind.path()}?watch_region=US&with_watch_providers=${providers.joinToString("|")}&sort_by=popularity.desc&vote_count.gte=100&language=en-US")
            .getJSONArray("results").objects().mapNotNull { title(it, kind) }

    suspend fun find(name: String, kind: TitleKind): Title? =
        get("/search/${kind.path()}?query=${URLEncoder.encode(name, "UTF-8")}&language=en-US")
            .getJSONArray("results").objects().firstNotNullOfOrNull { title(it, kind) }

    // Shows, movies and people matching what was typed or said, most relevant first.
    suspend fun search(query: String): SearchResults {
        val results = get("/search/multi?query=${URLEncoder.encode(query, "UTF-8")}&language=en-US&include_adult=false")
            .getJSONArray("results").objects()
        return SearchResults(
            shows = results.filter { it.text("media_type") == "tv" }.mapNotNull { title(it, TitleKind.Show, requireArt = false) },
            movies = results.filter { it.text("media_type") == "movie" }.mapNotNull { title(it, TitleKind.Movie, requireArt = false) },
            people = results.filter { it.text("media_type") == "person" }.map { person(it) },
        )
    }

    // Everything someone acted in or made, best known first.
    suspend fun credits(person: Person): List<Title> {
        val json = get("/person/${person.id}/combined_credits?language=en-US")
        return (json.getJSONArray("cast").objects() + json.getJSONArray("crew").objects())
            .mapNotNull { credit ->
                when (credit.text("media_type")) {
                    "tv" -> title(credit, TitleKind.Show)
                    "movie" -> title(credit, TitleKind.Movie)
                    else -> null
                }
            }
            .distinctBy { it.kind to it.tmdbId }
            .sortedByDescending { it.popularity }
    }

    // Where a title streams, rents or sells in the US.
    suspend fun offers(title: Title): List<Offer> {
        val us = get("/${title.kind.path()}/${title.tmdbId}/watch/providers").getJSONObject("results").optJSONObject("US")
            ?: return emptyList()
        fun list(key: String, rentOrBuy: Boolean) = us.optJSONArray(key)?.objects().orEmpty()
            .map { Offer(it.getInt("provider_id"), it.text("provider_name"), rentOrBuy) }
        return (list("flatrate", false) + list("ads", false) + list("free", false) + list("rent", true) + list("buy", true))
            .distinctBy { it.providerId to it.rentOrBuy }
    }

    private fun person(json: JSONObject) = Person(
        id = json.getInt("id"),
        name = json.text("name"),
        photoPath = json.text("profile_path").ifEmpty { null },
        knownFor = json.text("known_for_department"),
    )

    // Folder rows leave out titles without artwork (a card needs a picture); search keeps
    // them, since someone may be looking for exactly that title.
    private fun title(json: JSONObject, kind: TitleKind, requireArt: Boolean = true): Title? {
        val backdrop = json.text("backdrop_path").ifEmpty { null }
        if (backdrop == null && requireArt) return null
        return Title(
            tmdbId = json.getInt("id"),
            kind = kind,
            name = json.text(if (kind == TitleKind.Show) "name" else "title"),
            overview = json.text("overview"),
            year = json.text(if (kind == TitleKind.Show) "first_air_date" else "release_date").take(4),
            backdropPath = backdrop,
            seasons = json.optInt("number_of_seasons").takeIf { it > 0 },
            popularity = json.optDouble("popularity", 0.0),
            rating = json.optDouble("vote_average", 0.0),
        )
    }
}

object Watchmode {
    private const val Api = "https://api.watchmode.com/v1"
    private val Day = DateTimeFormatter.BASIC_ISO_DATE

    private suspend fun get(path: String): String =
        httpGet("$Api$path${if ('?' in path) '&' else '?'}apiKey=${BuildConfig.WATCHMODE_KEY}")

    // Every service's releases between two dates. The feed ignores service filters and
    // returns at most 250 entries, so callers ask for short spans and split by service.
    suspend fun releases(start: LocalDate, end: LocalDate): List<Release> =
        JSONObject(get("/releases/?start_date=${start.format(Day)}&end_date=${end.format(Day)}&limit=250"))
            .getJSONArray("releases").objects().mapNotNull { json ->
                val tmdbId = json.optInt("tmdb_id").takeIf { it > 0 } ?: return@mapNotNull null
                Release(
                    sourceId = json.getInt("source_id"),
                    tmdbId = tmdbId,
                    kind = if (json.text("tmdb_type") == "tv") TitleKind.Show else TitleKind.Movie,
                    season = json.optInt("season_number").takeIf { it > 0 },
                    date = LocalDate.parse(json.text("source_release_date")),
                )
            }

    // Every US service Watchmode tracks, except stores that only rent and sell.
    suspend fun services(): List<GuideService> =
        JSONArray(get("/sources/?regions=US")).objects()
            .filter { it.text("type") != "purchase" }
            .map { GuideService(it.getInt("id"), it.text("name")) }

    // The service's own web link for a title, which its TV app opens; null when the
    // service doesn't carry it.
    suspend fun webLink(title: Title, sources: Set<Int>): String? {
        val field = if (title.kind == TitleKind.Show) "tmdb_tv_id" else "tmdb_movie_id"
        val match = JSONObject(get("/search/?search_field=$field&search_value=${title.tmdbId}"))
            .getJSONArray("title_results").objects().firstOrNull() ?: return null
        return JSONArray(get("/title/${match.getInt("id")}/sources/?regions=US")).objects()
            .firstOrNull { it.optInt("source_id") in sources && it.text("web_url").isNotEmpty() }
            ?.text("web_url")
    }
}

object NetflixTop10 {
    suspend fun shows(): List<NetflixRank> = parseNetflixTop10(page("https://www.netflix.com/tudum/top10/united-states/tv"))

    suspend fun movies(): List<NetflixRank> = parseNetflixTop10(page("https://www.netflix.com/tudum/top10/united-states"))

    private suspend fun page(url: String) = httpGet(url, mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) SimpleTV"))
}
