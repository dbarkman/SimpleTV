package com.redmesastudios.simpletv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Ask SimpleTV to open a screen, from inside or outside the app (the remote's Star, Person and Free TV buttons).
const val OpenSearch = "com.redmesastudios.simpletv.OPEN_SEARCH"
const val OpenContinueWatching = "com.redmesastudios.simpletv.OPEN_CONTINUE_WATCHING"
const val OpenScreensaver = "com.redmesastudios.simpletv.OPEN_SCREENSAVER"

private sealed interface Screen {
    data object Home : Screen
    data object Search : Screen
    data object ContinueWatching : Screen
    data object Screensaver : Screen
    data class Info(val channel: Channel) : Screen
    data class Folder(val app: StreamingApp) : Screen
}

// A screen asked for from outside, numbered so asking for the same one twice still counts.
private data class ScreenRequest(val screen: Screen, val number: Int)

private fun requestedScreen(action: String?): Screen? = when (action) {
    OpenSearch -> Screen.Search
    OpenContinueWatching -> Screen.ContinueWatching
    OpenScreensaver -> Screen.Screensaver
    else -> null
}

class MainActivity : ComponentActivity() {
    // Bumped by Home presses that arrive while SimpleTV is running.
    private val homePresses = mutableIntStateOf(0)
    private var screenRequest by mutableStateOf<ScreenRequest?>(null)

    private fun request(screen: Screen) {
        screenRequest = ScreenRequest(screen, (screenRequest?.number ?: 0) + 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Continue Watching and the family photos, shared with the household's other sticks.
        Sync.start(applicationContext)
        requestedScreen(intent?.action)?.let(::request)
        setContent {
            var screen by remember { mutableStateOf<Screen>(Screen.Home) }
            // Which tile or button to land on when coming back home.
            var returnFocusTo by remember { mutableStateOf<String?>(null) }

            // Home always means the home screen, on its top-left app, even from inside a
            // folder or search.
            val presses = homePresses.intValue
            LaunchedEffect(presses) {
                screen = Screen.Home
                returnFocusTo = null
            }
            val requested = screenRequest
            LaunchedEffect(requested) { requested?.let { screen = it.screen } }

            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label = "screen",
            ) { shown ->
                when (shown) {
                    Screen.Home -> HomeScreen(
                        returnFocusTo = returnFocusTo,
                        arrival = presses,
                        onOpenFolder = {
                            returnFocusTo = it.packageName
                            screen = Screen.Folder(it)
                        },
                        onOpenSearch = {
                            returnFocusTo = SearchButtonFocus
                            screen = Screen.Search
                        },
                        onOpenChannel = {
                            returnFocusTo = glanceFocus(it)
                            screen = Screen.Info(it)
                        },
                    )
                    Screen.Search -> SearchScreen(onClose = { screen = Screen.Home })
                    Screen.ContinueWatching -> ContinueWatchingScreen(onClose = { screen = Screen.Home })
                    Screen.Screensaver -> ScreensaverScreen(onClose = { screen = Screen.Home })
                    is Screen.Info -> ChannelScreen(shown.channel, onClose = { screen = Screen.Home })
                    is Screen.Folder -> FolderScreen(shown.app, onClose = { screen = Screen.Home })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Sync.pullSoon()
        // The show guide refreshes at most once a day, in the background, for the known
        // services and any installed app matched to a service by name.
        lifecycleScope.launch {
            ServiceDirectory.refreshIfStale(applicationContext)
            val matched = homeApps(installedTvApps()).map(ServiceDirectory.services.value::resolve)
            CatalogStore.refreshIfStale(applicationContext, (StreamingApps + matched).distinctBy { it.packageName })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val requested = requestedScreen(intent.action)
        when {
            requested != null -> request(requested)
            intent.hasCategory(Intent.CATEGORY_HOME) -> homePresses.intValue++
        }
    }
}
