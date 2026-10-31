package com.redmesastudios.simpletv

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val Tag = "SimpleTV.YouTube"
private const val Prefs = "youtube"
private const val RefreshTokenKey = "refreshToken"
private const val CacheFile = "youtube.json"
private const val Scope = "https://www.googleapis.com/auth/youtube.readonly"
private val RefreshEvery = Duration.ofHours(1)
// Shorts are left out: they're made for phones.
private val ShortestVideo = Duration.ofSeconds(61)
private const val TopChannels = 15

fun youTubeThumbnail(videoId: String, size: String) = "https://i.ytimg.com/vi/$videoId/$size.jpg"

// "2 hours ago", "Yesterday", "3 days ago", then the date.
fun videoAge(published: Instant, now: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val age = Duration.between(published, now)
    val days = Duration.between(published.atZone(zone).toLocalDate().atStartOfDay(zone), now.atZone(zone).toLocalDate().atStartOfDay(zone)).toDays()
    return when {
        age.toMinutes() < 60 -> "Just now"
        age.toHours() < 24 && days == 0L -> if (age.toHours() == 1L) "1 hour ago" else "${age.toHours()} hours ago"
        days <= 1L -> "Yesterday"
        days < 7L -> "$days days ago"
        else -> DateTimeFormatter.ofPattern("MMM d").format(published.atZone(zone))
    }
}

// The newest videos across every subscription, newest first.
fun newestVideos(videos: List<Title>, published: Map<String, Instant>, count: Int): List<Title> =
    videos.sortedByDescending { published[it.videoId] ?: Instant.EPOCH }.distinctBy { it.videoId }.take(count)

// The newest full-length video from each channel, in the channels' own order (YouTube lists
// subscriptions by "relevance", believed to follow how much someone watches each one).
fun topChannelPicks(uploadsByChannel: List<List<Pair<Title, Instant>>>, fullLength: Set<String>, count: Int): List<Title> =
    uploadsByChannel.mapNotNull { uploads ->
        uploads.sortedByDescending { it.second }.firstOrNull { it.first.videoId in fullLength }?.first
    }.take(count)

// Signing in to YouTube the TV way: the screen shows a short code, which someone approves
// on their phone at google.com/device. The lasting refresh token is kept on the box.
object YouTubeAccount {
    sealed interface State {
        data object Unknown : State
        data object SignedOut : State
        data class Code(val userCode: String, val url: String) : State
        data object SignedIn : State
    }

    private val state = MutableStateFlow<State>(State.Unknown)
    val status: StateFlow<State> = state.asStateFlow()
    private val tokenLock = Mutex()
    private var accessToken: String? = null
    private var expiresAt: Instant = Instant.EPOCH

    fun check(context: Context) {
        state.value = if (refreshToken(context) != null) State.SignedIn else State.SignedOut
    }

    private fun refreshToken(context: Context) =
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString(RefreshTokenKey, null)

