package com.redmesastudios.simpletv

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.DateTimeException
import java.time.MonthDay
import kotlin.concurrent.thread

private const val Tag = "SimpleTV.Settings"
private const val PreferredPort = 8727
private const val MaxBody = 16 * 1024
private const val MaxName = 40

// What each holiday pack is called on the settings page.
val PackLabels = mapOf(
    Pack.UsNational to "US holidays",
    Pack.Christian to "Christian holidays",
    Pack.World to "Around the world",
    Pack.FunDays to "Fun days (National Donut Day and friends)",
    Pack.Seasons to "First days of the seasons",
    Pack.Jewish to "Jewish holidays",
    Pack.Muslim to "Muslim holidays",
    Pack.Indian to "Indian holidays",
)

// A birthday or anniversary typed on the phone, or null if it doesn't make sense.
fun familyDayFrom(json: JSONObject): FamilyDay? {
    val name = json.optString("name").trim().replace(Regex("\\s+"), " ")
    if (name.isEmpty() || name.length > MaxName) return null
    val kind = FamilyDayKind.entries.firstOrNull { it.name.equals(json.optString("kind"), ignoreCase = true) } ?: return null
    val day = try {
        MonthDay.of(json.optInt("month"), json.optInt("day"))
    } catch (e: DateTimeException) {
        return null
    }
    return FamilyDay(day, kind, name)
}

fun houseStateJson(state: HouseState): JSONObject = JSONObject()
    .put("familyDays", JSONArray(state.familyDays.map {
        JSONObject()
            .put("id", it.id)
            .put("month", it.day.day.monthValue)
            .put("day", it.day.day.dayOfMonth)
            .put("kind", it.day.kind.name.lowercase())
            .put("name", it.day.name)
    }))
    .put("cards", JSONArray(Channel.entries.map { JSONObject().put("key", it.name.lowercase()).put("label", it.name).put("shown", it !in state.hiddenCards) }))
    .put("packs", JSONArray(Pack.entries.map { JSONObject().put("key", it.key).put("label", PackLabels.getValue(it)).put("on", it in state.packs) }))

// A small web server on the home network, running only while the TV shows its settings
// code. Phones load the page and change the household's settings (HouseSettings.kt); every
// request carries the PIN shown on the TV.
class SettingsServer(private val context: Context, val pin: String) {
    private var server: ServerSocket? = null

    // The address phones should open, or null if the stick isn't on a network.
    fun start(): String? {
        val socket = runCatching { ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(PreferredPort)) } }
            .getOrElse { ServerSocket(0) }
        server = socket
        thread(name = "settings", isDaemon = true) {
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
            val requestLine = readHttpLine(input) ?: return
            val headerLines = generateSequence { readHttpLine(input)?.takeIf { it.isNotEmpty() } }.toList()
            val request = parseRequest(requestLine, headerLines) ?: return
            val out = client.getOutputStream()
            if (request.query["pin"] != pin && request.headers["x-pin"] != pin) {
                if (request.method == "GET" && request.path == "/") {
                    writeHttpResponse(out, 200, "text/html; charset=utf-8", codePage(wrong = "pin" in request.query, heading = "TV settings").toByteArray())
                } else {
                    writeHttpResponse(out, 403, "text/plain", "Wrong code. Scan the QR code on the TV again.".toByteArray())
                }
                return
            }
            val length = request.headers["content-length"]?.toIntOrNull() ?: 0
            if (length > MaxBody) return writeHttpResponse(out, 413, "text/plain", "Too big".toByteArray())
            val body = if (length > 0) String(input.readNBytes(length)) else ""
            route(request, body, out)
        }.onFailure { Log.w(Tag, "Request failed", it) }
    }

    private fun route(request: HttpRequest, body: String, out: OutputStream) {
        fun settings() = writeHttpResponse(out, 200, "application/json", houseStateJson(HouseSettings.state.value).toString().toByteArray())
        when {
            request.method == "GET" && request.path == "/" ->
                writeHttpResponse(out, 200, "text/html; charset=utf-8", SettingsPage.toByteArray())
            request.method == "GET" && request.path == "/api/settings" -> settings()
            request.method == "POST" && request.path == "/api/family" -> {
                val day = runCatching { familyDayFrom(JSONObject(body)) }.getOrNull()
                    ?: return writeHttpResponse(out, 400, "text/plain", "Check the name and date.".toByteArray())
                HouseSettings.addFamilyDay(context, day)
                settings()
            }
            request.method == "DELETE" && request.path.startsWith("/api/family:") -> {
                HouseSettings.removeFamilyDay(context, request.path.removePrefix("/api/"))
                settings()
            }
            request.method == "PUT" && request.path == "/api/cards" -> {
                val hidden = runCatching { JSONArray(body) }.getOrNull()?.let { array ->
                    (0 until array.length()).mapNotNull { i -> Channel.entries.firstOrNull { it.name.equals(array.optString(i), ignoreCase = true) } }.toSet()
                } ?: return writeHttpResponse(out, 400, "text/plain", "Bad list".toByteArray())
                HouseSettings.setHiddenCards(context, hidden)
                settings()
            }
            request.method == "PUT" && request.path == "/api/packs" -> {
                val packs = runCatching { JSONArray(body) }.getOrNull()?.let { array ->
                    (0 until array.length()).mapNotNull { i -> Pack.entries.firstOrNull { it.key == array.optString(i) } }.toSet()
                } ?: return writeHttpResponse(out, 400, "text/plain", "Bad list".toByteArray())
                HouseSettings.setPacks(context, packs)
                settings()
            }
            else -> writeHttpResponse(out, 404, "text/plain", "Not found".toByteArray())
        }
    }
}

