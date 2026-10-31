package com.redmesastudios.simpletv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redmesastudios.simpletv.ui.theme.Ground
import com.redmesastudios.simpletv.ui.theme.Ink
import com.redmesastudios.simpletv.ui.theme.InkSoft
import com.redmesastudios.simpletv.ui.theme.OnInk
import com.redmesastudios.simpletv.ui.theme.Outline
import com.redmesastudios.simpletv.ui.theme.SimpleTVType
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val CardFill = Color(0xFFFBF6EF)
private val CardEdge = Color(0xFFE2D2BE)
private val AlertFill = Color(0xFF7A3B26)
private val Up = Color(0xFF4E6B3A)
private val Down = Color(0xFFA84B3A)
private val CardShape = RoundedCornerShape(10.dp)
private val RefreshEvery = Duration.ofMinutes(10)

private val TitleStyle = SimpleTVType.greeting
private val SubtitleStyle = SimpleTVType.date
private val BodyStyle = SimpleTVType.heroBody
private val SmallStyle = SimpleTVType.hint.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

// A full-screen page behind one of the home screen's glance cards. Back returns home.
@Composable
fun ChannelScreen(channel: Channel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    when (channel) {
        Channel.Weather -> WeatherPage()
        Channel.News -> NewsPage()
        Channel.Markets -> MarketsPage()
    }
}

// Loads on open and every 10 minutes after, keeping the last good reading if one fails.
@Composable
private fun <T> rememberChannelData(key: Any? = Unit, load: suspend () -> T): T? {
    var data by remember(key) { mutableStateOf<T?>(null) }
    LaunchedEffect(key) {
        while (true) {
            runCatching { load() }.onSuccess { data = it }
            delay(RefreshEvery.toMillis())
        }
    }
    return data
}

@Composable
private fun ChannelPage(title: String, subtitle: String, backTo: String = "Home", content: @Composable () -> Unit) {
    val now by rememberMinuteClock()
    Box(Modifier.fillMaxSize().background(Ground)) {
        FallScene(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(start = 60.dp, end = 60.dp, top = 40.dp, bottom = 28.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    BasicText(title, style = TitleStyle)
                    BasicText(subtitle, style = SubtitleStyle)
                }
                BasicText(now.format(TimeFormat), style = SimpleTVType.clock)
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 24.dp, bottom = 16.dp)) { content() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.border(1.dp, Outline, RoundedCornerShape(12.dp)).padding(horizontal = 9.dp, vertical = 4.dp)) {
                    BasicText("BACK", style = SimpleTVType.okBadge.copy(fontSize = 8.5.sp))
                }
                AnimatedContent(backTo, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "backTo") {
                    BasicText(it, style = SimpleTVType.hint)
                }
            }
        }
    }
}

// Shows a calm line while the first reading loads, then fades to the page.
@Composable
private fun <T> Loading(data: T?, waiting: String, content: @Composable (T) -> Unit) {
    AnimatedContent(
        targetState = data,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "channel",
    ) { shown ->
        if (shown == null) BasicText(waiting, style = BodyStyle.copy(color = InkSoft)) else content(shown)
    }
}

private fun partOfDay(now: LocalDateTime): String = when (now.hour) {
    in 5..11 -> "morning"
    in 12..16 -> "afternoon"
    in 17..20 -> "evening"
    else -> "night"
}

