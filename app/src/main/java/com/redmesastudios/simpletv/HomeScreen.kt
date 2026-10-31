package com.redmesastudios.simpletv

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.redmesastudios.simpletv.ui.theme.BannerFallbacks
import com.redmesastudios.simpletv.ui.theme.Ground
import com.redmesastudios.simpletv.ui.theme.Ink
import com.redmesastudios.simpletv.ui.theme.InkSoft
import com.redmesastudios.simpletv.ui.theme.OnInk
import com.redmesastudios.simpletv.ui.theme.Outline
import com.redmesastudios.simpletv.ui.theme.SimpleTVType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val GridColumns = 5
const val MoreTile = "more"

// One page of the grid as shown: its place among the pages, its apps, and whether More follows.
private data class ShownPage(val index: Int, val apps: List<StreamingApp>, val hasMore: Boolean)
private val TileCorner = 8.dp
private val TileShape = RoundedCornerShape(TileCorner)
internal val TimeFormat = DateTimeFormatter.ofPattern("h:mm")
private val DateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d")

// returnFocusTo names the app whose folder was just closed, so focus lands back on its tile;
// otherwise the top-left app takes focus. arrival changes with every Home press, which sends
// focus back to the start. Pass SearchButtonFocus as returnFocusTo to land on the Search button,
// or glanceFocus(channel) for a glance card.
const val SearchButtonFocus = "search"
fun glanceFocus(channel: Channel) = "glance:${channel.name}"

@Composable
fun HomeScreen(
    returnFocusTo: String?,
    arrival: Int,
    onOpenFolder: (StreamingApp) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenChannel: (Channel) -> Unit,
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<StreamingApp>()) }
    // The focused tile: an app's package name, or MoreTile.
    var focusedTile by remember { mutableStateOf<String?>(null) }
    val searchButton = remember { FocusRequester() }
    val now by rememberMinuteClock()

    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val glanceCards = remember { Channel.entries.associateWith { FocusRequester() } }
    LaunchedEffect(windowFocused) {
        if (!windowFocused) return@LaunchedEffect
        if (returnFocusTo == SearchButtonFocus) searchButton.requestFocus()
        Channel.entries.firstOrNull { glanceFocus(it) == returnFocusTo }?.let { runCatching { glanceCards.getValue(it).requestFocus() } }
    }

    // Re-check on every return to the home screen, so installs and removals show up,
    // most-watched first.
    LifecycleResumeEffect(Unit) {
        apps = context.homeOrder(homeApps(context.installedTvApps()))
        onPauseOrDispose { }
    }

    // Banners decode off the main thread; tiles show their fallback color until then.
    val art by produceState(emptyMap<String, AppArt?>(), apps) {
        value = withContext(Dispatchers.Default) {
            apps.associate { it.packageName to context.packageManager.appArt(it.packageName) }
        }
    }

    // Weather, news and markets, kept fresh while the home screen is up.
    val glance by GlanceStore.glance.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                GlanceStore.refreshIfStale()
                delay(60_000)
            }
        }
    }

    // Apps come ten tiles to a page; a page with more after it gives its last tile to More.
    val pages = remember(apps) { homePages(apps) }
    var page by remember { mutableIntStateOf(0) }
    // Which tile the grid should land on: the app just returned to, then wherever paging moves it.
    var gridFocus by remember { mutableStateOf(returnFocusTo) }
    LaunchedEffect(pages) {
        page = pages.indexOfFirst { onPage -> onPage.any { it.packageName == gridFocus } }.coerceAtLeast(0)
    }
    val firstArrival = remember { arrival }
    LaunchedEffect(arrival) {
        if (arrival != firstArrival) {
            page = 0
            gridFocus = null
        }
    }

    // Holding OK on an app tile offers to remove it. The prompt takes no focus; the keys
    // below steer it, and the held OK's release is swallowed so it can't answer the prompt.
    var removing by remember { mutableStateOf<StreamingApp?>(null) }
    var removeChoice by remember { mutableStateOf(RemoveChoice.Cancel) }
    var swallowRelease by remember { mutableStateOf(false) }
    val builtIn = remember(removing) { removing?.let { context.cameWithTv(it) } ?: false }
    fun answerRemove(event: KeyEvent): Boolean {
        val select = event.keyCode in SelectKeys
        if (swallowRelease && select) {
            if (event.action == KeyEvent.ACTION_UP) swallowRelease = false
            return true
        }
        val app = removing ?: return false
        val handled = select || event.keyCode in setOf(
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_BACK,
        )
        if (!handled || event.action != KeyEvent.ACTION_UP) return handled
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> removeChoice = RemoveChoice.Remove
            KeyEvent.KEYCODE_DPAD_RIGHT -> removeChoice = RemoveChoice.Cancel
            KeyEvent.KEYCODE_BACK -> removing = null
            in SelectKeys -> {
                removing = null
                if (removeChoice == RemoveChoice.Remove) context.removeApp(app)
            }
        }
        return true
    }

    // This is the home screen: Back has nowhere further to go, except back a page.
    BackHandler { }
    BackHandler(enabled = page > 0) {
        page -= 1
        gridFocus = MoreTile
    }

    Box(Modifier.fillMaxSize().background(Ground).onPreviewKeyEvent { answerRemove(it.nativeKeyEvent) }) {
        FallScene(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(start = 60.dp, end = 60.dp, top = 32.dp, bottom = 28.dp)) {
            Header(
                now = now,
                searchButton = searchButton,
                onOpenApps = { context.openAppStore() },
                onOpenSearch = onOpenSearch,
                onOpenSettings = { context.openSystemSettings() },
            )
            Spacer(Modifier.height(20.dp))
            // More slides the next page in from the right, Back slides it out again; when the
            // daily re-sort moves tiles, the grid fades to its new order.
            AnimatedContent(
                targetState = ShownPage(page, pages.getOrElse(page) { emptyList() }, hasMore = page < pages.lastIndex),
                transitionSpec = {
                    when {
                        targetState.index > initialState.index ->
                            (slideInHorizontally(tween(350)) { it } + fadeIn(tween(350))) togetherWith
                                (slideOutHorizontally(tween(350)) { -it } + fadeOut(tween(350)))
                        targetState.index < initialState.index ->
                            (slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350))) togetherWith
                                (slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350)))
                        else -> fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    }
                },
                label = "appPage",
            ) { shown ->
                AppGrid(
                    page = shown,
                    art = art,
                    focusedTile = focusedTile,
                    initialFocus = gridFocus,
                    arrival = arrival,
                    aboveGrid = searchButton,
                    onFocused = { focusedTile = it },
                    onOpen = { if (it.hasFolder) onOpenFolder(it) else context.launchApp(it) },
                    onHold = {
                        removeChoice = RemoveChoice.Cancel
                        swallowRelease = true
                        removing = it
                    },
                    onMore = {
                        gridFocus = pages.getOrNull(shown.index + 1)?.firstOrNull()?.packageName
                        page = shown.index + 1
                    },
                )
            }
            Spacer(Modifier.weight(1f))
            GlanceCards(glance, glanceCards, onOpenChannel)
        }
        RemoveAppPrompt(removing, builtIn, removeChoice)
    }
}

