package com.redmesastudios.simpletv

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlin.concurrent.thread
import kotlin.random.Random

private const val Tag = "SimpleTV.Upload"
private const val PreferredPort = 8723
private const val MaxUpload = 40 * 1024 * 1024

// A four-digit code the phone needs, so only someone looking at the TV can add or remove photos.
fun newUploadPin(random: Random = Random.Default): String = random.nextInt(0, 10_000).toString().padStart(4, '0')

data class HttpRequest(val method: String, val path: String, val query: Map<String, String>, val headers: Map<String, String>)

// "PUT /api/upload?pin=1234 HTTP/1.1" plus header lines, as read off the socket.
fun parseRequest(requestLine: String, headerLines: List<String>): HttpRequest? {
    val parts = requestLine.split(" ")
    if (parts.size < 2) return null
    val target = parts[1]
    val path = URLDecoder.decode(target.substringBefore('?'), "UTF-8")
    val query = target.substringAfter('?', "").split('&').filter { '=' in it }.associate {
        URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('='), "UTF-8")
    }
    val headers = headerLines.filter { ':' in it }.associate { it.substringBefore(':').trim().lowercase() to it.substringAfter(':').trim() }
    return HttpRequest(parts[0].uppercase(), path, query, headers)
}

// A tiny web server on the home network, running only while the TV shows "Add photos".
// Phones load the page, pick photos and send them one at a time; the stick resizes each on
// arrival (PhotoStore). Every request must carry the PIN shown on the TV.
class PhotoUploadServer(private val context: Context, val pin: String, private val onChange: () -> Unit) {
    private var server: ServerSocket? = null

    // The address phones should open, or null if the stick isn't on a network.
    fun start(): String? {
        val socket = runCatching { ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(PreferredPort)) } }
            .getOrElse { ServerSocket(0) }
        server = socket
        thread(name = "photo-upload", isDaemon = true) {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { client.use { serve(it) } }
            }
        }
        val address = localAddress(context) ?: return null
        return "http://$address:${socket.localPort}/?pin=$pin"
    }

    fun stop() {
        runCatching { server?.close() }
        server = null
    }

    private fun serve(client: Socket) {
        runCatching {
            client.soTimeout = 30_000
            val input = BufferedInputStream(client.getInputStream())
            val requestLine = readLine(input) ?: return
            val headerLines = generateSequence { readLine(input)?.takeIf { it.isNotEmpty() } }.toList()
            val request = parseRequest(requestLine, headerLines) ?: return
            val out = client.getOutputStream()
            if (request.query["pin"] != pin && request.headers["x-pin"] != pin) {
                // Someone who typed the address instead of scanning gets asked for the code.
                if (request.method == "GET" && request.path == "/") {
                    respond(out, 200, "text/html; charset=utf-8", codePage(wrong = "pin" in request.query).toByteArray())
                } else {
                    respond(out, 403, "text/plain", "Wrong code. Scan the QR code on the TV again.".toByteArray())
                }
                return
            }
            route(request, input, out)
        }.onFailure { Log.w(Tag, "Request failed", it) }
    }

    private fun route(request: HttpRequest, input: InputStream, out: OutputStream) {
        when {
            request.method == "GET" && request.path == "/" ->
                respond(out, 200, "text/html; charset=utf-8", UploadPage.toByteArray())
            request.method == "GET" && request.path == "/api/photos" -> {
                val list = JSONArray(PhotoStore.list(context).map { JSONObject().put("name", it.name) })
                respond(out, 200, "application/json", list.toString().toByteArray())
            }
            request.method == "GET" && request.path.startsWith("/api/thumb/") -> {
                val thumb = PhotoStore.thumbnail(context, request.path.removePrefix("/api/thumb/"))
                if (thumb == null) respond(out, 404, "text/plain", "Not found".toByteArray())
                else respond(out, 200, "image/jpeg", thumb)
            }
            request.method == "PUT" && request.path == "/api/upload" -> {
                val length = request.headers["content-length"]?.toIntOrNull()
                if (length == null || length <= 0 || length > MaxUpload) {
                    respond(out, 413, "text/plain", "Too big".toByteArray())
                    return
                }
                val bytes = input.readNBytes(length)
                val name = PhotoStore.save(context, bytes)
                if (name == null) {
                    respond(out, 415, "text/plain", "That doesn't look like a photo.".toByteArray())
                } else {
                    onChange()
                    respond(out, 200, "application/json", JSONObject().put("name", name).toString().toByteArray())
                }
            }
            request.method == "DELETE" && request.path.startsWith("/api/photo/") -> {
                val deleted = PhotoStore.delete(context, request.path.removePrefix("/api/photo/"))
                if (deleted) onChange()
                respond(out, if (deleted) 200 else 404, "text/plain", (if (deleted) "Deleted" else "Not found").toByteArray())
            }
            else -> respond(out, 404, "text/plain", "Not found".toByteArray())
        }
    }

    private fun readLine(input: InputStream): String? {
        val line = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) return if (line.isEmpty()) null else line.toString()
            if (b == '\n'.code) return line.toString().trimEnd('\r')
            line.append(b.toChar())
            if (line.length > 8192) return null
        }
    }

    private fun respond(out: OutputStream, status: Int, type: String, body: ByteArray) {
        val reason = mapOf(200 to "OK", 403 to "Forbidden", 404 to "Not Found", 413 to "Payload Too Large", 415 to "Unsupported Media Type")[status] ?: "OK"
        val head = "HTTP/1.1 $status $reason\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n"
        out.write(head.toByteArray())
        out.write(body)
        out.flush()
    }
}

