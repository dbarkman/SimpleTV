package com.redmesastudios.simpletv

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Duration
import java.time.Instant

private const val Tag = "SimpleTV.Services"
private const val CacheFile = "services.json"
private val RefreshEvery = Duration.ofDays(7)

// One streaming service as a show guide names it.
data class GuideService(val id: Int, val name: String)

// Every US streaming service TMDB and Watchmode know, so an app SimpleTV wasn't told about
// (ESPN, say) can be matched to its service by name and get a folder of its own.
data class ServiceList(val tmdb: List<GuideService> = emptyList(), val watchmode: List<GuideService> = emptyList()) {
    // Known services keep their hand-entered IDs; any other app gets the IDs whose
    // service name matches its own. An app with both kinds gets a folder.
    fun resolve(app: StreamingApp): StreamingApp {
        if (app.tmdbProviders.isNotEmpty() || app.watchmodeSources.isNotEmpty()) return app
        if (StreamingApps.any { it.packageName == app.packageName }) return app
        return app.copy(tmdbProviders = matchService(app.label, tmdb), watchmodeSources = matchService(app.label, watchmode))
    }
}

// Words that say which tier or bundle a service is, not which service.
private val TierWords = setOf("app", "plus", "premium", "essential", "with", "ads", "ad", "free", "standard", "streaming")
// Services resold inside another one ("Starz Amazon Channel") belong to that other app.
private val ResoldMarks = listOf("channel", "via ", "amazon", "roku", "apple tv")

// "ESPN+" and "ESPN Plus" -> "espn"; "Tubi - Movies & TV Shows" -> "tubi"; "Tubi TV" ->
// "tubitv", which still matches "tubi" by its start.
fun serviceCore(name: String): String = name
    .substringBefore(" - ").substringBefore(":")
    .lowercase()
    .replace("+", " plus ")
    .split(Regex("[^a-z0-9]+"))
    .filter { it.isNotEmpty() && it !in TierWords }
    .joinToString("")

// The IDs of every service whose name matches the app's: the same core name, or a longer
// one that starts with it ("espn" also takes "ESPN Deportes").
fun matchService(appLabel: String, services: List<GuideService>): Set<Int> {
    val app = serviceCore(appLabel)
    if (app.length < 3) return emptySet()
    return services
        .filter { service -> ResoldMarks.none { it in service.name.lowercase() } }
        .filter { service ->
            val core = serviceCore(service.name)
            core == app || (app.length >= 4 && core.startsWith(app))
        }
        .map { it.id }
        .toSet()
}

// The service lists, kept on disk and refreshed once a week.
object ServiceDirectory {
    private val state = MutableStateFlow(ServiceList())
    val services: StateFlow<ServiceList> = state.asStateFlow()
    private val refreshing = Mutex()
    private var fetchedAt: Instant? = null

    suspend fun refreshIfStale(context: Context) = refreshing.withLock {
        val file = File(context.filesDir, CacheFile)
        if (fetchedAt == null && file.exists()) {
            runCatching { readCache(file.readText()) }.onSuccess { (at, list) ->
                fetchedAt = at
                state.value = list
            }
        }
        if (fetchedAt?.let { Duration.between(it, Instant.now()) < RefreshEvery } == true) return@withLock
        runCatching { ServiceList(Tmdb.services(), Watchmode.services()) }
            .onSuccess { list ->
                val now = Instant.now()
                fetchedAt = now
                state.value = list
                file.writeText(writeCache(now, list))
            }
            .onFailure { Log.w(Tag, "Couldn't refresh the service lists", it) }
    }

    private fun writeCache(at: Instant, list: ServiceList): String {
        fun array(services: List<GuideService>) = JSONArray(services.map { JSONObject().put("id", it.id).put("name", it.name) })
        return JSONObject()
            .put("fetchedAt", at.toString())
            .put("tmdb", array(list.tmdb))
            .put("watchmode", array(list.watchmode))
            .toString()
    }

    private fun readCache(text: String): Pair<Instant, ServiceList> {
        val json = JSONObject(text)
        fun services(key: String) = json.getJSONArray(key).objects().map { GuideService(it.getInt("id"), it.getString("name")) }
        return Instant.parse(json.getString("fetchedAt")) to ServiceList(services("tmdb"), services("watchmode"))
    }
}
