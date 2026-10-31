package com.redmesastudios.simpletv

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.redmesastudios.simpletv.ui.theme.Ground
import com.redmesastudios.simpletv.ui.theme.Ink
import com.redmesastudios.simpletv.ui.theme.InkSoft
import com.redmesastudios.simpletv.ui.theme.Outline
import com.redmesastudios.simpletv.ui.theme.SimpleTVType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Family photos, one at a time, each drifting slowly and fading into the next, with a quiet
// clock in the corner. Left and Right step through them, Up and Down change how long each
// stays up, OK opens "Add photos", and Back (or any other button) leaves.
@Composable
fun ScreensaverScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf(PhotoStore.list(context).shuffled()) }
    var adding by remember { mutableStateOf(photos.isEmpty()) }
    var index by remember { mutableIntStateOf(0) }
    var step by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(SlideshowTiming.seconds(context)) }
    // Shown briefly after Up or Down: "Each photo: 20 seconds".
    var timingNote by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(timingNote) { if (timingNote != null) { delay(2_500); timingNote = null } }
    val focus = remember { FocusRequester() }

    // The TV's own screensaver mustn't take over the slideshow.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(adding) { if (!adding) runCatching { focus.requestFocus() } }
    // Moves on every few seconds; stepping by hand or changing the timing restarts the wait.
    LaunchedEffect(photos, step, adding, seconds) {
        if (adding || photos.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(seconds * 1_000L)
            index = (index + 1) % photos.size
        }
    }

    BackHandler {
        if (adding && photos.isNotEmpty()) adding = false else onClose()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (adding || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> { index = (index + 1) % photos.size; step++ }
                    Key.DirectionLeft -> { index = (index - 1 + photos.size) % photos.size; step++ }
                    Key.DirectionUp, Key.DirectionDown -> {
                        seconds = SlideshowTiming.change(context, seconds, longer = event.key == Key.DirectionUp)
                        timingNote = "Each photo: ${SlideshowTiming.describe(seconds)}"
                        step++
                    }
                    Key.DirectionCenter, Key.Enter -> adding = true
                    // Back is the BackHandler's; volume belongs to the TV.
                    Key.Back, Key.VolumeUp, Key.VolumeDown, Key.VolumeMute -> return@onPreviewKeyEvent false
                    else -> onClose()
                }
                true
            },
    ) {
        photos.getOrNull(index)?.let { Slide(it, index, seconds) }
        ScreensaverClock(Modifier.align(Alignment.BottomEnd).padding(36.dp))
        AnimatedVisibility(timingNote != null, Modifier.align(Alignment.TopCenter).padding(top = 40.dp), enter = fadeIn(tween(200)), exit = fadeOut(tween(400))) {
            BasicText(
                timingNote.orEmpty(),
                Modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)).padding(horizontal = 22.dp, vertical = 10.dp),
                style = SimpleTVType.serviceName.copy(color = Color.White, fontSize = 18.sp),
            )
        }
        AnimatedVisibility(adding, enter = fadeIn(tween(300)), exit = fadeOut(tween(300))) {
            AddPhotos(
                count = photos.size,
                onChange = { photos = PhotoStore.list(context).shuffled().also { if (index >= it.size) index = 0 } },
            )
        }
    }
}

// A decoded photo, plus a tiny copy for the soft background behind portrait photos.
private class SlideImage(val photo: ImageBitmap, val backdrop: ImageBitmap?)

