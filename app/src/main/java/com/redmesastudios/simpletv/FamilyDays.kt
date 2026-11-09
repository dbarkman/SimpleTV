package com.redmesastudios.simpletv

import java.time.LocalDate
import java.time.MonthDay

// The household's birthdays and anniversaries: birthdays.txt in its settings folder
// (BuildConfig.FAMILY_DAYS) seeds them, and the phone settings page changes them
// (HouseSettings.kt). On the day, the greeting wishes them well and the scene
// celebrates, whatever holiday is on. Lines are "MM-DD birthday Name" or
// "MM-DD anniversary Name & Name"; # starts a comment.

enum class FamilyDayKind { Birthday, Anniversary }

data class FamilyDay(val day: MonthDay, val kind: FamilyDayKind, val name: String)

fun parseFamilyDays(text: String): List<FamilyDay> = text.lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .mapNotNull { line ->
        val parts = line.split(Regex("\\s+"), limit = 3)
        if (parts.size < 3) return@mapNotNull null
        val day = runCatching { MonthDay.parse("--${parts[0]}") }.getOrNull() ?: return@mapNotNull null
        val kind = FamilyDayKind.entries.firstOrNull { it.name.equals(parts[1], ignoreCase = true) } ?: return@mapNotNull null
        FamilyDay(day, kind, parts[2].trim())
    }
    .toList()

// The family days on a date. A February 29th one is kept on the 28th in other years.
fun familyDaysOn(date: LocalDate, days: List<FamilyDay>): List<FamilyDay> = days.filter {
    it.day == MonthDay.from(date) ||
        (it.day == MonthDay.of(2, 29) && !date.isLeapYear && MonthDay.from(date) == MonthDay.of(2, 28))
}

// "Happy birthday, Alex!", "Happy birthday, Alex and Sam!", or for an anniversary
// "Happy anniversary, Pat & Jo!". Birthdays come first when both fall on one day.
fun celebrationGreeting(days: List<FamilyDay>): String? {
    val birthdays = days.filter { it.kind == FamilyDayKind.Birthday }.map { it.name }
    val anniversaries = days.filter { it.kind == FamilyDayKind.Anniversary }.map { it.name }
    return when {
        birthdays.isNotEmpty() -> "Happy birthday, ${joinNames(birthdays)}!"
        anniversaries.isNotEmpty() -> "Happy anniversary, ${joinNames(anniversaries)}!"
        else -> null
    }
}

private fun joinNames(names: List<String>): String = when (names.size) {
    1 -> names[0]
    2 -> "${names[0]} and ${names[1]}"
    else -> names.dropLast(1).joinToString(", ") + " and " + names.last()
}
