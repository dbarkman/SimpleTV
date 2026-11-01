package com.redmesastudios.simpletv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.redmesastudios.simpletv.ui.theme.Ground
import com.redmesastudios.simpletv.ui.theme.Ink
import com.redmesastudios.simpletv.ui.theme.OnInk
import com.redmesastudios.simpletv.ui.theme.Outline
import com.redmesastudios.simpletv.ui.theme.RankInk
import com.redmesastudios.simpletv.ui.theme.SimpleTVType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val GridColumns = 5
private val CardWidth = 156.dp

// One sort and genre's list, loaded a page at a time.
private class MovieList(sort: MovieSort, genre: Genre?, today: LocalDate) {
    val pager = MoviePager(sort, genre?.id, today)
    val movies = mutableStateListOf<HomeMovie>()
    var loading by mutableStateOf(false)
    var failed by mutableStateOf(false)

    suspend fun loadMore() {
        if (loading || pager.finished) return
        loading = true
        runCatching { pager.next() }
            .onSuccess { page ->
                val known = movies.map { it.title.tmdbId }.toSet()
                movies.addAll(page.filter { it.title.tmdbId !in known })
                failed = false
            }
            .onFailure { failed = true }
        loading = false
    }
}

// Every movie out to watch at home: sort and genre buttons along the top, the focused
// movie's details below them, and the movies in a grid that keeps loading as it scrolls.
@Composable
fun MoviesScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val installed = remember { homeApps(context.installedTvApps()).map(ServiceDirectory.services.value::resolve) }
    val genres by produceState(emptyList<Genre>()) { value = runCatching { Tmdb.movieGenres() }.getOrDefault(emptyList()) }

    var sort by remember { mutableStateOf(MovieSort.Newest) }
    var genre by remember { mutableStateOf<Genre?>(null) }
    val list = remember(sort, genre) { MovieList(sort, genre, today) }
    val grid = remember(sort, genre) { LazyGridState() }
    val info = remember { mutableStateMapOf<Int, MovieInfo>() }
    val requested = remember { HashSet<Int>() }
    var focusedId by remember(sort, genre) { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val firstCard = remember { FocusRequester() }
    var landed by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onClose)

    // The first page, then the next whenever the grid nears its end.
    LaunchedEffect(list) {
        list.loadMore()
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (last >= list.movies.size - GridColumns * 2) list.loadMore() }
    }

    // Details and scores for every movie in the list, filled in as they arrive.
    LaunchedEffect(list, list.movies.size) {
        list.movies.map { it.title.tmdbId }.filter { it !in requested }.forEach { id ->
            requested += id
            scope.launch { MovieInfoStore.get(context, id)?.let { info[id] = it } }
        }
    }

    // Land on the first movie once there is one, the first time only; after that, a new
    // sort or genre keeps focus on its button.
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocused, list.movies.isNotEmpty()) {
        if (!windowFocused || list.movies.isEmpty() || landed > 0) return@LaunchedEffect
        repeat(20) {
            if (runCatching { firstCard.requestFocus() }.isSuccess) {
                landed += 1
                return@LaunchedEffect
            }
            delay(50)
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(5_000)
            message = null
        }
    }

    fun watchOf(movie: HomeMovie) = info[movie.title.tmdbId]?.let { movieWatch(it.facts.offers, installed) }

    fun open(movie: HomeMovie) {
        when (val watch = watchOf(movie)) {
            null -> message = "Still finding where to watch ${movie.title.name}…"
            is MovieWatch.Elsewhere, MovieWatch.Nowhere -> message = "${movie.title.name}: ${watch.sentence()}."
            else -> scope.launch { context.watchMovie(watch, movie.title) }
        }
    }

    val shown = list.movies.firstOrNull { it.title.tmdbId == focusedId } ?: list.movies.firstOrNull()

    Box(Modifier.fillMaxSize().background(Ground)) {
        FallScene(Modifier.fillMaxSize())
        Backdrop(shown?.title, Modifier.align(Alignment.TopEnd))
        Column(Modifier.fillMaxSize().padding(top = 30.dp)) {
            Row(
                Modifier.padding(start = ScreenMargin),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText("Movies", style = SimpleTVType.serviceName, modifier = Modifier.padding(end = 10.dp))
                MovieSort.entries.forEach { option ->
                    FilterPill(option.label, selected = option == sort) { sort = option }
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = ScreenMargin, end = ScreenMargin, top = 10.dp, bottom = 4.dp),
            ) {
                item(key = "all") { FilterPill("All genres", selected = genre == null) { genre = null } }
                items(genres, key = { it.id }) { option ->
                    FilterPill(option.name, selected = option == genre) { genre = option }
                }
            }
            MovieHero(
                movie = shown,
                info = shown?.let { info[it.title.tmdbId] },
                watch = shown?.let(::watchOf),
                emptyMessage = when {
                    list.failed -> "Movies aren't reachable right now. Check the internet and try again."
                    list.loading || list.movies.isNotEmpty() -> "Finding movies…"
                    else -> "No movies here yet."
                },
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(GridColumns),
                state = grid,
                modifier = Modifier
                    .weight(1f)
                    // The scene's holiday decorations would show through behind the card titles.
                    .background(Brush.verticalGradient(0f to Ground.copy(alpha = 0f), 0.12f to Ground.copy(alpha = 0.92f)))
                    // Rows scrolled partly out of view fade away at the top instead of being cut.
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.05f to Color.Black), blendMode = BlendMode.DstIn)
                    },
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = ScreenMargin, end = ScreenMargin, top = 8.dp, bottom = 40.dp),
            ) {
                itemsIndexed(list.movies, key = { _, movie -> movie.title.tmdbId }) { index, movie ->
                    val note = movieNote(movie, info[movie.title.tmdbId], watchOf(movie), today)
                    ShowCard(
                        title = movie.title.copy(note = note),
                        width = CardWidth,
                        modifier = if (index == 0) Modifier.focusRequester(firstCard) else Modifier,
                        onFocused = { focusedId = it.tmdbId },
                        onOpen = { open(movie) },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp),
        ) {
            BasicText(
                message.orEmpty(),
                style = SimpleTVType.heroBody.copy(color = OnInk, textAlign = TextAlign.Center),
                modifier = Modifier.background(Ink, CircleShape).padding(horizontal = 22.dp, vertical = 10.dp),
            )
        }
    }
}

