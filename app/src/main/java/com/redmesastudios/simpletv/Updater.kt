package com.redmesastudios.simpletv

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Duration

// SimpleTV keeps itself up to date. scripts/release.sh posts every household's build as a
// release on a private GitHub repo, with a latest.txt listing them; each stick checks every
// 6 hours, downloads its own household's APK when it's newer, checks its SHA-256 and installs
// it over itself. It installs only while the home screen is showing, never over a show.

private const val Tag = "SimpleTV.Update"
private const val Repo = "dbarkman/SimpleTV-builds"
private const val ListAsset = "latest.txt"
private const val Prefs = "update"
private const val CheckedAtKey = "checkedAt"
private val CheckEvery = Duration.ofHours(6)

// One household's build in a release: a line of latest.txt,
// "<household> <versionCode> <sha256> <asset name>".
data class Build(val household: String, val versionCode: Long, val sha256: String, val asset: String)

private val Sha256Hex = Regex("[0-9a-f]{64}")

fun parseBuilds(text: String): List<Build> = text.lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .mapNotNull { line ->
        val parts = line.split(Regex("\\s+"))
        val code = parts.getOrNull(1)?.toLongOrNull()
        if (parts.size == 4 && code != null && parts[2].matches(Sha256Hex)) Build(parts[0], code, parts[2], parts[3]) else null
    }
    .toList()

// The household's build, when it's newer than the one installed.
fun updateFor(builds: List<Build>, household: String, installed: Long): Build? =
    builds.firstOrNull { it.household == household }?.takeIf { it.versionCode > installed }

object Updater {
    // True while MainActivity shows the home screen; an install waits for it.
    val homeShowing = MutableStateFlow(false)

    private val apiHeaders = mapOf(
        "Authorization" to "Bearer ${BuildConfig.UPDATE_TOKEN}",
        "Accept" to "application/vnd.github+json",
        "X-GitHub-Api-Version" to "2022-11-28",
        "User-Agent" to "SimpleTV",
    )

    // Runs while the home screen is up (MainActivity cancels it when another app comes to the
    // front), checking at most every 6 hours across restarts.
    suspend fun run(context: Context) {
        if (BuildConfig.UPDATE_TOKEN.isEmpty()) return
        val prefs = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
        while (true) {
            val wait = prefs.getLong(CheckedAtKey, 0) + CheckEvery.toMillis() - System.currentTimeMillis()
            if (wait > 0) delay(wait)
            try {
                checkAndInstall(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(Tag, "Update check failed", e)
            }
            prefs.edit { putLong(CheckedAtKey, System.currentTimeMillis()) }
        }
    }

    private suspend fun checkAndInstall(context: Context) {
        val apk = File(context.cacheDir, "update.apk")
        apk.delete()
        val installed = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        val release = JSONObject(httpGet("https://api.github.com/repos/$Repo/releases/latest", apiHeaders))
        val assets = release.getJSONArray("assets").objects().associate { it.getString("name") to it.getString("url") }
        val list = File(context.cacheDir, ListAsset)
        fetchAsset(assets[ListAsset] ?: return, list)
        val build = updateFor(parseBuilds(list.readText()), BuildConfig.HOUSEHOLD, installed) ?: return
        fetchAsset(assets[build.asset] ?: return, apk)
        if (sha256(apk) != build.sha256) {
            Log.w(Tag, "Downloaded ${build.asset} doesn't match its checksum; skipping")
            apk.delete()
            return
        }
        homeShowing.first { it }
        Log.i(Tag, "Installing ${build.versionCode} over $installed")
        install(context, apk)
    }

    // GitHub answers an asset request with a redirect to a signed download link, which is
    // fetched without the token. Rate limits and server hiccups are retried with backoff.
    private suspend fun fetchAsset(url: String, into: File) = withContext(Dispatchers.IO) {
        val part = File(into.path + ".part")
        var wait = 1_000L
        repeat(4) {
            val code = download(url, part)
            if (code in 200..299) {
                if (!part.renameTo(into)) throw IOException("Couldn't keep ${into.name}")
                return@withContext
            }
            if (code != 429 && code < 500) throw IOException("HTTP $code fetching ${into.name}")
            delay(wait)
            wait *= 2
        }
        throw IOException("Gave up fetching ${into.name} after retries")
    }

    private fun download(url: String, into: File): Int {
        val asset = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 10_000
            readTimeout = 30_000
            apiHeaders.forEach { (name, value) -> setRequestProperty(name, value) }
            setRequestProperty("Accept", "application/octet-stream")
        }
        val source = try {
            when (val code = asset.responseCode) {
                in 300..399 -> asset.getHeaderField("Location") ?: throw IOException("Redirect without a location")
                in 200..299 -> {
                    asset.inputStream.use { input -> into.outputStream().use { input.copyTo(it) } }
                    return code
                }
                else -> return code
            }
        } finally {
            asset.disconnect()
        }
        val signed = (URL(source).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
        }
        try {
            val code = signed.responseCode
            if (code in 200..299) signed.inputStream.use { input -> into.outputStream().use { input.copyTo(it) } }
            return code
        } finally {
            signed.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // Android lets an app update itself without asking; if it asks anyway, UpdateResult shows
    // its prompt. The install ends this process, and Android brings the home screen back.
    private fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("SimpleTV.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val result = PendingIntent.getBroadcast(
                context, id, Intent(context, UpdateResult::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(result.intentSender)
        }
    }
}

// Android's answer to an install: a prompt to show, or how it went.
class UpdateResult : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION ->
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            PackageInstaller.STATUS_SUCCESS -> Log.i(Tag, "Updated")
            else -> Log.w(Tag, "Update failed ($status): ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
        }
    }
}