@Composable
private fun WeatherPage() {
    val report = rememberChannelData { Channels.weather() }
    val now by rememberMinuteClock()
    // A day picked from the forecast fills the page; Left and Right step through the days,
    // and Back returns to the forecast with that day selected.
    var viewing by remember { mutableStateOf<Int?>(null) }
    var lastViewed by remember { mutableStateOf<Int?>(null) }
    val dayCards = remember { mutableMapOf<Int, FocusRequester>() }
    BackHandler(enabled = viewing != null) { viewing = null }
    ChannelPage(
        title = report?.place?.let { "Weather in $it" } ?: "Weather",
        subtitle = "${now.format(DateTimeFormatter.ofPattern("EEEE"))} ${partOfDay(now)} · National Weather Service",
        backTo = if (viewing != null) "Forecast" else "Home",
    ) {
        Loading(report, if (Home.hasWeather) "Checking the forecast…" else "Set weather_point in ~/.config/simpletv/home.txt and rebuild to see the weather.") { weather ->
            val open = { index: Int ->
                lastViewed = index
                viewing = index
            }
            AnimatedContent(
                targetState = viewing != null && weather.days.isNotEmpty(),
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label = "weatherDay",
            ) { showingDay ->
                if (showingDay) {
                    DayView(weather.days, (viewing ?: 0).coerceIn(weather.days.indices), onStep = open)
                } else {
                    Forecast(weather, dayCards, lastViewed, onOpen = open)
                }
            }
        }
    }
}

