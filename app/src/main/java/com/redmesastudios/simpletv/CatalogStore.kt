package com.redmesastudios.simpletv

import android.content.Context
import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

private const val Tag = "SimpleTV"
private const val CacheFile = "catalog.json"
private const val RecentDays = 30L
private const val UpcomingDays = 60L
private const val SpanDays = 10L

// Folder rows for every service, rebuilt from the show guides once a day and kept on
// disk, so a folder opens instantly and still works if the guides can't be reached.
object CatalogStore {
    private val state = MutableStateFlow<Map<String, List<FolderRow>>>(emptyMap())
    val rows: StateFlow<Map<String, List<FolderRow>>> = state.asStateFlow()
    private val refreshing = Mutex()
    private var builtOn: LocalDate? = null

    suspend fun refreshIfStale(context: Context, apps: List<StreamingApp>, today: LocalDate = LocalDate.now()) = refreshing.withLock {
        val file = File(context.filesDir, CacheFile)
        if (builtOn == null && file.exists()) {
            runCatching { readCache(file.readText()) }.onSuccess { (day, rows) ->
                builtOn = day
                state.value = rows
            }
        }
        if (builtOn == today) return@withLock
        runCatching { build(apps.filter { it.hasGuide }, today) }
            .onSuccess { rows ->
                builtOn = today
                state.value = rows
                file.writeText(writeCache(today, rows))
            }
            .onFailure { Log.w(Tag, "Couldn't refresh the show guide", it) }
    }

    private suspend fun build(apps: List<StreamingApp>, today: LocalDate): Map<String, List<FolderRow>> = coroutineScope {
        val lookups = Semaphore(6)
        val known = mutableMapOf<Pair<TitleKind, Int>, Title?>()
        val knownLock = Mutex()
        suspend fun details(kind: TitleKind, id: Int): Title? {
            knownLock.withLock { if ((kind to id) in known) return known[kind to id] }
            val title = lookups.withPermit { runCatching { Tmdb.details(kind, id) }.getOrNull() }
            knownLock.withLock { known[kind to id] = title }
            return title
        }
        suspend fun enrich(releases: List<Release>, upcoming: Boolean, byPopularity: Boolean): List<Title> {
            val titles = releases.map { release ->
                async { details(release.kind, release.tmdbId)?.copy(note = releaseNote(release, upcoming)) }
            }.awaitAll().filterNotNull()
            return if (byPopularity) titles.sortedByDescending { it.popularity } else titles
        }

        val netflixShows = async { runCatching { NetflixTop10.shows() }.getOrDefault(emptyList()) }
        val netflixMovies = async { runCatching { NetflixTop10.movies() }.getOrDefault(emptyList()) }
        val allRecent = releasesBetween(today.minusDays(RecentDays), today)
        val allUpcoming = releasesBetween(today.plusDays(1), today.plusDays(UpcomingDays))

        apps.associate { app ->
            val providers = app.tmdbProviders
            val recent = allRecent.filter { it.sourceId in app.watchmodeSources }
            val upcoming = allUpcoming.filter { it.sourceId in app.watchmodeSources }

            val topShows: FolderRow
            val topMovies: FolderRow
            if (app.packageName == "com.netflix.ninja") {
                topShows = FolderRow("Top 10 Shows", netflixRanked(netflixShows.await(), TitleKind.Show), ranked = true)
                topMovies = FolderRow("Top 10 Movies", netflixRanked(netflixMovies.await(), TitleKind.Movie), ranked = true)
            } else {
                topShows = FolderRow("Popular Shows", runCatching { Tmdb.popular(TitleKind.Show, providers) }.getOrDefault(emptyList()).take(10), ranked = true)
                topMovies = FolderRow("Popular Movies", runCatching { Tmdb.popular(TitleKind.Movie, providers) }.getOrDefault(emptyList()).take(10), ranked = true)
            }

            app.packageName to listOf(
                FolderRow("Just Added", enrich(justAdded(recent), upcoming = false, byPopularity = true)),
                FolderRow("New Season", enrich(newSeasons(recent, upcoming = false), upcoming = false, byPopularity = true)),
                FolderRow("New Season Coming", enrich(newSeasons(upcoming, upcoming = true), upcoming = true, byPopularity = false)),
                topShows,
                topMovies,
            ).filter { it.titles.isNotEmpty() }
        }
    }

    // Ten days at a time keeps each request under the feed's 250-entry cap.
    private suspend fun releasesBetween(start: LocalDate, end: LocalDate): List<Release> {
        val spans = generateSequence(start) { it.plusDays(SpanDays) }.takeWhile { !it.isAfter(end) }
        return spans.toList().flatMap { from ->
            val to = minOf(from.plusDays(SpanDays - 1), end)
            runCatching { Watchmode.releases(from, to) }
                .onSuccess { if (it.size >= 250) Log.w(Tag, "Release feed full for $from–$to; some titles may be missing") }
                .getOrDefault(emptyList())
        }
    }

    // Netflix ranks titles by name; the guide supplies the artwork, Netflix the link.
    private suspend fun netflixRanked(ranks: List<NetflixRank>, kind: TitleKind): List<Title> = coroutineScope {
        ranks.map { rank ->
            async { runCatching { Tmdb.find(rank.name, kind) }.getOrNull()?.copy(netflixId = rank.netflixId) }
        }.awaitAll().filterNotNull()
    }
}

fun Title.toJson(): JSONObject = JSONObject()
    .put("tmdbId", tmdbId).put("kind", kind.name).put("name", name).put("overview", overview)
    .put("year", year).put("backdropPath", backdropPath).put("seasons", seasons)
    .put("popularity", popularity).put("note", note).put("netflixId", netflixId).put("videoId", videoId).put("rating", rating)

fun titleFromJson(json: JSONObject) = Title(
    tmdbId = json.getInt("tmdbId"),
    kind = TitleKind.valueOf(json.getString("kind")),
    name = json.getString("name"),
    overview = json.optString("overview"),
    year = json.optString("year"),
    backdropPath = json.optString("backdropPath").takeIf { it.isNotEmpty() && !json.isNull("backdropPath") },
    seasons = json.optInt("seasons").takeIf { it > 0 },
    popularity = json.optDouble("popularity", 0.0),
    note = json.optString("note"),
    netflixId = json.optString("netflixId").takeIf { it.isNotEmpty() && !json.isNull("netflixId") },
    videoId = json.optString("videoId").takeIf { it.isNotEmpty() && !json.isNull("videoId") },
    rating = json.optDouble("rating", 0.0),
)

private fun writeCache(day: LocalDate, rows: Map<String, List<FolderRow>>): String {
    val services = JSONObject()
    rows.forEach { (pkg, folder) ->
        services.put(pkg, JSONArray(folder.map { row ->
            JSONObject().put("heading", row.heading).put("ranked", row.ranked)
                .put("titles", JSONArray(row.titles.map { it.toJson() }))
        }))
    }
    return JSONObject().put("day", day.toString()).put("services", services).toString()
}

private fun readCache(text: String): Pair<LocalDate, Map<String, List<FolderRow>>> {
    val json = JSONObject(text)
    val services = json.getJSONObject("services")
    val rows = services.keys().asSequence().associateWith { pkg ->
        val folder = services.getJSONArray(pkg)
        List(folder.length()) { i ->
            val row = folder.getJSONObject(i)
            val titles = row.getJSONArray("titles")
            FolderRow(row.getString("heading"), List(titles.length()) { titleFromJson(titles.getJSONObject(it)) }, row.getBoolean("ranked"))
        }
    }
    return LocalDate.parse(json.getString("day")) to rows
}
