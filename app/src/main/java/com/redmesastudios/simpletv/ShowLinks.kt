package com.redmesastudios.simpletv

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

// Opening a show from a folder: into the app at that show when the app takes a link,
// otherwise just the app. Either way it joins the folder's Continue Watching row.
suspend fun Context.openTitle(app: StreamingApp, title: Title) {
    WatchHistory.record(this, app.packageName, title)
    NowPlaying.record(this, app.packageName, title)
    val link = showLink(app, title)
    val intent = link?.let {
        Intent(Intent.ACTION_VIEW, Uri.parse(it)).setPackage(app.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (intent != null && intent.resolveActivity(packageManager) != null) startActivity(intent) else launchApp(app)
}

// Links are looked up once and kept: each Watchmode lookup spends free-plan credits.
private suspend fun Context.showLink(app: StreamingApp, title: Title): String? {
    title.netflixId?.let { return "https://www.netflix.com/watch/$it" }
    title.videoId?.let { return "https://www.youtube.com/watch?v=$it" }
    if (app.watchmodeSources.isEmpty()) return null
    val prefs = getSharedPreferences("links", Context.MODE_PRIVATE)
    val key = "${app.packageName}:${title.kind}:${title.tmdbId}"
    if (prefs.contains(key)) return prefs.getString(key, null)?.ifEmpty { null }
    // A failed lookup isn't remembered, so it's tried again next time.
    val link = runCatching { Watchmode.webLink(title, app.watchmodeSources) }.getOrElse { return null }
    prefs.edit().putString(key, link.orEmpty()).apply()
    return link
}

// Shows started from SimpleTV, most recent first, per app. Streaming apps don't share
// what's being watched, so this is all SimpleTV can know.
object WatchHistory {
    private const val Prefs = "history"

    fun forApp(context: Context, packageName: String): List<Title> {
        val stored = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString(packageName, null) ?: return emptyList()
        val entries = JSONArray(stored)
        return List(entries.length()) { titleFromJson(entries.getJSONObject(it)) }
    }

    fun record(context: Context, packageName: String, title: Title) {
        val updated = listOf(title.copy(note = "")) + forApp(context, packageName).filterNot { it.key == title.key }
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit()
            .putString(packageName, JSONArray(updated.map(Title::toJson)).toString())
            .apply()
    }
}

// The last show started from SimpleTV, for the TV button's info panel. Kept in a file
// rather than preferences because the remote-button service reads it (and forgets it)
// from its own process.
object NowPlaying {
    private const val File = "now_playing.json"

    data class Started(val packageName: String, val title: Title, val at: Instant)

    fun record(context: Context, packageName: String, title: Title) {
        val json = JSONObject().put("package", packageName).put("title", title.toJson()).put("at", Instant.now().toString())
        java.io.File(context.filesDir, File).writeText(json.toString())
    }

    fun forget(context: Context) {
        java.io.File(context.filesDir, File).delete()
    }

    fun read(context: Context): Started? = runCatching {
        val json = JSONObject(java.io.File(context.filesDir, File).readText())
        Started(json.getString("package"), titleFromJson(json.getJSONObject("title")), Instant.parse(json.getString("at")))
    }.getOrNull()
}