    // Shows a code and waits for it to be approved, getting a new code when one runs out.
    // Runs while the YouTube folder is open; leaving the folder cancels it.
    suspend fun signIn(context: Context) {
        while (refreshToken(context) == null) {
            val device = runCatching {
                postForm("https://oauth2.googleapis.com/device/code", mapOf("client_id" to BuildConfig.YOUTUBE_CLIENT_ID, "scope" to Scope)).second
            }.getOrElse {
                Log.w(Tag, "Couldn't get a sign-in code", it)
                delay(30_000)
                continue
            }
            state.value = State.Code(device.getString("user_code"), device.getString("verification_url"))
            var interval = device.optLong("interval", 5)
            val expires = Instant.now().plusSeconds(device.getLong("expires_in"))
            while (Instant.now() < expires) {
                delay(interval * 1_000)
                val (code, reply) = runCatching {
                    postForm("https://oauth2.googleapis.com/token", mapOf(
                        "client_id" to BuildConfig.YOUTUBE_CLIENT_ID,
                        "client_secret" to BuildConfig.YOUTUBE_CLIENT_SECRET,
                        "device_code" to device.getString("device_code"),
                        "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    ))
                }.getOrElse { continue }
                if (code in 200..299) {
                    context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit()
                        .putString(RefreshTokenKey, reply.getString("refresh_token")).apply()
                    remember(reply)
                    state.value = State.SignedIn
                    return
                }
                when (reply.optString("error")) {
                    "authorization_pending" -> Unit
                    "slow_down" -> interval += 5
                    else -> break // Declined or expired: start over with a fresh code.
                }
            }
        }
        state.value = State.SignedIn
    }

    // A current access token, renewed from the refresh token as needed. Null when signed
    // out, including when Google has ended the sign-in.
    suspend fun token(context: Context): String? = tokenLock.withLock {
        accessToken?.let { if (Instant.now() < expiresAt) return@withLock it }
        val refresh = refreshToken(context) ?: return@withLock null
        val (code, reply) = postForm("https://oauth2.googleapis.com/token", mapOf(
            "client_id" to BuildConfig.YOUTUBE_CLIENT_ID,
            "client_secret" to BuildConfig.YOUTUBE_CLIENT_SECRET,
            "refresh_token" to refresh,
            "grant_type" to "refresh_token",
        ))
        if (code in 200..299) return@withLock remember(reply)
        if (reply.optString("error") == "invalid_grant") {
            context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().remove(RefreshTokenKey).apply()
            state.value = State.SignedOut
            return@withLock null
        }
        throw IOException("Token refresh failed with HTTP $code")
    }

    private fun remember(reply: JSONObject): String {
        val token = reply.getString("access_token")
        accessToken = token
        // Renewed a minute early so a request never goes out with a token about to lapse.
        expiresAt = Instant.now().plusSeconds(reply.optLong("expires_in", 3600) - 60)
        return token
    }
}

// Google's sign-in endpoints answer errors with a JSON body that matters, so this keeps it.
private suspend fun postForm(url: String, fields: Map<String, String>): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
    val body = fields.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 10_000
        readTimeout = 20_000
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    }
    try {
        connection.outputStream.use { it.write(body.toByteArray()) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        code to JSONObject(stream?.bufferedReader()?.use { it.readText() }.orEmpty().ifEmpty { "{}" })
    } finally {
        connection.disconnect()
    }
}

object YouTubeApi {
    private const val Api = "https://www.googleapis.com/youtube/v3"

    private suspend fun get(token: String, path: String) =
        JSONObject(httpGet(Api + path, mapOf("Authorization" to "Bearer $token", "Accept" to "application/json")))

    suspend fun subscribedChannels(token: String): List<String> = buildList {
        var page = ""
        do {
            val reply = get(token, "/subscriptions?part=snippet&mine=true&maxResults=50$page")
            reply.getJSONArray("items").objects().forEach { add(it.getJSONObject("snippet").getJSONObject("resourceId").getString("channelId")) }
            page = reply.optString("nextPageToken").let { if (it.isEmpty()) "" else "&pageToken=$it" }
        } while (page.isNotEmpty())
    }

    // A channel's latest uploads, with when each went up.
    suspend fun latestUploads(token: String, channelId: String, now: Instant): List<Pair<Title, Instant>> {
        val uploads = "UU" + channelId.removePrefix("UC")
        return get(token, "/playlistItems?part=snippet,contentDetails&maxResults=4&playlistId=$uploads")
            .getJSONArray("items").objects().mapNotNull { item ->
                val details = item.getJSONObject("contentDetails")
                val published = details.text("videoPublishedAt").ifEmpty { return@mapNotNull null }.let(Instant::parse)
                video(details.getString("videoId"), item.getJSONObject("snippet"), published, now) to published
            }
    }

    suspend fun popular(token: String, now: Instant): List<Title> = videos(token, "chart=mostPopular&regionCode=US", now)

    // Videos given a thumbs-up, most recent like first.
    suspend fun liked(token: String, now: Instant): List<Title> = videos(token, "myRating=like", now)

    private suspend fun videos(token: String, query: String, now: Instant): List<Title> =
        get(token, "/videos?part=snippet,contentDetails&maxResults=40&$query")
            .getJSONArray("items").objects()
            .filter { isFullLength(it.getJSONObject("contentDetails")) }
            .map { video(it.getString("id"), it.getJSONObject("snippet"), Instant.parse(it.getJSONObject("snippet").getString("publishedAt")), now) }

    // Which of these videos are full-length, checked 50 at a time.
    // A video's whole description, as it reads on YouTube now.
    suspend fun description(token: String, videoId: String): String? =
        get(token, "/videos?part=snippet&id=$videoId").getJSONArray("items").objects().firstOrNull()
            ?.getJSONObject("snippet")?.text("description")?.trim()

