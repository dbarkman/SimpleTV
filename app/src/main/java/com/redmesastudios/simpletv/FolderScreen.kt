package com.redmesastudios.simpletv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.redmesastudios.simpletv.ui.theme.Ground
import com.redmesastudios.simpletv.ui.theme.Ink
import com.redmesastudios.simpletv.ui.theme.OnInk
import com.redmesastudios.simpletv.ui.theme.Outline
import com.redmesastudios.simpletv.ui.theme.RankInk
import com.redmesastudios.simpletv.ui.theme.SimpleTVType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val ScreenMargin = 60.dp
private val CardCorner = 8.dp
private val CardShape = RoundedCornerShape(CardCorner)

// A service's folder: the focused show fills the top, rows of shows below, and
// "Open <app>" for anything else. Back returns home.
@Composable
fun FolderScreen(app: StreamingApp, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog by CatalogStore.rows.collectAsState()
    val isYouTube = app.packageName == YouTubePackage
    val youTubeRows by YouTubeStore.rows.collectAsState()
    val youTubeAccount by YouTubeAccount.status.collectAsState()
    // YouTube's folder comes from the family's own subscriptions, once the TV is signed in.
    if (isYouTube) {
        LaunchedEffect(app) {
            YouTubeAccount.check(context)
            YouTubeAccount.signIn(context)
            YouTubeStore.refreshIfStale(context)
        }
    }
    // Read up front so Continue Watching is there on the first frame and focus starts in it.
    var history by remember(app) { mutableStateOf(WatchHistory.forApp(context, app.packageName)) }
    LifecycleResumeEffect(app) {
        history = WatchHistory.forApp(context, app.packageName)
        onPauseOrDispose { }
    }
    val rows = remember(catalog, youTubeRows, history, app) {
        buildList {
            if (history.isNotEmpty()) add(FolderRow("Continue Watching", history))
            addAll(if (isYouTube) youTubeRows else catalog[app.packageName].orEmpty())
        }
    }
    val art by produceState<AppArt?>(null, app) {
        value = withContext(Dispatchers.Default) { context.packageManager.appArt(app.packageName) }
    }
    var focused by remember { mutableStateOf<Title?>(null) }
    val shown = focused ?: rows.firstOrNull()?.titles?.firstOrNull()
    val openButton = remember { FocusRequester() }
    val firstCard = remember { FocusRequester() }

    BackHandler(onBack = onClose)

    // Land on "Open <app>", so the plainest choice is the obvious one; the shows are a press below.
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocused) {
        if (!windowFocused || focused != null) return@LaunchedEffect
        runCatching { openButton.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(Ground)) {
        FallScene(Modifier.fillMaxSize())
        Backdrop(shown, Modifier.align(Alignment.TopEnd))
        // Rows run to the screen's edges so they read as scrolling; everything else keeps the margin.
        Column(Modifier.fillMaxSize().padding(top = 32.dp)) {
            OpenAppButton(app.label, art, Modifier.padding(start = ScreenMargin).focusRequester(openButton)) { context.launchApp(app) }
            val signInCode = (youTubeAccount as? YouTubeAccount.State.Code)?.takeIf { isYouTube }
            AnimatedContent(
                targetState = signInCode,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label = "signIn",
            ) { code ->
                if (code != null) {
                    YouTubeSignIn(code)
                } else {
                    HeroText(
                        shown,
                        heading = app.label,
                        watchIn = app.label,
                        emptyMessage = if (rows.isEmpty()) "Shows will appear here once the guide has loaded." else "",
                    )
                }
            }
            LazyColumn(
                Modifier
                    .weight(1f)
                    // Rows scrolled partly out of view fade away at the top instead of being cut.
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.04f to Color.Black), blendMode = BlendMode.DstIn)
                    },
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 40.dp),
            ) {
                rows.forEachIndexed { rowIndex, row ->
                    item(key = row.heading) {
                        ShowRow(
                            row = row,
                            firstCard = if (rowIndex == 0) firstCard else null,
                            above = if (rowIndex == 0) openButton else null,
                            onFocused = { focused = it },
                            onOpen = { title -> scope.launch { context.openTitle(app, title) } },
                        )
                    }
                }
            }
        }
    }
}

// The focused show's artwork, top right, fading into the background on its left and lower edges.
@Composable
internal fun Backdrop(title: Title?, modifier: Modifier) {
    val image = rememberRemoteImage(title?.imageUrl(large = true))
    // Holds the last picture while the next one downloads, so it crossfades instead of blinking.
    var latest by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(image) { if (image != null) latest = image }
    Box(modifier.size(600.dp, 337.dp)) {
        Crossfade(latest, animationSpec = tween(400), label = "backdrop") { bitmap ->
            if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Ground, 0.22f to Ground.copy(alpha = 0.6f), 0.48f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.66f to Color.Transparent, 1f to Ground)))
    }
}

// The focused show's name and details; with nothing focused, the screen's heading.
@Composable
internal fun HeroText(title: Title?, heading: String, watchIn: String?, emptyMessage: String) {
    AnimatedContent(
        targetState = title,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
        modifier = Modifier.padding(start = ScreenMargin, top = 26.dp).width(400.dp).height(206.dp),
        label = "heroText",
    ) { shown ->
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (shown == null) {
                BasicText(heading, style = SimpleTVType.heroTitle)
                BasicText(emptyMessage, style = SimpleTVType.heroBody)
                return@Column
            }
            // A title that wraps takes a line from the description, so the block keeps its height.
            var titleLines by remember { mutableIntStateOf(1) }
            BasicText(
                shown.name,
                style = SimpleTVType.heroTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { titleLines = it.lineCount },
            )
            BasicText(metaLine(shown), style = SimpleTVType.heroMeta)
            BasicText(shown.overview, style = SimpleTVType.heroBody, maxLines = 4 - titleLines, overflow = TextOverflow.Ellipsis)
            if (watchIn != null) {
                Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).border(1.dp, Outline, CircleShape), contentAlignment = Alignment.Center) {
                        BasicText("OK", style = SimpleTVType.okBadge)
                    }
                    BasicText("Watch in $watchIn", style = SimpleTVType.heroMeta)
                }
            }
        }
    }
}