@Composable
private fun Header(
    now: LocalDateTime,
    searchButton: FocusRequester,
    onOpenApps: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            BasicText(greetingFor(now.hour), style = SimpleTVType.greeting)
            BasicText(now.format(DateFormat), style = SimpleTVType.date)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            BasicText(now.format(TimeFormat), style = SimpleTVType.clock)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderButton("Get Apps", Modifier, onOpenApps)
                HeaderButton("Search", Modifier.focusRequester(searchButton), onOpenSearch)
                HeaderButton("Settings", Modifier, onOpenSettings)
            }
        }
    }
}

// Quiet by default so they don't compete with the apps; reached by pressing Up.
@Composable
private fun HeaderButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, tween(200), label = "headerScale")
    val container by animateColorAsState(if (focused) Ink else Color.Transparent, tween(200), label = "headerColor")
    val content by animateColorAsState(if (focused) OnInk else InkSoft, tween(200), label = "headerText")
    val border by animateColorAsState(if (focused) Ink else Outline, tween(200), label = "headerBorder")

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(container, CircleShape)
            .border(1.dp, border, CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 5.dp),
    ) {
        BasicText(label, style = SimpleTVType.hint, color = { content })
    }
}

@Composable
private fun AppGrid(
    page: ShownPage,
    art: Map<String, AppArt?>,
    focusedTile: String?,
    initialFocus: String?,
    arrival: Int,
    aboveGrid: FocusRequester,
    onFocused: (String) -> Unit,
    onOpen: (StreamingApp) -> Unit,
    onHold: (StreamingApp) -> Unit,
    onMore: () -> Unit,
) {
    val tiles = page.apps.map { it.packageName } + if (page.hasMore) listOf(MoreTile) else emptyList()
    // Start on a tile once the window can take focus, so the remote always has
    // something highlighted: the one asked for, else the top-left one.
    val startTile = remember { FocusRequester() }
    val startIndex = tiles.indexOf(initialFocus).coerceAtLeast(0)
    val startKey = tiles.getOrNull(startIndex)
    val currentFocus by rememberUpdatedState(focusedTile)
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(startKey, windowFocused, arrival) {
        val elsewhere = initialFocus == SearchButtonFocus || initialFocus?.startsWith("glance:") == true
        if (elsewhere || startKey == null || !windowFocused) return@LaunchedEffect
        // The grid may still be sliding or fading in, or something else may grab focus first,
        // so keep asking for a moment until the tile holds it.
        repeat(20) {
            runCatching { startTile.requestFocus() }
            delay(50)
            if (currentFocus == startKey) return@LaunchedEffect
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        tiles.chunked(GridColumns).forEachIndexed { rowIndex, row ->
            // The focused tile grows past its neighbors, so its row draws on top.
            Row(
                Modifier.zIndex(if (focusedTile in row) 1f else 0f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                row.forEachIndexed { columnIndex, key ->
                    val index = rowIndex * GridColumns + columnIndex
                    val modifier = Modifier
                        .weight(1f)
                        .then(if (index == startIndex) Modifier.focusRequester(startTile) else Modifier)
                        // Up from anywhere in the top row reaches Settings, not just
                        // from the tiles that happen to sit beneath it.
                        .then(if (rowIndex == 0) Modifier.focusProperties { up = aboveGrid } else Modifier)
                    val app = page.apps.getOrNull(index)
                    if (app == null) {
                        MoreAppsTile(modifier, onFocused = { onFocused(MoreTile) }, onClick = onMore)
                    } else {
                        AppTile(
                            app = app,
                            art = art[app.packageName],
                            fallback = BannerFallbacks[index % BannerFallbacks.size],
                            modifier = modifier,
                            onFocused = { onFocused(app.packageName) },
                            onClick = { onOpen(app) },
                            onHold = { onHold(app) },
                        )
                    }
                }
                repeat(GridColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// The last tile of a page with more apps after it.
@Composable
private fun MoreAppsTile(modifier: Modifier, onFocused: () -> Unit, onClick: () -> Unit) {
    TileFrame(modifier, Ink, onFocused, onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText("More apps", style = SimpleTVType.tile.copy(fontSize = 19.sp), color = { OnInk })
            BasicText("→", style = SimpleTVType.tile.copy(fontSize = 22.sp), color = { OnInk })
        }
    }
}

// The app's own TV banner, which carries the service's logo. Apps without one get their
// icon and name on a warm color.
@Composable
private fun AppTile(
    app: StreamingApp,
    art: AppArt?,
    fallback: Color,
    modifier: Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onHold: () -> Unit,
) {
    TileFrame(modifier, fallback, onFocused, onClick, onHold) {
        when (art) {
            is AppArt.Banner -> Image(art.image, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            is AppArt.Icon -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Image(art.image, contentDescription = null, Modifier.size(30.dp))
                BasicText(app.label, style = SimpleTVType.tile, color = { OnInk })
            }
            null -> BasicText(app.label, style = SimpleTVType.tile, color = { OnInk })
        }
    }
}

// A 16:9 tile that grows, lifts and rings when focused.
@Composable
private fun TileFrame(
    modifier: Modifier,
    fill: Color,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onHold: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(200), label = "tileScale")
    val elevation by animateDpAsState(if (focused) 12.dp else 0.dp, tween(200), label = "tileElevation")
    val ring by animateColorAsState(if (focused) Ink else Ink.copy(alpha = 0f), tween(200), label = "tileRing")

    Box(
        modifier
            .zIndex(if (focused) 1f else 0f)
            .aspectRatio(16f / 9f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            // Focus ring just outside the banner, with a sliver of background between.
            .drawBehind {
                val stroke = 2.dp.toPx()
                val outset = 2.dp.toPx() + stroke / 2
                drawRoundRect(
                    color = ring,
                    topLeft = Offset(-outset, -outset),
                    size = Size(size.width + 2 * outset, size.height + 2 * outset),
                    cornerRadius = CornerRadius(TileCorner.toPx() + outset),
                    style = Stroke(stroke),
                )
            }
            .shadow(elevation, TileShape, ambientColor = Ink, spotColor = Ink)
            .clip(TileShape)
            .background(fill)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                val hold = onHold?.takeIf { isHoldToRemove(event.nativeKeyEvent) } ?: return@onPreviewKeyEvent false
                hold()
                true
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// Current time, refreshed on each minute boundary.
@Composable
internal fun rememberMinuteClock(): State<LocalDateTime> {
    val now = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = LocalDateTime.now()
            now.value = current
            delay(60_000L - current.second * 1_000L - current.nano / 1_000_000L)
        }
    }
    return now
}
