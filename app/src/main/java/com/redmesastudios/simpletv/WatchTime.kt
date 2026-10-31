package com.redmesastudios.simpletv

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.Duration
import java.time.LocalDate

private const val Prefs = "home"
private const val OrderKey = "order"
private const val OrderDayKey = "orderDay"
private val Window = Duration.ofDays(30)

// The home screen's app order: most-watched over the last 30 days first, worked out at
// most once a day so tiles don't move under someone's thumb.
fun Context.homeOrder(installed: List<StreamingApp>, today: LocalDate = LocalDate.now()): List<StreamingApp> {
    val prefs = getSharedPreferences(Prefs, Context.MODE_PRIVATE)
    val saved = prefs.getString(OrderKey, null)?.split(",")
    if (saved != null && prefs.getLong(OrderDayKey, -1) == today.toEpochDay()) {
        return inSavedOrder(installed, saved)
    }
    val watchTime = watchTimeByApp() ?: return inSavedOrder(installed, saved.orEmpty())
    val ranked = rankByWatchTime(installed, watchTime)
    prefs.edit()
        .putString(OrderKey, ranked.joinToString(",") { it.packageName })
        .putLong(OrderDayKey, today.toEpochDay())
        .apply()
    return ranked
}

// Foreground time per package over the window, or null without usage access.
private fun Context.watchTimeByApp(): Map<String, Long>? {
    val ops = getSystemService(AppOpsManager::class.java)
    val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
    if (mode != AppOpsManager.MODE_ALLOWED) return null
    val now = System.currentTimeMillis()
    val stats = getSystemService(UsageStatsManager::class.java)
        .queryAndAggregateUsageStats(now - Window.toMillis(), now)
    return stats.mapValues { it.value.totalTimeVisible }
}