// The page phones see: the At a glance cards, the holiday packs, and the birthdays and
// anniversaries (adding one first, then the list). Every change saves at once.
private val SettingsPage = """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>TV settings</title>
<style>
  :root { --ground:#F4ECE1; --card:#FBF6EF; --edge:#E2D2BE; --ink:#2A211B; --soft:#6B5D52; --rust:#A84B3A; }
  * { box-sizing:border-box; }
  body { margin:0; background:var(--ground); color:var(--ink); font:16px/1.45 -apple-system, system-ui, sans-serif; padding:20px 16px 48px; }
  main { max-width:560px; margin:0 auto; }
  h1 { font-size:26px; margin:4px 0 4px; }
  h2 { font-size:19px; margin:28px 0 4px; }
  p { color:var(--soft); margin:0 0 12px; }
  section { background:var(--card); border:1px solid var(--edge); border-radius:14px; padding:6px 14px; }
  .row { display:flex; align-items:center; gap:12px; padding:11px 0; border-bottom:1px solid var(--edge); }
  .row:last-child { border-bottom:none; }
  .row .main { flex:1; min-width:0; }
  .when { color:var(--soft); font-size:14px; }
  .remove { border:none; background:none; color:var(--rust); font-size:15px; font-weight:600; padding:6px 4px; }
  label.toggle { display:flex; align-items:center; gap:12px; padding:12px 0; border-bottom:1px solid var(--edge); }
  label.toggle:last-child { border-bottom:none; }
  label.toggle span { flex:1; }
  input[type=checkbox] { width:24px; height:24px; accent-color:var(--ink); }
  form { display:grid; grid-template-columns:1fr 1fr; gap:10px; margin:12px 0 14px; }
  form .wide { grid-column:1 / -1; }
  input[type=text], select { width:100%; font-size:17px; padding:11px; border:1px solid var(--edge); border-radius:10px; background:#fff; color:var(--ink); }
  button.add { grid-column:1 / -1; padding:14px; border:none; border-radius:12px; background:var(--ink); color:#FBF4EA; font-size:17px; font-weight:700; }
  #toast { position:fixed; left:50%; bottom:24px; transform:translateX(-50%); background:var(--ink); color:#FBF4EA; padding:10px 18px; border-radius:20px; font-weight:600; opacity:0; transition:opacity .25s; pointer-events:none; }
  #toast.show { opacity:1; }
  .empty { color:var(--soft); padding:12px 0; }
</style>
</head>
<body>
<main>
  <h1>TV settings</h1>
  <p>Changes show on the TV right away, and on the other TVs in this home too.</p>

  <h2>At a glance</h2>
  <p>The cards along the bottom of the home screen.</p>
  <section id="cards"></section>

  <h2>Holiday decorations</h2>
  <p>Which holidays dress up the home screen.</p>
  <section id="packs"></section>

  <h2>Birthdays and anniversaries</h2>
  <p>On the day, the TV wishes them a happy birthday, with balloons and a cake.</p>
  <form id="add">
    <input class="wide" type="text" id="name" placeholder="Name, like Grandpa or Sam &amp; Jo" maxlength="40" autocomplete="off">
    <select id="month"></select>
    <select id="day"></select>
    <select class="wide" id="kind"><option value="birthday">Birthday</option><option value="anniversary">Anniversary</option></select>
    <button class="add" type="submit">Add</button>
  </form>
  <section id="family"></section>
</main>
<div id="toast"></div>
<script>
  var pin = new URLSearchParams(location.search).get('pin') || '';
  var months = ['January','February','March','April','May','June','July','August','September','October','November','December'];
  var monthSelect = document.getElementById('month'), daySelect = document.getElementById('day');
  months.forEach(function (m, i) { monthSelect.add(new Option(m, i + 1)); });
  for (var d = 1; d <= 31; d++) daySelect.add(new Option(d, d));

  function api(path, method, body) {
    return fetch(path + '?pin=' + encodeURIComponent(pin), { method: method || 'GET', body: body === undefined ? undefined : JSON.stringify(body) })
      .then(function (r) {
        if (r.status === 403) throw new Error('This page has expired. Scan the code on the TV again.');
        if (!r.ok) return r.text().then(function (t) { throw new Error(t || 'That didn\'t work.'); });
        return r.json();
      });
  }

  var toastTimer;
  function toast(text) {
    var t = document.getElementById('toast');
    t.textContent = text; t.className = 'show';
    clearTimeout(toastTimer); toastTimer = setTimeout(function () { t.className = ''; }, 1800);
  }
  function fail(e) { toast(e.message); }

  function render(s) {
    var family = document.getElementById('family');
    family.innerHTML = '';
    if (!s.familyDays.length) family.innerHTML = '<div class="empty">None yet.</div>';
    s.familyDays.forEach(function (f) {
      var row = document.createElement('div'); row.className = 'row';
      var main = document.createElement('div'); main.className = 'main';
      var name = document.createElement('div'); name.textContent = f.name;
      var when = document.createElement('div'); when.className = 'when';
      when.textContent = months[f.month - 1] + ' ' + f.day + ' · ' + (f.kind === 'anniversary' ? 'Anniversary' : 'Birthday');
      main.appendChild(name); main.appendChild(when);
      var remove = document.createElement('button'); remove.className = 'remove'; remove.textContent = 'Remove';
      remove.onclick = function () {
        if (!confirm('Remove ' + f.name + '?')) return;
        api('/api/' + f.id, 'DELETE').then(function (s) { render(s); toast('Removed'); }).catch(fail);
      };
      row.appendChild(main); row.appendChild(remove); family.appendChild(row);
    });

    function toggles(id, items, isOn, save) {
      var box = document.getElementById(id);
      box.innerHTML = '';
      items.forEach(function (item) {
        var label = document.createElement('label'); label.className = 'toggle';
        var text = document.createElement('span'); text.textContent = item.label;
        var box2 = document.createElement('input'); box2.type = 'checkbox'; box2.checked = isOn(item); box2.dataset.key = item.key;
        box2.onchange = save;
        label.appendChild(text); label.appendChild(box2); box.appendChild(label);
      });
    }
    toggles('cards', s.cards, function (c) { return c.shown; }, function () {
      var hidden = Array.prototype.filter.call(document.querySelectorAll('#cards input'), function (i) { return !i.checked; }).map(function (i) { return i.dataset.key; });
      api('/api/cards', 'PUT', hidden).then(function () { toast('Saved'); }).catch(fail);
    });
    toggles('packs', s.packs, function (p) { return p.on; }, function () {
      var on = Array.prototype.filter.call(document.querySelectorAll('#packs input'), function (i) { return i.checked; }).map(function (i) { return i.dataset.key; });
      api('/api/packs', 'PUT', on).then(function () { toast('Saved'); }).catch(fail);
    });
  }

  document.getElementById('add').addEventListener('submit', function (event) {
    event.preventDefault();
    var name = document.getElementById('name').value.trim();
    if (!name) { toast('Type a name first'); return; }
    api('/api/family', 'POST', { name: name, month: +monthSelect.value, day: +daySelect.value, kind: document.getElementById('kind').value })
      .then(function (s) { render(s); document.getElementById('name').value = ''; toast('Added ' + name); })
      .catch(fail);
  });

  api('/api/settings').then(render).catch(fail);
</script>
</body>
</html>
""".trimIndent()
