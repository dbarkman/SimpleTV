package com.redmesastudios.simpletv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.redmesastudios.simpletv.ui.theme.Ground
import com.redmesastudios.simpletv.ui.theme.SimpleTVType
import kotlinx.coroutines.launch

// Everything started from SimpleTV, one row per app, in home-screen order (most-watched
// first). The remote's Person button opens it from anywhere. Back returns home.
@Composable
fun ContinueWatchingScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun load() = context.homeOrder(installedApps(StreamingApps) { context.packageManager.tvLaunchIntent(it) != null })
        .map { it to WatchHistory.forApp(context, it.packageName) }
        .filter { (_, titles) -> titles.isNotEmpty() }
    var rows by remember { mutableStateOf(load()) }
    LifecycleResumeEffect(Unit) {
        rows = load()
        onPauseOrDispose { }
    }
    var focused by remember { mutableStateOf<Pair<StreamingApp, Title>?>(null) }
    val shown = focused ?: rows.firstOrNull()?.let { (app, titles) -> app to titles.first() }
    val firstCard = remember { FocusRequester() }

    BackHandler(onBack = onClose)

    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(rows.isNotEmpty(), windowFocused) {
        if (!windowFocused || focused != null || rows.isEmpty()) return@LaunchedEffect
        runCatching { firstCard.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(Ground)) {
        FallScene(Modifier.fillMaxSize())
        Backdrop(shown?.second, Modifier.align(Alignment.TopEnd))
        Column(Modifier.fillMaxSize().padding(top = 32.dp)) {
            BasicText("Continue Watching", style = SimpleTVType.serviceName, modifier = Modifier.padding(horizontal = ScreenMargin))
            HeroText(
                shown?.second,
                heading = "Continue Watching",
                watchIn = shown?.first?.label,
                emptyMessage = "Shows and movies you start from SimpleTV will appear here.",
            )
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 40.dp),
            ) {
                rows.forEachIndexed { rowIndex, (app, titles) ->
                    item(key = app.packageName) {
                        ShowRow(
                            row = FolderRow(app.label, titles),
                            firstCard = if (rowIndex == 0) firstCard else null,
                            above = null,
                            onFocused = { focused = app to it },
                            onOpen = { title -> scope.launch { context.openTitle(app, title) } },
                        )
                    }
                }
            }
        }
    }
}