    suspend fun fullLength(token: String, videoIds: List<String>): Set<String> = videoIds.chunked(50).flatMap { ids ->
        get(token, "/videos?part=contentDetails&id=${ids.joinToString(",")}").getJSONArray("items").objects()
            .filter { isFullLength(it.getJSONObject("contentDetails")) }
            .map { it.getString("id") }
    }.toSet()

    private fun isFullLength(details: JSONObject): Boolean =
        runCatching { Duration.parse(details.getString("duration")) >= ShortestVideo }.getOrDefault(false)

    private fun video(id: String, snippet: JSONObject, published: Instant, now: Instant): Title {
        val thumbnails = snippet.getJSONObject("thumbnails")
        val large = listOf("maxres", "standard", "high").firstNotNullOfOrNull { thumbnails.optJSONObject(it)?.text("url")?.ifEmpty { null } }
        return Title(
            tmdbId = 0,
            kind = TitleKind.Video,
            name = snippet.text("title"),
            overview = snippet.text("description").trim(),
            backdropPath = large,
            note = "${snippet.text("channelTitle")} · ${videoAge(published, now)}",
            videoId = id,
        )
    }
}

// The YouTube folder's rows, fetched when the folder opens (at most hourly) and kept on
// disk so it opens instantly.
object YouTubeStore {
    private val state = MutableStateFlow<List<FolderRow>>(emptyList())
    val rows: StateFlow<List<FolderRow>> = state.asStateFlow()
    private val refreshing = Mutex()
    private var fetchedAt: Instant? = null

    suspend fun refreshIfStale(context: Context) = refreshing.withLock {
        val file = File(context.filesDir, CacheFile)
        if (fetchedAt == null && file.exists()) {
            runCatching { readCache(file.readText()) }.onSuccess { (at, rows) ->
                fetchedAt = at
                state.value = rows
            }
        }
        if (fetchedAt?.let { Duration.between(it, Instant.now()) < RefreshEvery } == true) return@withLock
        runCatching {
            val token = YouTubeAccount.token(context) ?: return@withLock
            fetch(token)
        }
            .onSuccess { rows ->
                val now = Instant.now()
                fetchedAt = now
                state.value = rows
                file.writeText(writeCache(now, rows))
            }
            .onFailure { Log.w(Tag, "Couldn't refresh YouTube", it) }
    }

    fun forget(context: Context) {
        state.value = emptyList()
        fetchedAt = null
        File(context.filesDir, CacheFile).delete()
    }

    private suspend fun fetch(token: String): List<FolderRow> = coroutineScope {
        val now = Instant.now()
        val lookups = Semaphore(6)
        val channels = YouTubeApi.subscribedChannels(token)
        val uploadsByChannel = channels.map { channel ->
            async { lookups.withPermit { runCatching { YouTubeApi.latestUploads(token, channel, now) }.getOrDefault(emptyList()) } }
        }.awaitAll()
        val uploads = uploadsByChannel.flatten()
        val published = uploads.associate { (video, at) -> video.videoId!! to at }
        val candidates = newestVideos(uploads.map { it.first }, published, 60)
        val topChannels = uploadsByChannel.take(TopChannels + 5)
        val keep = YouTubeApi.fullLength(token, (candidates + topChannels.flatten().map { it.first }).mapNotNull { it.videoId }.distinct())
        val subscriptions = candidates.filter { it.videoId in keep }.take(30)
        val favorites = topChannelPicks(topChannels, keep, TopChannels)
        val liked = runCatching { YouTubeApi.liked(token, now).take(30) }.getOrDefault(emptyList())
        val popular = runCatching { YouTubeApi.popular(token, now).take(20) }.getOrDefault(emptyList())
        listOf(
            FolderRow("New From Your Subscriptions", subscriptions),
            FolderRow("Your Top Channels", favorites),
            FolderRow("Liked Videos", liked),
            FolderRow("Popular on YouTube", popular),
        ).filter { it.titles.isNotEmpty() }
    }

    private fun writeCache(at: Instant, rows: List<FolderRow>): String = JSONObject()
        .put("fetchedAt", at.toString())
        .put("rows", JSONArray(rows.map { row -> JSONObject().put("heading", row.heading).put("titles", JSONArray(row.titles.map(Title::toJson))) }))
        .toString()

    private fun readCache(text: String): Pair<Instant, List<FolderRow>> {
        val json = JSONObject(text)
        val rows = json.getJSONArray("rows").objects().map { row ->
            FolderRow(row.getString("heading"), row.getJSONArray("titles").objects().map(::titleFromJson))
        }
        return Instant.parse(json.getString("fetchedAt")) to rows
    }
}
