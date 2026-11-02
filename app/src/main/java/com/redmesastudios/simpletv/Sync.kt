package com.redmesastudios.simpletv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.coroutines.resume

private const val Tag = "SimpleTV.Sync"
private const val PreferredPort = 8724
private const val ServiceType = "_simpletv._tcp."
private const val Prefs = "sync"
private const val KnownPeersKey = "peers"
private val PullEvery = Duration.ofMinutes(10)

// One show started from SimpleTV, and when.
data class Watched(val title: Title, val at: Instant)

// Both sticks' lists for one app as one: each show once, at its latest start, newest first.
fun mergeWatched(mine: List<Watched>, theirs: List<Watched>): List<Watched> =
    (mine + theirs).groupBy { it.title.key }.values.map { same -> same.maxBy { it.at } }.sortedByDescending { it.at }

// What one stick does after reading another's photo list: fetch the photos it lacks, drop the
// ones removed anywhere, and keep every removal note so the removal keeps spreading.
data class PhotoPlan(val fetch: List<String>, val drop: List<String>, val removed: Map<String, Long>)

fun planPhotos(mine: Set<String>, myRemoved: Map<String, Long>, theirs: Set<String>, theirRemoved: Map<String, Long>): PhotoPlan {
    val removed = (myRemoved.keys + theirRemoved.keys).associateWith { maxOf(myRemoved[it] ?: 0L, theirRemoved[it] ?: 0L) }
    return PhotoPlan(
        fetch = (theirs - mine - removed.keys).sorted(),
        drop = (mine intersect removed.keys).sorted(),
        removed = removed,
    )
}

