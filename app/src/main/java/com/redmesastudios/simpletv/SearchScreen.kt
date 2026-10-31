package com.redmesastudios.simpletv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.redmesastudios.simpletv.ui.theme.Ground
import com.redmesastudios.simpletv.ui.theme.Ink
import com.redmesastudios.simpletv.ui.theme.InkSoft
import com.redmesastudios.simpletv.ui.theme.OnInk
import com.redmesastudios.simpletv.ui.theme.Outline
import com.redmesastudios.simpletv.ui.theme.RankInk
import com.redmesastudios.simpletv.ui.theme.SimpleTVType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val FieldShape = RoundedCornerShape(14.dp)
private val Panel = Color(0xFFFBF6EF)
private val PanelBorder = Color(0xFFE2D2BE)

private fun Title.key() = "$kind:$tmdbId"

// Search for shows, movies and people. Every card says where it can be watched; picking
// one opens it in the app that has it, the same way a folder does.
@Composable
fun SearchScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val installed = remember { installedApps(StreamingApps) { context.packageManager.tvLaunchIntent(it) != null } }

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<SearchResults?>(null) }
    var failed by remember { mutableStateOf(false) }
    var person by remember { mutableStateOf<Person?>(null) }
    var personTitles by remember { mutableStateOf(emptyList<Title>()) }
    val where = remember { mutableStateMapOf<String, WhereToWatch>() }
    var choosing by remember { mutableStateOf<Pair<Title, List<StreamingApp>>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val field = remember { FocusRequester() }

    val voice = rememberVoiceSearch { text, final ->
        query = text
        if (final) focusManager.moveFocus(FocusDirection.Down)
    }

    // Back steps out of an open choice, then an actor's page, then search itself.
    BackHandler {
        when {
            choosing != null -> choosing = null
            person != null -> person = null
            else -> onClose()
        }
    }

    LaunchedEffect(Unit) { field.requestFocus() }

    // Searches once typing pauses.
    LaunchedEffect(query) {
        person = null
        if (query.isBlank()) {
            results = null
            return@LaunchedEffect
        }
        delay(450)
        runCatching { Tmdb.search(query.trim()) }
            .onSuccess { results = it; failed = false }
            .onFailure { failed = true }
    }

    LaunchedEffect(person) {
        personTitles = emptyList()
        person?.let { p -> personTitles = runCatching { Tmdb.credits(p) }.getOrDefault(emptyList()) }
    }

    // Where-to-watch for everything on screen, filled in as it arrives.
    val shownTitles = if (person != null) personTitles else results?.let { it.shows + it.movies }.orEmpty()
    LaunchedEffect(shownTitles) {
        val lookups = Semaphore(6)
        coroutineScope {
            shownTitles.filter { it.key() !in where }.map { title ->
                async {
                    lookups.withPermit {
                        runCatching { Tmdb.offers(title) }.getOrNull()?.let { where[title.key()] = whereToWatch(it, installed) }
                    }
                }
            }.awaitAll()
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(5_000)
            message = null
        }
    }

    fun open(title: Title) {
        val options = where[title.key()] ?: return
        when {
            options.here.size == 1 -> scope.launch { context.openTitle(options.here.first(), title) }
            options.here.size > 1 -> choosing = title to options.here
            options.rentHere.isNotEmpty() -> scope.launch { context.openTitle(options.rentHere.first(), title) }
            options.elsewhere.isNotEmpty() -> message = "${title.name} streams on ${options.elsewhere.first()}, which isn't on this TV."
            else -> message = "${title.name} isn't streaming anywhere right now."
        }
    }

    // Titles you can watch here come first.
    fun withNotes(titles: List<Title>) = titles
        .map { it.copy(note = where[it.key()]?.summary.orEmpty()) }
        .sortedByDescending { where[it.key()]?.let { w -> w.here.isNotEmpty() || w.rentHere.isNotEmpty() } ?: false }

    val rows = when {
        person != null -> listOf(
            FolderRow("Shows with ${person!!.name}", withNotes(personTitles.filter { it.kind == TitleKind.Show })),
            FolderRow("Movies with ${person!!.name}", withNotes(personTitles.filter { it.kind == TitleKind.Movie })),
        )
        else -> listOf(
            FolderRow("Shows", withNotes(results?.shows.orEmpty())),
            FolderRow("Movies", withNotes(results?.movies.orEmpty())),
        )
    }.filter { it.titles.isNotEmpty() }
    val people = if (person == null) results?.people.orEmpty() else emptyList()

    Box(Modifier.fillMaxSize().background(Ground)) {
        FallScene(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchField(
                    query = query,
                    listening = voice.listening,
                    modifier = Modifier.weight(1f).focusRequester(field),
                    onQueryChange = { query = it },
                    onDone = {
                        keyboard?.hide()
                        focusManager.moveFocus(FocusDirection.Down)
                    },
                )
                MicButton(voice.listening) { voice.start() }
            }
            BasicText(
                text = when {
                    person != null -> "${person!!.name} · ${person!!.knownFor}"
                    failed -> "Search isn't reachable right now. Check the internet and try again."
                    query.isBlank() -> "Type, or press the microphone and say a show, movie or actor."
                    results != null && rows.isEmpty() && people.isEmpty() -> "Nothing found for \"$query\"."
                    else -> ""
                },
                style = SimpleTVType.heroMeta,
                modifier = Modifier.padding(start = ScreenMargin, top = 14.dp).height(20.dp),
            )
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            ) {
                rows.forEach { row ->
                    item(key = row.heading) {
                        ShowRow(row = row, firstCard = null, above = null, onFocused = {}, onOpen = ::open)
                    }
                }
                if (people.isNotEmpty()) {
                    item(key = "people") { PeopleRow(people) { person = it } }
                }
            }
        }

        // Shown when a title streams on more than one of your apps.
        choosing?.let { (title, apps) ->
            ChooseApp(title, apps, modifier = Modifier.align(Alignment.Center)) { app ->
                choosing = null
                scope.launch { context.openTitle(app, title) }
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

@Composable
private fun SearchField(
    query: String,
    listening: Boolean,
    modifier: Modifier,
    onQueryChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) Ink else PanelBorder, tween(200), label = "fieldBorder")
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = SimpleTVType.rowHeading.copy(fontSize = SimpleTVType.heroBody.fontSize * 1.4f),
        cursorBrush = SolidColor(Ink),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onDone() }),
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .background(Panel, FieldShape)
            .border(2.dp, border, FieldShape)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        decorationBox = { inner ->
            Box {
                if (query.isEmpty()) {
                    BasicText(
                        if (listening) "Listening…" else "Search shows, movies and people",
                        style = SimpleTVType.rowHeading.copy(color = InkSoft, fontSize = SimpleTVType.heroBody.fontSize * 1.4f),
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun MicButton(listening: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(200), label = "micScale")
    val container by animateColorAsState(if (focused || listening) Ink else Panel, tween(200), label = "micColor")
    val content by animateColorAsState(if (focused || listening) OnInk else Ink, tween(200), label = "micIcon")
    // A slow pulse while listening, so it's clear the TV is taking voice.
    val pulse by animateFloatAsState(if (listening) 1.15f else 1f, tween(600), label = "micPulse")
    Box(
        Modifier
            .size(54.dp)
            .graphicsLayer { scaleX = scale * pulse; scaleY = scale * pulse }
            .background(container, CircleShape)
            .border(2.dp, if (focused) Ink else PanelBorder, CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) {
            val u = size.width / 24f
            val stroke = Stroke(width = 1.8f * u)
            drawRoundRect(content, topLeft = Offset(9f * u, 3f * u), size = Size(6f * u, 11f * u), cornerRadius = CornerRadius(3f * u), style = stroke)
            drawArc(content, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(5.5f * u, 6.5f * u), size = Size(13f * u, 11f * u), style = stroke)
            drawLine(content, Offset(12f * u, 17.5f * u), Offset(12f * u, 21f * u), strokeWidth = 1.8f * u)
        }
    }
}

@Composable
private fun PeopleRow(people: List<Person>, onPick: (Person) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        BasicText("People", style = SimpleTVType.rowHeading, modifier = Modifier.padding(start = ScreenMargin))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(start = ScreenMargin, end = ScreenMargin, top = 6.dp, bottom = 6.dp),
        ) {
            items(people, key = { it.id }) { p -> PersonCard(p) { onPick(p) } }
        }
    }
}

@Composable
private fun PersonCard(person: Person, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(200), label = "personScale")
    val ring by animateColorAsState(if (focused) Ink else Ink.copy(alpha = 0f), tween(200), label = "personRing")
    val photo = rememberRemoteImage(person.photoPath?.let { tmdbImage(it, "w185") })
    Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(84.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .border(2.dp, ring, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(RankInk)
                .onFocusChanged { focused = it.isFocused }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(photo, animationSpec = tween(250), label = "personPhoto") { bitmap ->
                if (bitmap != null) {
                    Image(bitmap, person.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    BasicText(person.name.take(1), style = SimpleTVType.heroTitle.copy(color = OnInk))
                }
            }
        }
        BasicText(person.name, style = SimpleTVType.cardTitle.copy(textAlign = TextAlign.Center), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChooseApp(title: Title, apps: List<StreamingApp>, modifier: Modifier, onPick: (StreamingApp) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    Column(
        modifier
            .background(Panel, FieldShape)
            .border(2.dp, PanelBorder, FieldShape)
            .padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BasicText("Watch ${title.name} on", style = SimpleTVType.rowHeading)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            apps.forEachIndexed { index, app ->
                PillButton(app.label, if (index == 0) Modifier.focusRequester(first) else Modifier) { onPick(app) }
            }
        }
    }
}

@Composable
private fun PillButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, tween(200), label = "pillScale")
    val container by animateColorAsState(if (focused) Ink else Color.Transparent, tween(200), label = "pillColor")
    val content by animateColorAsState(if (focused) OnInk else Ink, tween(200), label = "pillText")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(container, CircleShape)
            .border(1.dp, if (focused) Ink else Outline, CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        BasicText(label, style = SimpleTVType.button, color = { content })
    }
}

// Voice input through the TV's own speech service (the same one Google search uses).
private class VoiceSearch(context: Context, private val onText: (String, Boolean) -> Unit) : RecognitionListener {
    var listening by mutableStateOf(false)
        private set
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(this) }

    fun listen() {
        listening = true
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true),
        )
    }

    fun destroy() = recognizer.destroy()

    private fun Bundle.best() = getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    override fun onPartialResults(partialResults: Bundle) { partialResults.best()?.let { onText(it, false) } }
    override fun onResults(results: Bundle) {
        listening = false
        results.best()?.let { onText(it, true) }
    }
    override fun onError(error: Int) { listening = false }
    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

private class VoiceControl(val voice: VoiceSearch, private val askPermission: () -> Unit, private val hasPermission: () -> Boolean) {
    val listening get() = voice.listening
    fun start() = if (hasPermission()) voice.listen() else askPermission()
}

// The microphone permission is asked for the first time voice is used.
@Composable
private fun rememberVoiceSearch(onText: (String, Boolean) -> Unit): VoiceControl {
    val context = LocalContext.current
    val voice = remember { VoiceSearch(context, onText) }
    DisposableEffect(voice) { onDispose { voice.destroy() } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voice.listen()
    }
    return remember(voice) {
        VoiceControl(
            voice = voice,
            askPermission = { permission.launch(Manifest.permission.RECORD_AUDIO) },
            hasPermission = { context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED },
        )
    }
}

