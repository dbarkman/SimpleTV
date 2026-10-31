package com.redmesastudios.simpletv

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteButtonsTest {

    @Test
    fun appButtonsOpenTheirApps() {
        assertEquals(ButtonAction.OpenApp("com.google.android.youtube.tv"), buttonAction(KeyEvent.KEYCODE_BUTTON_3))
        assertEquals(ButtonAction.OpenApp("com.netflix.ninja"), buttonAction(KeyEvent.KEYCODE_BUTTON_4))
        assertEquals(ButtonAction.OpenApp("com.disney.disneyplus"), buttonAction(KeyEvent.KEYCODE_BUTTON_9))
        assertEquals(ButtonAction.OpenApp("com.cbs.ott"), buttonAction(KeyEvent.KEYCODE_BUTTON_10))
    }

    @Test
    fun appButtonsNameCuratedApps() {
        val curated = StreamingApps.map { it.packageName }.toSet()
        listOf(KeyEvent.KEYCODE_BUTTON_3, KeyEvent.KEYCODE_BUTTON_4, KeyEvent.KEYCODE_BUTTON_9, KeyEvent.KEYCODE_BUTTON_10)
            .forEach { assert((buttonAction(it) as ButtonAction.OpenApp).packageName in curated) }
    }

    @Test
    fun otherButtonsHaveTheirJobs() {
        assertEquals(ButtonAction.Search, buttonAction(KeyEvent.KEYCODE_MACRO_1))
        assertEquals(ButtonAction.ContinueWatching, buttonAction(KeyEvent.KEYCODE_PROFILE_SWITCH))
        assertEquals(ButtonAction.Settings, buttonAction(KeyEvent.KEYCODE_NOTIFICATION))
        assertEquals(ButtonAction.Media(KeyEvent.KEYCODE_MEDIA_PLAY), buttonAction(KeyEvent.KEYCODE_CHANNEL_UP))
        assertEquals(ButtonAction.Media(KeyEvent.KEYCODE_MEDIA_PAUSE), buttonAction(KeyEvent.KEYCODE_CHANNEL_DOWN))
        assertEquals(ButtonAction.Info, buttonAction(KeyEvent.KEYCODE_GUIDE))
        assertEquals(ButtonAction.Screensaver, buttonAction(KeyEvent.KEYCODE_FEATURED_APP_1))
    }

    @Test
    fun everydayButtonsPassThrough() {
        listOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_VOLUME_UP)
            .forEach { assertNull(buttonAction(it)) }
    }
}