// The household's sticks share Continue Watching, the family photos and other apps' records
// (SharedRecords.kt). Each runs a small
// server on the home network while SimpleTV runs, announces it (NSD), and pulls from every
// other stick it finds: on start, every 10 minutes, when SimpleTV comes back to the front,
// and right after another stick says something changed. Every request carries the
// household's sync key (BuildConfig.SYNC_KEY, from ~/.config/simpletv/sync_key), so only
// sticks built with it take part. App order stays per TV.
object Sync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pulling = Mutex()
    private val resolving = Mutex()
    // Every other stick: its NSD name (or "known:" for one remembered from before) to "host:port".
    private val peers = ConcurrentHashMap<String, String>()
    private var app: Context? = null
    private var port = PreferredPort
    private var myName: String? = null

    val enabled: Boolean get() = BuildConfig.SYNC_KEY.isNotEmpty()

    fun start(context: Context) {
        if (!enabled || app != null) return
        app = context.applicationContext
        val server = runCatching { ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(PreferredPort)) } }
            .getOrElse { ServerSocket(0) }
        port = server.localPort
        thread(name = "sync-server", isDaemon = true) {
            while (!server.isClosed) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { client.use { serve(it) } }
            }
        }
        knownPeers(context).forEach { peers["known:$it"] = it }
        announceAndLook(context)
        scope.launch {
            while (true) {
                pullAll()
                delay(PullEvery.toMillis())
            }
        }
    }

    // SimpleTV is back in front: catch up now.
    fun pullSoon() {
        if (app != null) scope.launch { pullAll() }
    }

    // Something changed here; the other sticks pull it.
    fun changed(context: Context) {
        if (app == null) return
        scope.launch {
            peers.values.toSet().forEach { peer -> runCatching { request(peer, "/sync/poke", method = "POST") } }
        }
    }

    private suspend fun pullAll() = pulling.withLock {
        val context = app ?: return@withLock
        for (peer in peers.values.toSet()) {
            if (peer == "${localAddress(context)}:$port") continue
            runCatching { pullFrom(context, peer) }
                .onSuccess { remember(context, peer) }
                .onFailure { Log.w(Tag, "Couldn't sync with $peer", it) }
        }
    }

    private fun pullFrom(context: Context, peer: String) {
        val history = JSONObject(String(request(peer, "/sync/history")))
        for (pkg in history.keys()) {
            val theirs = history.getJSONArray(pkg).objects().map { Watched(titleFromJson(it), Instant.parse(it.getString("watchedAt"))) }
            val mine = WatchHistory.entries(context, pkg)
            val merged = mergeWatched(mine, theirs)
            if (merged != mine) WatchHistory.save(context, pkg, merged)
        }

        val photos = JSONObject(String(request(peer, "/sync/photos")))
        val theirs = photos.getJSONObject("photos").let { json -> json.keys().asSequence().filter(::isPhotoName).associateWith { json.getLong(it) } }
        val theirRemoved = photos.getJSONObject("removed").let { json -> json.keys().asSequence().filter(::isPhotoName).associateWith { json.getLong(it) } }
        val myRemoved = PhotoStore.removed(context)
        val plan = planPhotos(PhotoStore.list(context).map { it.name }.toSet(), myRemoved, theirs.keys, theirRemoved)
        if (plan.removed != myRemoved) PhotoStore.writeRemoved(context, plan.removed)
        plan.drop.forEach { PhotoStore.drop(context, it) }
        plan.fetch.forEach { name -> PhotoStore.adopt(context, name, request(peer, "/sync/photo/$name"), theirs.getValue(name)) }
        if (plan.fetch.isNotEmpty() || plan.drop.isNotEmpty()) Log.i(Tag, "From $peer: ${plan.fetch.size} new photos, ${plan.drop.size} removed")

        // Other apps' records (Trivia Night's players and scores); a stick too old to have
        // them answers 404, which leaves the rest of the sync alone.
        runCatching {
            val shared = JSONObject(String(request(peer, "/sync/shared")))
            for (name in shared.keys().asSequence().filter(::isCollectionName)) {
                SharedRecords.merge(context, name, parseShared(shared.getJSONArray(name)))
            }
        }.onFailure { Log.w(Tag, "Couldn't get shared records from $peer", it) }
    }

    // Plain HTTP over a socket: Android blocks cleartext HttpURLConnection, rightly so for
    // the internet, and these requests never leave the home network.
    private fun request(peer: String, path: String, method: String = "GET"): ByteArray {
        val host = peer.substringBeforeLast(':')
        val peerPort = peer.substringAfterLast(':').toInt()
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, peerPort), 5_000)
            socket.soTimeout = 30_000
            val head = "$method $path HTTP/1.1\r\nHost: $peer\r\nX-Sync-Key: ${BuildConfig.SYNC_KEY}\r\n" +
                "X-Sync-Port: $port\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
            socket.getOutputStream().apply { write(head.toByteArray()); flush() }
            val input = BufferedInputStream(socket.getInputStream())
            val status = readHttpLine(input)?.split(" ")?.getOrNull(1)?.toIntOrNull()
            val headers = generateSequence { readHttpLine(input)?.takeIf { it.isNotEmpty() } }
                .filter { ':' in it }.associate { it.substringBefore(':').trim().lowercase() to it.substringAfter(':').trim() }
            if (status != 200) throw java.io.IOException("HTTP $status for $path")
            val length = headers["content-length"]?.toIntOrNull() ?: throw java.io.IOException("No length for $path")
            return input.readNBytes(length)
        }
    }

    private fun serve(client: Socket) {
        val context = app ?: return
        runCatching {
            client.soTimeout = 30_000
            val input = BufferedInputStream(client.getInputStream())
            val requestLine = readHttpLine(input) ?: return
            val headerLines = generateSequence { readHttpLine(input)?.takeIf { it.isNotEmpty() } }.toList()
            val request = parseRequest(requestLine, headerLines) ?: return
            val length = request.headers["content-length"]?.toIntOrNull()?.coerceIn(0, 16 shl 20) ?: 0
            val body = if (length > 0) input.readNBytes(length) else ByteArray(0)
            val out = client.getOutputStream()
            val key = request.headers["x-sync-key"].orEmpty()
            if (!MessageDigest.isEqual(key.toByteArray(), BuildConfig.SYNC_KEY.toByteArray())) {
                writeHttpResponse(out, 403, "text/plain", "Forbidden".toByteArray())
                return
            }
            // A stick that calls is a stick to pull from, even before NSD finds it.
            val callerPort = request.headers["x-sync-port"]?.toIntOrNull()
            val caller = client.inetAddress?.hostAddress
            if (callerPort != null && caller != null) peers.putIfAbsent("caller:$caller", "$caller:$callerPort")
            route(context, request, body, out)
        }.onFailure { Log.w(Tag, "Sync request failed", it) }
    }

    private fun route(context: Context, request: HttpRequest, body: ByteArray, out: OutputStream) {
        when {
            request.method == "GET" && request.path == "/sync/shared" -> {
                val json = JSONObject()
                SharedRecords.collections(context).forEach { json.put(it, sharedToJson(SharedRecords.read(context, it))) }
                writeHttpResponse(out, 200, "application/json", json.toString().toByteArray())
            }
            // An app on this stick hands over its records and gets the household's copy back.
            request.method == "POST" && request.path.startsWith("/sync/shared/") -> {
                val name = request.path.removePrefix("/sync/shared/")
                if (!isCollectionName(name)) return writeHttpResponse(out, 404, "text/plain", "Not found".toByteArray())
                val (merged, changed) = SharedRecords.merge(context, name, parseShared(JSONArray(String(body))))
                writeHttpResponse(out, 200, "application/json", sharedToJson(merged).toString().toByteArray())
                if (changed) changed(context)
            }
            request.method == "GET" && request.path == "/sync/history" -> {
                val json = JSONObject()
                WatchHistory.all(context).forEach { (pkg, entries) ->
                    json.put(pkg, JSONArray(entries.map { it.title.toJson().put("watchedAt", it.at.toString()) }))
                }
                writeHttpResponse(out, 200, "application/json", json.toString().toByteArray())
            }
            request.method == "GET" && request.path == "/sync/photos" -> {
                val photos = JSONObject(PhotoStore.list(context).associate { it.name to it.lastModified() })
                val json = JSONObject().put("photos", photos).put("removed", JSONObject(PhotoStore.removed(context)))
                writeHttpResponse(out, 200, "application/json", json.toString().toByteArray())
            }
            request.method == "GET" && request.path.startsWith("/sync/photo/") -> {
                val bytes = PhotoStore.bytes(context, request.path.removePrefix("/sync/photo/"))
                if (bytes == null) writeHttpResponse(out, 404, "text/plain", "Not found".toByteArray())
                else writeHttpResponse(out, 200, "image/jpeg", bytes)
            }
            request.method == "POST" && request.path == "/sync/poke" -> {
                writeHttpResponse(out, 200, "text/plain", "OK".toByteArray())
                scope.launch {
                    delay(1_000)
                    pullAll()
                }
            }
            else -> writeHttpResponse(out, 404, "text/plain", "Not found".toByteArray())
        }
    }

    // Announces this stick and keeps the list of the others current as they come and go.
    private fun announceAndLook(context: Context) {
        val nsd = context.getSystemService(NsdManager::class.java) ?: return
        val info = NsdServiceInfo().apply {
            serviceName = "SimpleTV"
            serviceType = ServiceType
            port = this@Sync.port
        }
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registered: NsdServiceInfo) { myName = registered.serviceName }
            override fun onRegistrationFailed(info: NsdServiceInfo, error: Int) = Log.w(Tag, "Couldn't announce this stick ($error)").let { }
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, error: Int) = Unit
        })
        nsd.discoverServices(ServiceType, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.DiscoveryListener {
            override fun onServiceFound(found: NsdServiceInfo) {
                if (found.serviceName == myName) return
                scope.launch {
                    val address = resolve(nsd, found) ?: return@launch
                    if (address == "${localAddress(context)}:$port") return@launch
                    peers[found.serviceName] = address
                    pullAll()
                }
            }
            override fun onServiceLost(lost: NsdServiceInfo) { peers.remove(lost.serviceName) }
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, error: Int) = Log.w(Tag, "Couldn't look for other sticks ($error)").let { }
            override fun onStopDiscoveryFailed(serviceType: String, error: Int) = Unit
        })
    }

    // NSD resolves one service at a time.
    @Suppress("DEPRECATION")
    private suspend fun resolve(nsd: NsdManager, found: NsdServiceInfo): String? = resolving.withLock {
        suspendCancellableCoroutine { done ->
            nsd.resolveService(found, object : NsdManager.ResolveListener {
                override fun onServiceResolved(info: NsdServiceInfo) {
                    val host = info.host?.hostAddress
                    done.resume(if (host != null && ':' !in host) "$host:${info.port}" else null)
                }
                override fun onResolveFailed(info: NsdServiceInfo, error: Int) = done.resume(null)
            })
        }
    }

    // Sticks synced with before, tried first next time in case NSD is slow to find them.
    private fun knownPeers(context: Context): Set<String> =
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getStringSet(KnownPeersKey, emptySet()).orEmpty()

    private fun remember(context: Context, peer: String) {
        val known = knownPeers(context)
        if (peer !in known) context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().putStringSet(KnownPeersKey, known + peer).apply()
    }
}
