package com.redmesastudios.simpletv

import java.time.LocalDate
import java.time.format.DateTimeFormatter

// What a folder shows: rows of titles for one service, built from the show guides.

enum class TitleKind { Show, Movie, Video }

data class Title(
    val tmdbId: Int,
    val kind: TitleKind,
    val name: String,
    val overview: String = "",
    val year: String = "",
    val backdropPath: String? = null,
    val seasons: Int? = null,
    val popularity: Double = 0.0,
    // A short line under the card, like "Season 4 · Nov 1".
    val note: String = "",
    // Set for Netflix's own Top 10, which links straight into Netflix.
    val netflixId: String? = null,
    // Set for YouTube videos; their artwork comes from YouTube rather than TMDB.
    val videoId: String? = null,
    // TMDB's average score out of 10; 0 when unknown.
    val rating: Double = 0.0,
) {
    // Identifies the title within a row or a watch history.
    val key: String get() = videoId ?: "$kind:$tmdbId"

    // The artwork at card size or at full size, wherever it comes from.
    fun imageUrl(large: Boolean): String? = when {
        videoId != null -> if (large) backdropPath ?: youTubeThumbnail(videoId, "hqdefault") else youTubeThumbnail(videoId, "mqdefault")
        else -> backdropPath?.let { tmdbImage(it, if (large) "w1280" else "w300") }
    }
}

data class FolderRow(val heading: String, val titles: List<Title>, val ranked: Boolean = false)

// One title arriving on (or coming to) a service, from Watchmode's release calendar.
data class Release(val sourceId: Int, val tmdbId: Int, val kind: TitleKind, val season: Int?, val date: LocalDate)

data class NetflixRank(val rank: Int, val name: String, val netflixId: String)

data class Person(val id: Int, val name: String, val photoPath: String?, val knownFor: String)

data class SearchResults(val shows: List<Title>, val movies: List<Title>, val people: List<Person>)

// One place a title can be watched: streaming (with or without ads, or free) or rent/buy.
data class Offer(val providerId: Int, val providerName: String, val rentOrBuy: Boolean)

// Where a title can be watched, from this TV's point of view.
data class WhereToWatch(
    // Apps on this TV that stream it.
    val here: List<StreamingApp>,
    // Services that stream it but aren't on this TV, by name.
    val elsewhere: List<String>,
    // Apps on this TV that rent or sell it.
    val rentHere: List<StreamingApp>,
) {
    // The line under a search card.
    val summary: String
        get() = when {
            here.isNotEmpty() -> "On " + here.joinToString(" · ") { it.label }
            elsewhere.isNotEmpty() -> "On " + elsewhere.take(2).joinToString(" · ") + " · not on this TV"
            rentHere.isNotEmpty() -> "Rent or buy on " + rentHere.first().label
            else -> "Not streaming right now"
        }
}

fun whereToWatch(offers: List<Offer>, installed: List<StreamingApp>): WhereToWatch {
    fun appFor(offer: Offer) = installed.firstOrNull { offer.providerId in it.tmdbProviders }
    val streaming = offers.filterNot { it.rentOrBuy }
    return WhereToWatch(
        here = streaming.mapNotNull(::appFor).distinct(),
        elsewhere = streaming.filter { appFor(it) == null }.map { it.providerName }.distinct(),
        rentHere = offers.filter { it.rentOrBuy }.mapNotNull(::appFor).distinct(),
    )
}

private val ShortDate = DateTimeFormatter.ofPattern("MMM d")

// Movies and brand-new shows, newest first.
fun justAdded(releases: List<Release>): List<Release> =
    releases.filter { it.kind == TitleKind.Movie || (it.season ?: 1) <= 1 }
        .distinctBy { it.kind to it.tmdbId }
        .sortedByDescending { it.date }

// A returning show's next season, either just out or on its way.
fun newSeasons(releases: List<Release>, upcoming: Boolean): List<Release> =
    releases.filter { it.kind == TitleKind.Show && (it.season ?: 0) > 1 }
        .distinctBy { it.tmdbId }
        .let { if (upcoming) it.sortedBy { r -> r.date } else it.sortedByDescending { r -> r.date } }

fun releaseNote(release: Release, upcoming: Boolean): String {
    val date = release.date.format(ShortDate)
    val season = release.season?.takeIf { release.kind == TitleKind.Show && it > 1 }
    return when {
        season != null && upcoming -> "Season $season · $date"
        season != null -> "Season $season"
        else -> "Added $date"
    }
}

// Netflix's Top 10 page lists each title as an image (its name in alt text), a play
// link carrying the Netflix ID, then its rank, in that order.
fun parseNetflixTop10(html: String): List<NetflixRank> {
    data class Mark(val at: Int, val kind: Char, val value: String)
    val marks = buildList {
        Regex("""<img[^>]*\balt="([^"]+)"""").findAll(html).forEach { add(Mark(it.range.first, 'a', it.groupValues[1])) }
        Regex("""href="https://www\.netflix\.com/watch/(\d+)""").findAll(html).forEach { add(Mark(it.range.first, 'w', it.groupValues[1])) }
        Regex("""#(\d+) in (?:Shows|Movies|Films)""").findAll(html).forEach { add(Mark(it.range.first, 'r', it.groupValues[1])) }
    }.sortedBy { it.at }

    val ranks = linkedMapOf<Int, NetflixRank>()
    var name: String? = null
    var id: String? = null
    for (mark in marks) when (mark.kind) {
        'a' -> name = mark.value
        'w' -> id = mark.value
        'r' -> {
            val rank = mark.value.toInt()
            val title = name
            val netflixId = id
            if (title != null && netflixId != null && rank !in ranks) {
                ranks[rank] = NetflixRank(rank, cleanNetflixName(title), netflixId)
            }
            id = null
        }
    }
    return ranks.values.sortedBy { it.rank }
}

// "Nobody Wants This: Season 3" -> "Nobody Wants This", so the guide can find the show.
fun cleanNetflixName(raw: String): String {
    val decoded = raw.replace("&#x27;", "'").replace("&#39;", "'").replace("&quot;", "\"").replace("&amp;", "&")
    return decoded.replace(Regex("""\s*:\s*(Season \d+|Limited Series|Part \d+|Volume \d+|S\d+\b.*|\d{4}\s*-.*)$""", RegexOption.IGNORE_CASE), "").trim()
}