// The stick's own address on the home network.
fun localAddress(context: Context): String? {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val links = connectivity.getLinkProperties(connectivity.activeNetwork) ?: return null
    return links.linkAddresses.map { it.address }.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
}

// Asks for the four-digit code shown on the TV.
private fun codePage(wrong: Boolean) = """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>SimpleTV photos</title>
<style>
  body { margin:0; background:#F4ECE1; color:#2A211B; font:16px/1.45 -apple-system, system-ui, sans-serif; padding:40px 16px; }
  main { max-width:420px; margin:0 auto; }
  h1 { font-size:26px; margin:0 0 6px; }
  p { color:#6B5D52; margin:0 0 18px; }
  input { width:100%; box-sizing:border-box; font-size:28px; letter-spacing:8px; text-align:center; padding:12px; border:2px solid #2A211B; border-radius:12px; background:#FBF6EF; }
  button { margin-top:14px; width:100%; padding:14px; border:none; border-radius:12px; background:#2A211B; color:#FBF4EA; font-size:18px; font-weight:700; }
  .wrong { color:#A84B3A; font-weight:600; }
</style>
</head>
<body>
<main>
  <h1>Photos for the TV</h1>
  <p>Enter the four-digit code shown on the TV.</p>
  ${if (wrong) "<p class=\"wrong\">That code didn't match. Check the TV and try again.</p>" else ""}
  <form method="get" action="/">
    <input name="pin" inputmode="numeric" pattern="[0-9]*" maxlength="4" autofocus>
    <button type="submit">Continue</button>
  </form>
</main>
</body>
</html>
""".trimIndent()