// One photo, crossfading from the last. Landscape photos fill the screen, slowly zooming and
// drifting. Portrait and square ones show whole, full height, over a blurred, enlarged copy
// of themselves, so no one's head gets cut off.
@Composable
internal fun Slide(file: File, index: Int, seconds: Int) {
    val image by produceState<SlideImage?>(null, file) {
        value = withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(file.path) ?: return@withContext null
            // Shrunk to a few dozen pixels and stretched back up, a photo reads as a soft blur.
            val backdrop = if (showsWhole(bitmap.width, bitmap.height)) {
                android.graphics.Bitmap.createScaledBitmap(bitmap, maxOf(1, bitmap.width / 24), maxOf(1, bitmap.height / 24), true).asImageBitmap()
            } else {
                null
            }
            SlideImage(bitmap.asImageBitmap(), backdrop)
        }
    }
    Crossfade(image, animationSpec = tween(1500), label = "slide") { shown ->
        if (shown == null) return@Crossfade
        val zoom = remember { Animatable(1f) }
        val backdrop = shown.backdrop
        LaunchedEffect(shown) {
            zoom.animateTo(if (backdrop == null) 1.08f else 1.03f, tween((seconds + 2) * 1_000, easing = LinearEasing))
        }
        if (backdrop != null) {
            Box(Modifier.fillMaxSize()) {
                Image(backdrop, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = FilterQuality.High)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                Image(
                    shown.photo,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = zoom.value; scaleY = zoom.value },
                )
            }
            return@Crossfade
        }
        // Alternate the drift so consecutive photos don't all slide the same way.
        val drift = if (index % 2 == 0) 1f else -1f
        Image(
            shown.photo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = zoom.value
                scaleY = zoom.value
                translationX = (zoom.value - 1f) * size.width * 0.25f * drift
            },
        )
    }
}

@Composable
internal fun ScreensaverClock(modifier: Modifier) {
    val now by rememberMinuteClock()
    val shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 8f)
    Column(modifier, horizontalAlignment = Alignment.End) {
        BasicText(now.format(TimeFormat), style = SimpleTVType.clock.copy(color = Color.White, fontSize = 34.sp, shadow = shadow))
        BasicText(now.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d")), style = SimpleTVType.date.copy(color = Color.White, shadow = shadow))
    }
}

// Runs the upload page while it's showing and gives phones a QR code to reach it.
@Composable
private fun AddPhotos(count: Int, onChange: () -> Unit) {
    val context = LocalContext.current
    val pin = remember { newUploadPin() }
    val scope = rememberCoroutineScope()
    var address by remember { mutableStateOf<String?>(null) }
    var started by remember { mutableStateOf(false) }
    DisposableEffect(pin) {
        // Uploads arrive on the server's threads; the photo list updates on the main one.
        val server = PhotoUploadServer(context.applicationContext, pin) { scope.launch { onChange() } }
        address = server.start()
        started = true
        onDispose { server.stop() }
    }
    Box(Modifier.fillMaxSize().background(Ground), contentAlignment = Alignment.Center) {
        FallScene(Modifier.fillMaxSize())
        Row(
            Modifier.background(Ground, RoundedCornerShape(20.dp)).border(1.dp, Outline, RoundedCornerShape(20.dp)).padding(36.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            address?.let { QrCode(it, Modifier.size(230.dp)) }
            Column(Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicText("Add family photos", style = SimpleTVType.heroTitle.copy(fontSize = 32.sp, lineHeight = 36.sp))
                when {
                    address != null -> {
                        BasicText(
                            "Scan this code with your phone's camera, on the same Wi-Fi as the TV. Pick photos, and they'll appear here.",
                            style = SimpleTVType.heroBody.copy(fontSize = 16.sp, lineHeight = 23.sp),
                        )
                        BasicText("Or open ${address!!.substringBefore("/?")} and enter code $pin", style = SimpleTVType.heroMeta.copy(fontSize = 13.sp))
                    }
                    started -> BasicText("The TV isn't connected to Wi-Fi right now.", style = SimpleTVType.heroBody)
                }
                BasicText(
                    if (count == 0) "No photos yet." else if (count == 1) "1 photo on the TV." else "$count photos on the TV.",
                    style = SimpleTVType.serviceName.copy(fontSize = 16.sp),
                )
                BasicText(
                    if (count == 0) "Back to leave" else "Back to watch the slideshow · Up and Down change how long each photo shows",
                    style = SimpleTVType.hint.copy(color = InkSoft),
                )
            }
        }
    }
}

@Composable
private fun QrCode(text: String, modifier: Modifier) {
    val matrix = remember(text) {
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1))
    }
    Canvas(modifier.background(Color.White, RoundedCornerShape(10.dp)).padding(10.dp)) {
        val cell = size.width / matrix.width
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            if (matrix.get(x, y)) drawRect(Ink, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}
