package com.redmesastudios.simpletv

import android.service.dreams.DreamService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.redmesastudios.simpletv.ui.theme.SimpleTVType
import kotlinx.coroutines.delay

// The TV's own screensaver, chosen once over adb (see CLAUDE.md): the family photo slideshow,
// shown when the TV sits idle. Any button wakes it. A dream isn't an activity, so it hosts
// Compose itself and supplies the lifecycle Compose expects.
class PhotoDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        val content = ComposeView(this).apply { setContent { DreamSlideshow() } }
        window.decorView.setViewTreeLifecycleOwner(this)
        window.decorView.setViewTreeSavedStateRegistryOwner(this)
        content.setViewTreeLifecycleOwner(this)
        content.setViewTreeSavedStateRegistryOwner(this)
        setContentView(content)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
}

@Composable
private fun DreamSlideshow() {
    val context = LocalContext.current
    val photos = remember { PhotoStore.list(context).shuffled() }
    val seconds = remember { SlideshowTiming.seconds(context) }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(photos) {
        if (photos.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(seconds * 1_000L)
            index = (index + 1) % photos.size
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val photo = photos.getOrNull(index)
        if (photo != null) {
            Slide(photo, index, seconds)
        } else {
            BasicText(
                "Press Free TV on the remote to add family photos.",
                Modifier.align(Alignment.Center),
                style = SimpleTVType.serviceName.copy(color = Color.White, fontSize = 20.sp),
            )
        }
        ScreensaverClock(Modifier.align(Alignment.BottomEnd).padding(36.dp))
    }
}
