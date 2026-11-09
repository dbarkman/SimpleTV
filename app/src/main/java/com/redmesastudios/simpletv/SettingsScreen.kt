package com.redmesastudios.simpletv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redmesastudios.simpletv.ui.theme.Ground
import com.redmesastudios.simpletv.ui.theme.Ink
import com.redmesastudios.simpletv.ui.theme.InkSoft
import com.redmesastudios.simpletv.ui.theme.OnInk
import com.redmesastudios.simpletv.ui.theme.Outline
import com.redmesastudios.simpletv.ui.theme.SimpleTVType

// The home screen's Settings button: a QR code for the household's settings page on a
// phone (SettingsServer.kt), which runs only while this shows, and a way on to the TV's own
// settings.
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val pin = remember { newUploadPin() }
    var address by remember { mutableStateOf<String?>(null) }
    var started by remember { mutableStateOf(false) }
    val house by HouseSettings.state.collectAsState()
    val tvSettings = remember { FocusRequester() }
    DisposableEffect(pin) {
        val server = SettingsServer(context.applicationContext, pin)
        address = server.start()
        started = true
        onDispose { server.stop() }
    }
    LaunchedEffect(Unit) { runCatching { tvSettings.requestFocus() } }
    BackHandler { onClose() }

    Box(Modifier.fillMaxSize().background(Ground), contentAlignment = Alignment.Center) {
        HomeScene(modifier = Modifier.fillMaxSize())
        Row(
            Modifier.background(Ground, RoundedCornerShape(20.dp)).border(1.dp, Outline, RoundedCornerShape(20.dp)).padding(36.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            address?.let { QrCode(it, Modifier.size(230.dp)) }
            Column(Modifier.width(440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicText("Settings", style = SimpleTVType.heroTitle.copy(fontSize = 32.sp, lineHeight = 36.sp))
                when {
                    address != null -> {
                        BasicText(
                            "Scan this code with your phone's camera, on the same Wi-Fi as the TV, to change birthdays, the cards along the bottom and the holiday decorations.",
                            style = SimpleTVType.heroBody.copy(fontSize = 16.sp, lineHeight = 23.sp),
                        )
                        BasicText("Or open ${address!!.substringBefore("/?")} and enter code $pin", style = SimpleTVType.heroMeta.copy(fontSize = 13.sp))
                    }
                    started -> BasicText("The TV isn't connected to Wi-Fi right now.", style = SimpleTVType.heroBody)
                }
                BasicText(summary(house), style = SimpleTVType.serviceName.copy(fontSize = 15.sp))
                Spacer(Modifier.height(4.dp))
                SettingsButton("TV settings", Modifier.focusRequester(tvSettings)) { context.openSystemSettings() }
                BasicText("Back to leave", style = SimpleTVType.hint.copy(color = InkSoft))
            }
        }
    }
}

private fun summary(house: HouseState): String {
    val days = house.familyDays.size
    val cards = Channel.entries.size - house.hiddenCards.size
    return "${if (days == 1) "1 birthday or anniversary" else "$days birthdays and anniversaries"} · " +
        "${if (cards == 1) "1 card" else "$cards cards"} · ${house.packs.size} holiday ${if (house.packs.size == 1) "pack" else "packs"}"
}

@Composable
private fun SettingsButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val fill by animateColorAsState(if (focused) Ink else Ground, tween(200), label = "settingsButtonFill")
    val text by animateColorAsState(if (focused) OnInk else Ink, tween(200), label = "settingsButtonText")
    Box(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .background(fill, RoundedCornerShape(50))
            .border(1.5.dp, Ink, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        BasicText(label, style = SimpleTVType.button.copy(color = text, fontSize = 15.sp))
    }
}