@Composable
private fun Forecast(weather: WeatherReport, dayCards: MutableMap<Int, FocusRequester>, lastViewed: Int?, onOpen: (Int) -> Unit) {
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocused) {
        val target = lastViewed ?: return@LaunchedEffect
        if (!windowFocused) return@LaunchedEffect
        // The forecast may still be fading back in, so ask a few times as it settles.
        repeat(4) {
            runCatching { dayCards[target]?.requestFocus() }
            delay(60)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        weather.alerts.take(2).forEach { alert ->
            Box(Modifier.fillMaxWidth().background(AlertFill, CardShape).padding(horizontal = 16.dp, vertical = 9.dp)) {
                BasicText(alert, style = BodyStyle.copy(color = OnInk, fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                "${weather.temperature}°",
                style = SimpleTVType.greeting.copy(fontSize = 96.sp, lineHeight = 96.sp, letterSpacing = (-3).sp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(weather.summary, style = SimpleTVType.serviceName.copy(fontSize = 23.sp))
                weather.rainLine?.let { BasicText(it, style = SimpleTVType.heroMeta.copy(fontSize = 15.sp)) }
                weather.nextLine?.let { BasicText(it, style = SimpleTVType.heroMeta.copy(fontSize = 15.sp)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            weather.days.forEachIndexed { index, day ->
                DayCard(day, dayCards.getOrPut(index) { FocusRequester() }, onOpen = { onOpen(index) })
            }
        }
    }
}

@Composable
private fun RowScope.DayCard(day: DayForecast, focusRequester: FocusRequester, onOpen: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, tween(200), label = "dayScale")
    val edge by animateColorAsState(if (focused) Ink else CardEdge, tween(200), label = "dayEdge")
    Column(
        Modifier
            .weight(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .focusRequester(focusRequester)
            .background(CardFill, CardShape)
            .border(if (focused) 2.dp else 1.dp, edge, CardShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(day.name, style = SimpleTVType.serviceName.copy(fontSize = 14.sp))
        Canvas(Modifier.size(30.dp)) { drawSky(skyFor(day.summary)) }
        BasicText(
            day.low?.let { "${day.high}° / $it°" } ?: "${day.high}°",
            style = SimpleTVType.serviceName.copy(fontSize = 17.sp),
        )
        BasicText(day.summary, style = SmallStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        BasicText("Rain ${day.rain}%", style = SmallStyle)
    }
}

// One day in full, between ◀ and ▶ that step to the day before and after. The arrows stay
// put while the day's card slides.
@Composable
private fun DayView(days: List<DayForecast>, index: Int, onStep: (Int) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(4) {
            runCatching { focus.requestFocus() }
            delay(60)
        }
    }
    Row(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { if (index > 0) onStep(index - 1); true }
                    Key.DirectionRight -> { if (index < days.lastIndex) onStep(index + 1); true }
                    else -> false
                }
            },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepArrow("◀", visible = index > 0)
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(300)) { direction * it / 4 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { -direction * it / 4 } + fadeOut(tween(300)))
            },
            modifier = Modifier.weight(1f),
            label = "day",
        ) { shown -> DayCardInFull(days[shown]) }
        StepArrow("▶", visible = index < days.lastIndex)
    }
}

@Composable
private fun StepArrow(symbol: String, visible: Boolean) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(200), label = "stepArrow")
    Box(
        Modifier
            .graphicsLayer { this.alpha = alpha }
            .size(44.dp)
            .background(CardFill, RoundedCornerShape(50))
            .border(1.dp, CardEdge, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(symbol, style = SimpleTVType.serviceName.copy(fontSize = 16.sp, color = InkSoft))
    }
}

@Composable
private fun DayCardInFull(day: DayForecast) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardFill, CardShape)
            .border(1.dp, CardEdge, CardShape)
            .padding(horizontal = 28.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(48.dp)) { drawSky(skyFor(day.summary)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    day.date?.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)) ?: day.name,
                    style = SimpleTVType.heroTitle.copy(fontSize = 26.sp, lineHeight = 30.sp),
                )
                BasicText(day.summary, style = SimpleTVType.heroMeta.copy(fontSize = 15.sp))
            }
            BasicText(
                day.low?.let { "${day.high}° / $it°" } ?: "${day.high}°",
                style = SimpleTVType.greeting.copy(fontSize = 36.sp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DayFact("Rain ${day.rain}%")
            if (day.wind.isNotEmpty()) DayFact("Wind ${day.wind}")
        }
        DayPart("Day", day.detail.ifEmpty { day.summary })
        day.night?.let { DayPart("Night", it.detail.ifEmpty { it.summary }) }
        val hours = daytimeHours(day.hours)
        if (hours.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                hours.forEach { hour ->
                    // "3 PM  74°", with the rain chance only when it's worth a look.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicText(hourLabel(hour.time), style = SmallStyle)
                        BasicText("${hour.temperature}°", style = SimpleTVType.serviceName.copy(fontSize = 16.sp))
                        if (hour.rain >= 20) BasicText("${hour.rain}%", style = SmallStyle)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayFact(text: String) {
    Box(Modifier.border(1.dp, CardEdge, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 5.dp)) {
        BasicText(text, style = SimpleTVType.heroMeta.copy(fontSize = 13.sp))
    }
}

@Composable
private fun DayPart(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        BasicText(label, style = SimpleTVType.rowHeading.copy(fontSize = 13.sp, color = InkSoft))
        BasicText(text, style = BodyStyle.copy(fontSize = 15.sp, lineHeight = 21.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun NewsPage() {
    val stories = rememberChannelData { Channels.news() }
    val local = rememberChannelData { if (Home.hasLocalNews) runCatching { Channels.localNews() }.getOrDefault(emptyList()) else emptyList() }
    // A story picked from the list fills the page; Back returns to it in the list.
    var reading by remember { mutableStateOf<NewsStory?>(null) }
    var lastRead by remember { mutableStateOf<NewsStory?>(null) }
    val cards = remember { mutableMapOf<String, FocusRequester>() }
    val columns = List(3) { rememberLazyListState() }
    BackHandler(enabled = reading != null) { reading = null }
    ChannelPage(
        title = "News",
        subtitle = "The biggest stories, by how many newsrooms cover them · SignalPost",
        backTo = if (reading != null) "All stories" else "Home",
    ) {
        Loading(stories, if (Home.hasNews) "Checking the news…" else "Add a SignalPost API key in ~/.config/signalpost/api_key and rebuild to see the news.") { all ->
            AnimatedContent(
                targetState = reading,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label = "story",
            ) { story ->
                if (story != null) {
                    StoryPage(story)
                    return@AnimatedContent
                }
                val windowFocused = LocalWindowInfo.current.isWindowFocused
                LaunchedEffect(windowFocused) {
                    if (!windowFocused) return@LaunchedEffect
                    val target = lastRead?.title ?: all.firstOrNull { !it.world }?.title
                    // The list may still be fading back in, so ask a few times as it settles.
                    repeat(4) {
                        runCatching { cards[target]?.requestFocus() }
                        delay(60)
                    }
                }
                val open = { picked: NewsStory ->
                    lastRead = picked
                    reading = picked
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StoryColumn("Top stories", all.filterNot { it.world }.take(12), columns[0], cards, open)
                    StoryColumn("World", all.filter { it.world }.take(12), columns[1], cards, open)
                    if (Home.hasLocalNews) {
                        StoryColumn(Home.localNewsName, local.orEmpty().take(12), columns[2], cards, open, empty = "No ${Home.localNewsName} stories right now.")
                    }
                }
            }
        }
    }
}

// One story in full: the whole title and SignalPost's summary.
@Composable
private fun StoryPage(story: NewsStory) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    // On a card, so the text stays clear of whatever the scene is doing behind it.
    Column(
        Modifier
            .fillMaxWidth(0.8f)
            .focusRequester(focus)
            .focusable()
            .background(CardFill, CardShape)
            .border(1.dp, CardEdge, CardShape)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BasicText(story.title, style = SimpleTVType.heroTitle.copy(fontSize = 32.sp, lineHeight = 38.sp))
        BasicText(storyMeta(story), style = SimpleTVType.heroMeta.copy(fontSize = 14.sp))
        BasicText(story.summary, style = BodyStyle.copy(fontSize = 18.sp, lineHeight = 28.sp))
    }
}

private fun storyMeta(story: NewsStory): String {
    val newsrooms = if (story.newsrooms == 1) "1 newsroom" else "${story.newsrooms} newsrooms"
    return "$newsrooms · updated ${storyAge(story.lastSeen, Instant.now())}"
}

@Composable
private fun RowScope.StoryColumn(
    heading: String,
    stories: List<NewsStory>,
    state: LazyListState,
    cards: MutableMap<String, FocusRequester>,
    onOpen: (NewsStory) -> Unit,
    empty: String = "",
) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText(heading, style = SimpleTVType.rowHeading)
        if (stories.isEmpty()) BasicText(empty, style = BodyStyle.copy(color = InkSoft))
        LazyColumn(
            state = state,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp, start = 4.dp, end = 4.dp),
        ) {
            items(stories, key = { it.title }) { story ->
                StoryCard(story, cards.getOrPut(story.title) { FocusRequester() }) { onOpen(story) }
            }
        }
    }
}

@Composable
private fun StoryCard(story: NewsStory, focusRequester: FocusRequester, onOpen: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(200), label = "storyScale")
    val edge by animateColorAsState(if (focused) Ink else CardEdge, tween(200), label = "storyEdge")
    Column(
        Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(CardFill, CardShape)
            .border(if (focused) 2.dp else 1.dp, edge, CardShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(story.title, style = SimpleTVType.serviceName.copy(fontSize = 14.sp, lineHeight = 18.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        BasicText(story.summary, style = BodyStyle.copy(fontSize = 12.sp, lineHeight = 16.sp), maxLines = if (focused) 5 else 2, overflow = TextOverflow.Ellipsis)
        BasicText(storyMeta(story), style = SmallStyle)
    }
}

// Short ages: "25 min ago", "3 hours ago", "2 days ago".
fun storyAge(at: Instant, now: Instant): String {
    val age = Duration.between(at, now)
    return when {
        age.toMinutes() < 60 -> "${age.toMinutes().coerceAtLeast(1)} min ago"
        age.toHours() == 1L -> "1 hour ago"
        age.toHours() < 48 -> "${age.toHours()} hours ago"
        else -> "${age.toDays()} days ago"
    }
}

@Composable
private fun MarketsPage() {
    val context = LocalContext.current
    var symbols by remember { mutableStateOf(WatchList.symbols(context)) }
    var editing by remember { mutableStateOf(false) }
    val indexes = rememberChannelData { Channels.markets() }
    val stocks = rememberChannelData(symbols) { Channels.quotes(symbols) }
    val asOf = indexes?.maxOfOrNull { it.asOf }
    val open = indexes?.any { it.open } == true
    val time = asOf?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("EEE h:mm a"))
    // A market picked from the cards fills the page; Back returns to its card.
    var viewing by remember { mutableStateOf<IndexDay?>(null) }
    var lastViewed by remember { mutableStateOf<String?>(null) }
    val cards = remember { mutableMapOf<String, FocusRequester>() }
    val listState = rememberLazyListState()
    BackHandler(enabled = editing) { editing = false }
    BackHandler(enabled = viewing != null && !editing) { viewing = null }
    Box {
    ChannelPage(
        title = "Markets",
        subtitle = when {
            indexes == null -> "Yahoo Finance"
            open -> "Market open · as of $time · Yahoo Finance"
            else -> "Market closed · last trade $time · Yahoo Finance"
        },
        backTo = if (viewing != null) "All markets" else "Home",
    ) {
        Loading(indexes, "Checking the markets…") { all ->
          AnimatedContent(
              targetState = viewing,
              transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
              label = "market",
          ) { picked ->
            if (picked != null) {
                MarketDetailPage(picked)
                return@AnimatedContent
            }
            val windowFocused = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(windowFocused) {
                if (!windowFocused) return@LaunchedEffect
                val target = lastViewed ?: all.first().symbol
                // The cards may still be fading back in, so ask a few times as they settle.
                repeat(4) {
                    runCatching { cards[target]?.requestFocus() }
                    delay(60)
                }
            }
            val open = { day: IndexDay ->
                lastViewed = day.symbol
                viewing = day
            }
            fun card(day: IndexDay) = cards.getOrPut(day.symbol) { FocusRequester() }
            // One row of three per screen; Down brings the next three up.
            val rows = (stocks ?: symbols.map { null }).chunked(3)
            // Each row is exactly one screen tall, so moving down lines the next row up whole.
            LazyColumn(state = listState) {
                item(key = "indexes") {
                    MarketRow("Market indexes", Modifier.fillParentMaxHeight(), editButton = { EditStocksButton { editing = true } }) {
                        all.forEach { index -> IndexCard(index, card(index)) { open(index) } }
                    }
                }
                rows.forEachIndexed { rowIndex, row ->
                    item(key = "stocks$rowIndex") {
                        val from = rowIndex * 3 + 1
                        val range = if (row.size == 1) "$from" else "$from–${from + row.size - 1}"
                        MarketRow("Your stocks · $range of ${symbols.size}", Modifier.fillParentMaxHeight()) {
                            row.forEachIndexed { i, quote ->
                                if (quote != null) IndexCard(quote, card(quote)) { open(quote) } else MissingCard(symbols.getOrElse(rowIndex * 3 + i) { "" }, stocks == null)
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                if (symbols.isEmpty()) {
                    item(key = "add") {
                        BasicText(
                            "Add your own stocks with Edit your stocks, then press Down to page through them three at a time.",
                            style = BodyStyle.copy(color = InkSoft),
                        )
                    }
                }
            }
          }
        }
    }
    AnimatedVisibility(editing, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
        StockEditor(symbols, onSave = {
            symbols = it
            WatchList.save(context, it)
            editing = false
        })
    }
    }
}

@Composable
private fun MarketRow(heading: String, modifier: Modifier, editButton: (@Composable () -> Unit)? = null, cards: @Composable RowScope.() -> Unit) {
    // The cards reach the row's bottom edge, so moving down lines the next row up exactly.
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().height(30.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BasicText(heading, style = SimpleTVType.rowHeading)
            editButton?.invoke()
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp), content = cards)
    }
}

@Composable
private fun EditStocksButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val container by animateColorAsState(if (focused) Ink else Color.Transparent, tween(200), label = "editColor")
    val content by animateColorAsState(if (focused) OnInk else InkSoft, tween(200), label = "editText")
    Box(
        Modifier
            .background(container, RoundedCornerShape(50))
            .border(1.dp, if (focused) Ink else Outline, RoundedCornerShape(50))
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 5.dp),
    ) {
        BasicText("Edit your stocks", style = SimpleTVType.hint, color = { content })
    }
}

// Type symbols with the remote's keyboard; Done saves them.
@Composable
private fun StockEditor(symbols: List<String>, onSave: (List<String>) -> Unit) {
    var text by remember { mutableStateOf(symbols.joinToString(" ")) }
    val field = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { field.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(560.dp).background(Ground, RoundedCornerShape(16.dp)).border(1.dp, Outline, RoundedCornerShape(16.dp)).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText("Your stocks", style = SimpleTVType.serviceName.copy(fontSize = 22.sp))
            BasicText("Type ticker symbols with spaces between them, like AAPL MSFT TSLA, then press Done.", style = BodyStyle.copy(color = InkSoft))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = SimpleTVType.rowHeading.copy(fontSize = 19.sp),
                cursorBrush = SolidColor(Ink),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave(parseSymbols(text)) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(field)
                    .background(CardFill, CardShape)
                    .border(2.dp, Ink, CardShape)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            BasicText("Back closes without saving.", style = SmallStyle)
        }
    }
}

@Composable
private fun RowScope.MissingCard(symbol: String, loading: Boolean) {
    Column(
        Modifier.weight(1f).fillMaxHeight().background(CardFill, CardShape).border(1.dp, CardEdge, CardShape).focusable().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(symbol, style = SimpleTVType.serviceName.copy(fontSize = 16.sp))
        BasicText(if (loading) "Checking…" else "Yahoo Finance doesn't know this symbol.", style = BodyStyle.copy(color = InkSoft))
    }
}

@Composable
private fun RowScope.IndexCard(index: IndexDay, focusRequester: FocusRequester, onOpen: () -> Unit) {
    val color = if (index.change < 0) Down else Up
    val number = NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
    var focused by remember { mutableStateOf(false) }
    val edge by animateColorAsState(if (focused) Ink else CardEdge, tween(200), label = "indexEdge")
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .focusRequester(focusRequester)
            .background(CardFill, CardShape)
            .border(if (focused) 2.dp else 1.dp, edge, CardShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(index.name, style = SimpleTVType.serviceName.copy(fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        BasicText(number.format(index.price), style = SimpleTVType.greeting.copy(fontSize = 34.sp))
        BasicText(
            "${if (index.change < 0) "−" else "+"}${number.format(abs(index.change))}  (${percentText(index.percent)})",
            style = SimpleTVType.serviceName.copy(fontSize = 16.sp, color = color),
        )
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().weight(1f)) { drawDayChart(index, color) }
        BasicText("Dashed line: yesterday's close", style = SmallStyle)
    }
}

// One market in full: today, the year's range, how it's done lately, and a year's chart.
@Composable
private fun MarketDetailPage(day: IndexDay) {
    val detail = rememberChannelData(day.symbol) { Channels.detail(day) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val color = if (day.change < 0) Down else Up
    val number = NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
    val asOf = day.asOf.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE h:mm a"))
    Column(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .background(CardFill, CardShape)
            .border(1.dp, CardEdge, CardShape)
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(day.name, style = SimpleTVType.heroTitle.copy(fontSize = 30.sp, lineHeight = 36.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                BasicText(
                    listOfNotNull(detail?.exchange?.ifEmpty { null }, detail?.currency?.ifEmpty { null }, "${if (day.open) "as of" else "last trade"} $asOf").joinToString(" · "),
                    style = SimpleTVType.heroMeta.copy(fontSize = 14.sp),
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(number.format(day.price), style = SimpleTVType.greeting.copy(fontSize = 38.sp))
                BasicText(
                    "${if (day.change < 0) "−" else "+"}${number.format(abs(day.change))}  (${percentText(day.percent)}) today",
                    style = SimpleTVType.serviceName.copy(fontSize = 16.sp, color = color),
                )
            }
        }
        Loading(detail, "Checking the past year…") { more ->
            fun price(value: Double?) = value?.let { number.format(it) } ?: "—"
            fun range(low: Double?, high: Double?) = if (low != null && high != null) "${number.format(low)} – ${number.format(high)}" else "—"
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BasicText("Today", style = SimpleTVType.rowHeading)
                    Stat("Range", range(more.dayLow, more.dayHigh))
                    Stat("Open", price(more.openPrice))
                    Stat("Previous close", price(day.previousClose))
                    Stat("Volume", more.volume?.let(::bigNumber) ?: "—")
                    Stat("52-week range", range(more.yearLow, more.yearHigh))
                }
                Column(Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BasicText("Performance", style = SimpleTVType.rowHeading)
                    more.performance.forEach { (label, change) ->
                        Stat(label, change?.let(::percentText) ?: "—", change?.let { if (it < 0) Down else Up })
                    }
                }
                Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BasicText("Past year", style = SimpleTVType.rowHeading)
                    val yearColor = if ((more.yearPrices.lastOrNull() ?: 0.0) < (more.yearPrices.firstOrNull() ?: 0.0)) Down else Up
                    Canvas(Modifier.fillMaxWidth().height(170.dp)) { drawPriceLine(more.yearPrices, more.yearPrices.firstOrNull(), yearColor, steps = 1) }
                    BasicText("Dashed line: a year ago", style = SmallStyle)
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, valueColor: Color? = null) {
    // The value keeps to one line; the label gives way if space runs short.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(label, Modifier.weight(1f), style = BodyStyle.copy(color = InkSoft, fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        BasicText(value, style = SimpleTVType.serviceName.copy(fontSize = 15.sp, color = valueColor ?: Ink), softWrap = false)
    }
}

// The day's prices as a line, against a dashed line at the previous close.
private fun DrawScope.drawDayChart(index: IndexDay, color: Color) =
    // A full trading day is 78 five-minute steps; a partial day fills only part of the width.
    drawPriceLine(index.prices.ifEmpty { listOf(index.previousClose, index.price) }, index.previousClose, color, steps = 77)

// Prices as a line, optionally against a dashed reference line; at least `steps` wide.
private fun DrawScope.drawPriceLine(prices: List<Double>, reference: Double?, color: Color, steps: Int) {
    if (prices.isEmpty()) return
    val low = minOf(prices.min(), reference ?: prices.min())
    val high = maxOf(prices.max(), reference ?: prices.max())
    val span = (high - low).takeIf { it > 0 } ?: 1.0
    fun y(price: Double) = (size.height * (1 - (price - low) / span)).toFloat()
    reference?.let {
        val refY = y(it)
        drawLine(InkSoft, Offset(0f, refY), Offset(size.width, refY), strokeWidth = 1.dp.toPx(), alpha = 0.6f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
    }
    val steps = maxOf(prices.size - 1, steps)
    val path = Path()
    prices.forEachIndexed { i, price ->
        val x = size.width * i / steps
        if (i == 0) path.moveTo(x, y(price)) else path.lineTo(x, y(price))
    }
    drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

// Weather icons on the 24-unit grid, in the cards' line style.
private fun DrawScope.drawSky(sky: Sky) {
    when (sky) {
        Sky.Sun -> drawSun()
        Sky.Cloud -> drawCloud()
        Sky.PartlyCloudy -> {
            drawSmallSun()
            drawCloud()
        }
        Sky.Rain -> {
            drawCloud()
            line(scaled { u ->
                moveTo(8f * u, 19f * u); lineTo(7f * u, 22f * u)
                moveTo(12f * u, 19f * u); lineTo(11f * u, 22f * u)
                moveTo(16f * u, 19f * u); lineTo(15f * u, 22f * u)
            })
        }
        Sky.Storm -> {
            drawCloud()
            line(scaled { u -> moveTo(12.5f * u, 17.5f * u); lineTo(10.5f * u, 20.5f * u); lineTo(13f * u, 20.5f * u); lineTo(11f * u, 23.5f * u) })
        }
    }
}

private fun DrawScope.drawSmallSun() = line(scaled { u ->
    addOval(Rect(15f * u, 2f * u, 21f * u, 8f * u))
    moveTo(18f * u, 0.2f * u); lineTo(18f * u, 0.9f * u)
    moveTo(22.6f * u, 5f * u); lineTo(23.4f * u, 5f * u)
})
