package com.redmesastudios.simpletv

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val Tag = "SimpleTV.Shared"

// One record another app on this stick keeps in the household's shared box (Trivia Night's
// players, games and asked questions): an id, when it last changed, and the whole record as
// JSON, which SimpleTV passes along without reading.
data class SharedRecord(val id: String, val at: Long, val json: String)

// Two copies of a collection as one: each id at its newest; on a tie, mine stays.
fun mergeShared(mine: List<SharedRecord>, theirs: List<SharedRecord>): List<SharedRecord> {
    val merged = LinkedHashMap<String, SharedRecord>()
    mine.forEach { merged[it.id] = it }
    theirs.forEach { record -> if ((merged[record.id]?.at ?: Long.MIN_VALUE) < record.at) merged[record.id] = record }
    return merged.values.toList()
}

fun isCollectionName(name: String): Boolean = name.matches(Regex("[a-z0-9-]{1,40}"))

// The household's shared box: collections of records, one file each in files/shared. Apps on
// this stick swap records with it over the sync server (POST /sync/shared/<collection>), and
// Sync passes every collection between the sticks like Continue Watching.
object SharedRecords {
    private val lock = Any()

    fun collections(context: Context): List<String> =
        dir(context).listFiles().orEmpty().map { it.name.removeSuffix(".json") }.filter(::isCollectionName).sorted()

    fun read(context: Context, name: String): List<SharedRecord> = synchronized(lock) {
        val file = File(dir(context), "$name.json")
        if (!file.exists()) return emptyList()
        runCatching { parseShared(JSONArray(file.readText())) }
            .onFailure { Log.w(Tag, "Couldn't read $name", it) }
            .getOrDefault(emptyList())
    }

    // Merges records in and returns the collection afterward, and whether anything changed.
    fun merge(context: Context, name: String, incoming: List<SharedRecord>): Pair<List<SharedRecord>, Boolean> = synchronized(lock) {
        val mine = read(context, name)
        val merged = mergeShared(mine, incoming)
        val changed = merged != mine
        if (changed) {
            val file = File(dir(context), "$name.json")
            val temp = File(file.parentFile, "$name.json.tmp")
            temp.writeText(sharedToJson(merged).toString())
            temp.renameTo(file)
        }
        merged to changed
    }

    private fun dir(context: Context): File = File(context.filesDir, "shared").apply { mkdirs() }
}

// Records without an id or a time are dropped.
fun parseShared(array: JSONArray): List<SharedRecord> = List(array.length()) { array.optJSONObject(it) }.mapNotNull { record ->
    val id = record?.optString("id").orEmpty()
    if (id.isEmpty() || !record!!.has("at")) null else SharedRecord(id, record.optLong("at"), record.toString())
}

fun sharedToJson(records: List<SharedRecord>): JSONArray = JSONArray(records.map { JSONObject(it.json) })