// Takes the hero's place until the TV is signed in to YouTube.
@Composable
private fun YouTubeSignIn(code: YouTubeAccount.State.Code) {
    Column(
        Modifier.padding(start = ScreenMargin, top = 26.dp).width(560.dp).height(206.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText("Connect YouTube", style = SimpleTVType.heroTitle)
        BasicText(
            "To see new videos from your subscriptions, go to ${code.url.removePrefix("https://").removePrefix("www.")} on your phone and enter this code:",
            style = SimpleTVType.heroBody,
        )
        Box(Modifier.padding(top = 4.dp).border(1.dp, Outline, RoundedCornerShape(10.dp)).padding(horizontal = 18.dp, vertical = 6.dp)) {
            BasicText(code.userCode, style = SimpleTVType.heroTitle)
        }
    }
}

private fun metaLine(title: Title): String = listOfNotNull(
    title.year.ifEmpty { null },
    title.seasons?.let { if (it == 1) "1 season" else "$it seasons" } ?: if (title.kind == TitleKind.Movie) "Movie" else null,
    title.note.ifEmpty { null },
).joinToString(" · ")

// The folder's main button, with the app's logo. It bounces once as the folder opens, to
// catch the eye.
@Composable
private fun OpenAppButton(label: String, art: AppArt?, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(200), label = "openScale")
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        delay(400)
        bounce.animateTo(1.16f, tween(150))
        bounce.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 260f))
    }
    // Sits over show artwork, so it keeps a light fill to stay readable.
    val container by animateColorAsState(if (focused) Ink else Ground.copy(alpha = 0.92f), tween(200), label = "openColor")
    val content by animateColorAsState(if (focused) OnInk else Ink, tween(200), label = "openText")
    val border by animateColorAsState(if (focused) Ink else Outline, tween(200), label = "openBorder")
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale * bounce.value
                scaleY = scale * bounce.value
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .background(container, CircleShape)
            .border(1.5.dp, border, CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(start = 8.dp, end = 22.dp, top = 7.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        art?.let { Image(it.image, null, Modifier.size(52.dp, 30.dp).clip(CircleShape), contentScale = ContentScale.Crop) }
        BasicText("Open $label", style = SimpleTVType.button.copy(fontSize = 17.sp), color = { content })
    }
}

@Composable
internal fun ShowRow(
    row: FolderRow,
    firstCard: FocusRequester?,
    above: FocusRequester?,
    onFocused: (Title) -> Unit,
    onOpen: (Title) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        BasicText(row.heading, style = SimpleTVType.rowHeading, modifier = Modifier.padding(start = ScreenMargin))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(if (row.ranked) 11.dp else 15.dp),
            contentPadding = PaddingValues(start = ScreenMargin, end = ScreenMargin, top = 6.dp, bottom = 6.dp),
        ) {
            itemsIndexed(row.titles, key = { _, t -> t.key }) { index, title ->
                val modifier = Modifier
                    .then(if (index == 0 && firstCard != null) Modifier.focusRequester(firstCard) else Modifier)
                    .then(if (above != null) Modifier.focusProperties { up = above } else Modifier)
                if (row.ranked) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        BasicText("${index + 1}", style = SimpleTVType.rank)
                        ShowCard(title, 100.dp, modifier, onFocused, onOpen)
                    }
                } else {
                    ShowCard(title, 165.dp, modifier, onFocused, onOpen)
                }
            }
        }
    }
}

@Composable
internal fun ShowCard(title: Title, width: Dp, modifier: Modifier, onFocused: (Title) -> Unit, onOpen: (Title) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(200), label = "cardScale")
    val elevation by animateDpAsState(if (focused) 10.dp else 0.dp, tween(200), label = "cardElevation")
    val ring by animateColorAsState(if (focused) Ink else Ink.copy(alpha = 0f), tween(200), label = "cardRing")
    val image = rememberRemoteImage(title.imageUrl(large = false))

    Column(Modifier.width(width).zIndex(if (focused) 1f else 0f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier
                .size(width, width * 9 / 16)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    val outset = 2.dp.toPx() + stroke / 2
                    drawRoundRect(
                        color = ring,
                        topLeft = Offset(-outset, -outset),
                        size = Size(size.width + 2 * outset, size.height + 2 * outset),
                        cornerRadius = CornerRadius(CardCorner.toPx() + outset),
                        style = Stroke(stroke),
                    )
                }
                .shadow(elevation, CardShape, ambientColor = Ink, spotColor = Ink)
                .clip(CardShape)
                .background(RankInk)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused(title)
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpen(title) },
        ) {
            Crossfade(image, animationSpec = tween(250), label = "cardImage") { bitmap ->
                if (bitmap != null) Image(bitmap, title.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        BasicText(title.name, style = SimpleTVType.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (title.note.isNotEmpty()) BasicText(title.note, style = SimpleTVType.cardNote, maxLines = 1)
    }
}
