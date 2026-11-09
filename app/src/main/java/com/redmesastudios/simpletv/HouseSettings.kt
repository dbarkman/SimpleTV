package com.redmesastudios.simpletv

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.MonthDay
import java.util.UUID

// The household's own settings, changed from a phone (SettingsServer.kt): its birthdays and
// anniversaries, which At a glance cards show, and which holiday packs decorate. They're
// records in the shared box (SharedRecords.kt, collection "house"), so they spread to the
// household's other sticks like the games' records, and the newest change to each wins.
//
// The build's birthdays.txt seeds the list: each of its lines becomes a record dated 0,
// added only where no record of that id exists yet, so anything changed or removed from a
// phone stays that way, and a line added to the file later still arrives with the next
// update.

const val HouseCollection = "house"
private const val CardsId = "cards"
private const val PacksId = "packs"
private const val FamilyPrefix = "family:"

data class StoredFamilyDay(val id: String, val day: FamilyDay)

data class HouseState(
    val familyDays: List<StoredFamilyDay>,
    val hiddenCards: Set<Channel>,
    val packs: Set<Pack>,
)

// The seed id for a line of birthdays.txt: the same on every stick, from the line itself.
fun seedId(day: FamilyDay): String {
    val line = "${familyDayText(day.day)} ${day.kind.name.lowercase()} ${day.name}"
    val hash = MessageDigest.getInstance("SHA-256").digest(line.toByteArray()).joinToString("") { "%02x".format(it) }
    return "${FamilyPrefix}seed-${hash.take(16)}"
}

fun familyDayText(day: MonthDay): String = "%02d-%02d".format(day.monthValue, day.dayOfMonth)

fun seedRecords(baked: List<FamilyDay>): List<SharedRecord> = baked.map { familyRecord(seedId(it), it, at = 0) }

fun familyRecord(id: String, day: FamilyDay, at: Long): SharedRecord = record(
    JSONObject()
        .put("id", id)
        .put("at", at)
        .put("date", familyDayText(day.day))
        .put("kind", day.kind.name.lowercase())
        .put("name", day.name),
)

fun removedRecord(id: String, at: Long): SharedRecord = record(JSONObject().put("id", id).put("at", at).put("removed", true))

fun cardsRecord(hidden: Set<Channel>, at: Long): SharedRecord =
    record(JSONObject().put("id", CardsId).put("at", at).put("hidden", JSONArray(hidden.map { it.name.lowercase() }.sorted())))

fun packsRecord(packs: Set<Pack>, at: Long): SharedRecord =
    record(JSONObject().put("id", PacksId).put("at", at).put("packs", JSONArray(packs.map { it.key })))

private fun record(json: JSONObject) = SharedRecord(json.getString("id"), json.getLong("at"), json.toString())

// The settings the records add up to, with the build's packs where none were chosen.
fun houseState(records: List<SharedRecord>, defaultPacks: Set<Pack>): HouseState {
    val byId = records.associate { it.id to JSONObject(it.json) }
    val family = byId.filterKeys { it.startsWith(FamilyPrefix) }
        .filterValues { !it.optBoolean("removed") }
        .mapNotNull { (id, json) ->
            parseFamilyDays("${json.optString("date")} ${json.optString("kind")} ${json.optString("name")}").singleOrNull()
                ?.let { StoredFamilyDay(id, it) }
        }
        .sortedWith(compareBy({ it.day.day }, { it.day.name }))
    val hidden = byId[CardsId]?.optJSONArray("hidden")?.let { array ->
        (0 until array.length()).mapNotNull { i -> Channel.entries.firstOrNull { it.name.equals(array.optString(i), ignoreCase = true) } }.toSet()
    }.orEmpty()
    val packs = byId[PacksId]?.optJSONArray("packs")?.let { array ->
        (0 until array.length()).mapNotNull { i -> Pack.entries.firstOrNull { it.key == array.optString(i) } }.toSet()
    } ?: defaultPacks
    return HouseState(family, hidden, packs)
}

object HouseSettings {
    private val defaultPacks get() = parsePacks(BuildConfig.HOLIDAY_PACKS)
    private val mutableState = MutableStateFlow(houseState(seedRecords(parseFamilyDays(BuildConfig.FAMILY_DAYS)), parsePacks(BuildConfig.HOLIDAY_PACKS)))
    val state: StateFlow<HouseState> = mutableState.asStateFlow()

    // On start: adds the build's birthdays where they're new, then reads the settings.
    fun load(context: Context) {
        SharedRecords.merge(context, HouseCollection, seedRecords(parseFamilyDays(BuildConfig.FAMILY_DAYS)))
        refresh(context)
    }

    // Called whenever the house records change, here or from another stick.
    fun refresh(context: Context) {
        mutableState.value = houseState(SharedRecords.read(context, HouseCollection), defaultPacks)
    }

    fun addFamilyDay(context: Context, day: FamilyDay) =
        save(context, familyRecord(FamilyPrefix + UUID.randomUUID(), day, System.currentTimeMillis()))

    fun removeFamilyDay(context: Context, id: String) {
        if (id.startsWith(FamilyPrefix)) save(context, removedRecord(id, System.currentTimeMillis()))
    }

    fun setHiddenCards(context: Context, hidden: Set<Channel>) = save(context, cardsRecord(hidden, System.currentTimeMillis()))

    fun setPacks(context: Context, packs: Set<Pack>) = save(context, packsRecord(packs, System.currentTimeMillis()))

    private fun save(context: Context, record: SharedRecord) {
        val (_, changed) = SharedRecords.merge(context, HouseCollection, listOf(record))
        if (changed) Sync.changed(context)
    }
}
