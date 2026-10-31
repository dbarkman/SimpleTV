package com.redmesastudios.simpletv

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redmesastudios.simpletv.ui.theme.Ground
import com.redmesastudios.simpletv.ui.theme.Ink
import com.redmesastudios.simpletv.ui.theme.InkSoft
import com.redmesastudios.simpletv.ui.theme.OnInk
import com.redmesastudios.simpletv.ui.theme.SimpleTVType

// Holding OK this long on an app tile offers to remove the app; a normal press is far shorter.
const val HoldToRemoveMillis = 1_500L

val SelectKeys = setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)

// True once a held OK key (still repeating) has been down long enough to count as a hold.
fun isHoldToRemove(event: KeyEvent): Boolean =
    event.action == KeyEvent.ACTION_DOWN && event.keyCode in SelectKeys &&
        event.repeatCount > 0 && isHeldLongEnough(event.downTime, event.eventTime)

fun isHeldLongEnough(downTime: Long, eventTime: Long): Boolean = eventTime - downTime >= HoldToRemoveMillis

enum class RemoveChoice { Remove, Cancel }

// Apps that came with the TV can't be uninstalled, only turned off in their settings page.
fun Context.cameWithTv(app: StreamingApp): Boolean = runCatching {
    packageManager.getApplicationInfo(app.packageName, 0).flags and ApplicationInfo.FLAG_SYSTEM != 0
}.getOrDefault(false)

// Hands over to Android's own uninstall confirmation, or the app's settings page for an app
// that came with the TV. The home screen re-checks its apps when it comes back.
fun Context.removeApp(app: StreamingApp) {
    val intent = if (cameWithTv(app)) {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
    } else {
        Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
    }
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

// "Remove Tubi from this TV?" over the home screen. It takes no focus: the home screen
// steers it with Left, Right, OK and Back, so the tile underneath keeps its place.
@Composable
fun RemoveAppPrompt(app: StreamingApp?, builtIn: Boolean, choice: RemoveChoice) {
    // Keep showing the last app while the prompt fades out.
    var shown by remember { mutableStateOf(app) }
    if (app != null) shown = app

    AnimatedVisibility(app != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                app != null,
                enter = scaleIn(tween(220), initialScale = 0.92f),
                exit = scaleOut(tween(200), targetScale = 0.92f),
            ) {
                val label = shown?.label.orEmpty()
                Column(
                    Modifier
                        .widthIn(max = 460.dp)
                        .background(Ground, RoundedCornerShape(24.dp))
                        .padding(horizontal = 36.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BasicText(
                        if (builtIn) "Turn off $label?" else "Remove $label from this TV?",
                        style = SimpleTVType.greeting.copy(fontSize = 22.sp, textAlign = TextAlign.Center),
                    )
                    BasicText(
                        if (builtIn) "$label came with this TV, so it can only be turned off in its settings."
                        else "You can get it again anytime from Get Apps.",
                        style = SimpleTVType.hint.copy(fontSize = 14.sp, textAlign = TextAlign.Center),
                    )
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PromptButton(if (builtIn) "Open settings" else "Remove", choice == RemoveChoice.Remove)
                        PromptButton("Cancel", choice == RemoveChoice.Cancel)
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptButton(label: String, selected: Boolean) {
    val scale by animateFloatAsState(if (selected) 1.07f else 1f, tween(200), label = "promptScale")
    val container by animateColorAsState(if (selected) Ink else Ink.copy(alpha = 0.08f), tween(200), label = "promptFill")
    val content by animateColorAsState(if (selected) OnInk else InkSoft, tween(200), label = "promptText")
    Box(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(container, CircleShape)
            .padding(horizontal = 22.dp, vertical = 9.dp),
    ) {
        BasicText(label, style = SimpleTVType.button.copy(fontSize = 15.sp), color = { content })
    }
}
