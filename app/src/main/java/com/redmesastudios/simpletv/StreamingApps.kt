package com.redmesastudios.simpletv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

// tmdbProviders and watchmodeSources name the service in each show guide (US region).
// A service can appear under several IDs (with-ads tiers, free tiers), so both are sets.
// Apps with both get a folder of shows from the guides; YouTube's folder comes from
// YouTube itself; the rest open straight away. tmdbProviders alone lets search say a
// title is on that app.
data class StreamingApp(
    val label: String,
    val packageName: String,
    val tmdbProviders: Set<Int> = emptySet(),
    val watchmodeSources: Set<Int> = emptySet(),
) {
    val hasGuide: Boolean get() = tmdbProviders.isNotEmpty() && watchmodeSources.isNotEmpty()
    val hasFolder: Boolean get() = hasGuide || packageName == YouTubePackage
}

// Home-screen order before there's any watch time. Apps that aren't installed are skipped.
const val YouTubePackage = "com.google.android.youtube.tv"

val StreamingApps = listOf(
    StreamingApp("YouTube", YouTubePackage, tmdbProviders = setOf(192, 188)),
    StreamingApp("Netflix", "com.netflix.ninja", tmdbProviders = setOf(8, 1796), watchmodeSources = setOf(203)),
    StreamingApp("Disney+", "com.disney.disneyplus", tmdbProviders = setOf(337), watchmodeSources = setOf(372)),
    StreamingApp("Paramount+", "com.cbs.ott", tmdbProviders = setOf(2303, 2616), watchmodeSources = setOf(444)),
    StreamingApp("HBO Max", "com.wbd.stream", tmdbProviders = setOf(1899), watchmodeSources = setOf(387)),
    StreamingApp("Prime Video", "com.amazon.amazonvideo.livingroom", tmdbProviders = setOf(9, 2100), watchmodeSources = setOf(26)),
    StreamingApp("Fandango at Home", "air.com.vudu.air.DownloaderTablet", tmdbProviders = setOf(7, 332)),
    StreamingApp("Apple TV", "com.apple.atve.androidtv.appletv", tmdbProviders = setOf(350), watchmodeSources = setOf(371)),
    StreamingApp("Peacock", "com.peacocktv.peacockandroid", tmdbProviders = setOf(386, 387), watchmodeSources = setOf(388, 389)),
    StreamingApp("PBS", "com.pbs.video", tmdbProviders = setOf(209)),
    StreamingApp("Hulu", "com.hulu.livingroomplus", tmdbProviders = setOf(15), watchmodeSources = setOf(157)),
    StreamingApp("Pandora", "com.pandora.android.atv"),
    StreamingApp("Instagram", "com.instagram.airwave"),
)

fun installedApps(apps: List<StreamingApp>, isInstalled: (String) -> Boolean): List<StreamingApp> =
    apps.filter { isInstalled(it.packageName) }

// Most-watched first. The sort is stable, so ties (including apps never opened)
// keep the curated order.
fun rankByWatchTime(apps: List<StreamingApp>, watchTime: Map<String, Long>): List<StreamingApp> =
    apps.sortedByDescending { watchTime[it.packageName] ?: 0L }

// Lays apps out in a saved order. Apps the saved order doesn't know (newly installed)
// follow in their curated order, and saved apps that are gone drop out.
fun inSavedOrder(apps: List<StreamingApp>, savedOrder: List<String>): List<StreamingApp> {
    val position = savedOrder.withIndex().associate { (index, pkg) -> pkg to index }
    return apps.sortedBy { position[it.packageName] ?: Int.MAX_VALUE }
}

fun PackageManager.tvLaunchIntent(packageName: String): Intent? =
    getLeanbackLaunchIntentForPackage(packageName) ?: getLaunchIntentForPackage(packageName)

fun Context.openSystemSettings() {
    startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

const val PlayStorePackage = "com.android.vending"

// The Play Store, for installing apps; SimpleTV's tiles only cover the curated apps.
fun Context.openAppStore() {
    val intent = packageManager.tvLaunchIntent(PlayStorePackage) ?: return
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun Context.launchApp(app: StreamingApp) {
    val intent = packageManager.tvLaunchIntent(app.packageName) ?: return
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

// Home-screen pages of ten tiles. A page with apps after it shows nine and gives its
// tenth tile to "More"; the last page shows up to ten. Always at least one page.
private const val PageTiles = 10

fun <T> homePages(apps: List<T>): List<List<T>> {
    val pages = mutableListOf<List<T>>()
    var rest = apps
    while (rest.size > PageTiles) {
        pages += rest.take(PageTiles - 1)
        rest = rest.drop(PageTiles - 1)
    }
    pages += rest
    return pages
}