// The page phones see: pick photos, watch them upload, and remove ones already on the TV.
// Photos are shrunk in the phone's browser first so they upload quickly; the stick resizes
// them again either way.
private val UploadPage = """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>SimpleTV photos</title>
<style>
  :root { --ground:#F4ECE1; --card:#FBF6EF; --edge:#E2D2BE; --ink:#2A211B; --soft:#6B5D52; --rust:#A84B3A; }
  * { box-sizing: border-box; }
  body { margin:0; background:var(--ground); color:var(--ink); font:16px/1.45 -apple-system, system-ui, sans-serif; padding:20px 16px 48px; }
  main { max-width:640px; margin:0 auto; }
  h1 { font-size:26px; margin:4px 0 4px; }
  p { color:var(--soft); margin:0 0 18px; }
  .pick { display:block; width:100%; padding:16px; border:none; border-radius:14px; background:var(--ink); color:#FBF4EA; font-size:18px; font-weight:700; }
  input[type=file] { display:none; }
  #status { margin:14px 0 6px; font-weight:600; min-height:1.4em; }
  h2 { font-size:18px; margin:26px 0 10px; }
  .grid { display:grid; grid-template-columns:repeat(3, minmax(0,1fr)); gap:8px; }
  .tile { position:relative; aspect-ratio:16/9; border-radius:10px; overflow:hidden; background:var(--edge); }
  .tile img { width:100%; height:100%; object-fit:cover; display:block; }
  .tile button { position:absolute; top:6px; right:6px; width:30px; height:30px; border-radius:15px; border:none; background:rgba(42,33,27,.75); color:#fff; font-size:16px; line-height:30px; padding:0; }
  .empty { color:var(--soft); }
</style>
</head>
<body>
<main>
  <h1>Photos for the TV</h1>
  <p>Pick photos to show on SimpleTV's screensaver. They stay on the TV and never go online.</p>
  <label class="pick" for="files">Choose photos</label>
  <input id="files" type="file" accept="image/*" multiple>
  <div id="status"></div>
  <h2 id="count">On the TV</h2>
  <div class="grid" id="grid"></div>
</main>
<script>
  var pin = new URLSearchParams(location.search).get('pin') || '';
  var statusLine = document.getElementById('status');

  function api(path) { return path + (path.indexOf('?') < 0 ? '?' : '&') + 'pin=' + encodeURIComponent(pin); }

  // Shrinks a photo to TV size in the browser; falls back to the original if it can't.
  function shrink(file) {
    if (!window.createImageBitmap) return Promise.resolve(file);
    return createImageBitmap(file, { imageOrientation: 'from-image' }).then(function (img) {
      var scale = Math.min(1, 1920 / img.width, 1080 / img.height);
      var canvas = document.createElement('canvas');
      canvas.width = Math.round(img.width * scale);
      canvas.height = Math.round(img.height * scale);
      canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
      return new Promise(function (done) { canvas.toBlob(function (blob) { done(blob || file); }, 'image/jpeg', 0.9); });
    }).catch(function () { return file; });
  }

  document.getElementById('files').addEventListener('change', function (event) {
    var files = Array.prototype.slice.call(event.target.files);
    var sent = 0, failed = 0;
    function next() {
      if (!files.length) {
        statusLine.textContent = 'Added ' + sent + (sent === 1 ? ' photo' : ' photos') + (failed ? ' (' + failed + ' could not be added)' : '') + '.';
        event.target.value = '';
        load();
        return;
      }
      var file = files.shift();
      statusLine.textContent = 'Sending ' + (sent + failed + 1) + ' of ' + (sent + failed + files.length + 1) + '…';
      shrink(file).then(function (blob) {
        return fetch(api('/api/upload'), { method: 'PUT', body: blob });
      }).then(function (r) { if (r.ok) sent++; else failed++; })
        .catch(function () { failed++; })
        .then(next);
    }
    if (files.length) next();
  });

  function load() {
    fetch(api('/api/photos')).then(function (r) {
      if (!r.ok) throw new Error('code');
      return r.json();
    }).then(function (photos) {
      document.getElementById('count').textContent = 'On the TV (' + photos.length + ')';
      var grid = document.getElementById('grid');
      grid.innerHTML = '';
      if (!photos.length) { grid.innerHTML = '<div class="empty">No photos yet.</div>'; return; }
      photos.forEach(function (photo) {
        var tile = document.createElement('div');
        tile.className = 'tile';
        var img = document.createElement('img');
        img.loading = 'lazy';
        img.src = api('/api/thumb/' + photo.name);
        var remove = document.createElement('button');
        remove.textContent = '✕';
        remove.setAttribute('aria-label', 'Remove this photo');
        remove.onclick = function () {
          if (!confirm('Remove this photo from the TV?')) return;
          fetch(api('/api/photo/' + photo.name), { method: 'DELETE' }).then(load);
        };
        tile.appendChild(img);
        tile.appendChild(remove);
        grid.appendChild(tile);
      });
    }).catch(function () {
      statusLine.textContent = 'This page has expired. Scan the code on the TV again.';
    });
  }
  load();
</script>
</body>
</html>
""".trimIndent()