// The focused movie: name, year, length, rating and genres, its scores, the start of its
// story, and what OK does.
@Composable
private fun MovieHero(movie: HomeMovie?, info: MovieInfo?, watch: MovieWatch?, emptyMessage: String) {
    AnimatedContent(
        targetState = movie,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
        modifier = Modifier.padding(start = ScreenMargin, top = 16.dp).width(440.dp).height(168.dp),
        contentKey = { it?.title?.tmdbId },
        label = "movieHero",
    ) { shownMovie ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (shownMovie == null) {
                BasicText(emptyMessage, style = SimpleTVType.heroBody)
                return@Column
            }
            BasicText(shownMovie.title.name, style = SimpleTVType.heroTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(movieMeta(info, shownMovie), style = SimpleTVType.heroMeta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // The details arrive a moment after the movie does, so they fade in rather than pop.
            AnimatedContent(
                targetState = info?.scores?.line.orEmpty(),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "movieScores",
            ) { line -> BasicText(line, style = SimpleTVType.heroMeta.copy(color = Ink), modifier = Modifier.height(17.dp)) }
            BasicText(shownMovie.title.overview, style = SimpleTVType.heroBody, maxLines = 2, overflow = TextOverflow.Ellipsis)
            AnimatedContent(
                targetState = watch?.sentence().orEmpty(),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "movieWatch",
            ) { sentence ->
                Row(Modifier.padding(top = 2.dp).height(22.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (sentence.isNotEmpty()) {
                        Box(Modifier.size(22.dp).border(1.dp, Outline, CircleShape), contentAlignment = Alignment.Center) {
                            BasicText("OK", style = SimpleTVType.okBadge)
                        }
                        BasicText(sentence, style = SimpleTVType.heroMeta)
                    }
                }
            }
        }
    }
}

// A sort or genre button. The chosen one is filled; the focused one is dark.
@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, tween(200), label = "pillScale")
    val container by animateColorAsState(
        when {
            focused -> Ink
            selected -> RankInk
            else -> Ground.copy(alpha = 0.85f)
        },
        tween(200),
        label = "pillColor",
    )
    val content by animateColorAsState(if (focused) OnInk else Ink, tween(200), label = "pillText")
    val border by animateColorAsState(if (focused || selected) Ink else Outline, tween(200), label = "pillBorder")
    Box(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(container, CircleShape)
            .border(1.dp, border, CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        BasicText(label, style = SimpleTVType.button, color = { content })
    }
}
