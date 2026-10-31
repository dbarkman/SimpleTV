package com.redmesastudios.simpletv

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime

// What each of the remote's extra buttons does.
sealed interface ButtonAction {
    data class OpenApp(val packageName: String) : ButtonAction
    data object Search : ButtonAction
    data object ContinueWatching : ButtonAction
    data object Settings : ButtonAction
    data class Media(val keyCode: Int) : ButtonAction
    data object Info : ButtonAction
    data object Screensaver : ButtonAction
}

// Key codes as the onn remote sends them. Buttons not listed here pass through untouched.
fun buttonAction(keyCode: Int): ButtonAction? = when (keyCode) {
    KeyEvent.KEYCODE_BUTTON_3 -> ButtonAction.OpenApp(YouTubePackage)
    KeyEvent.KEYCODE_BUTTON_4 -> ButtonAction.OpenApp("com.netflix.ninja")
    KeyEvent.KEYCODE_BUTTON_9 -> ButtonAction.OpenApp("com.disney.disneyplus")
    KeyEvent.KEYCODE_BUTTON_10 -> ButtonAction.OpenApp("com.cbs.ott")
    // Free TV: the family photo screensaver.
    KeyEvent.KEYCODE_FEATURED_APP_1 -> ButtonAction.Screensaver
    KeyEvent.KEYCODE_MACRO_1 -> ButtonAction.Search
    KeyEvent.KEYCODE_PROFILE_SWITCH -> ButtonAction.ContinueWatching
    KeyEvent.KEYCODE_NOTIFICATION -> ButtonAction.Settings
    KeyEvent.KEYCODE_CHANNEL_UP -> ButtonAction.Media(KeyEvent.KEYCODE_MEDIA_PLAY)
    KeyEvent.KEYCODE_CHANNEL_DOWN -> ButtonAction.Media(KeyEvent.KEYCODE_MEDIA_PAUSE)
    // The TV button (the old Guide button).
    KeyEvent.KEYCODE_GUIDE, KeyEvent.KEYCODE_TV -> ButtonAction.Info
    else -> null
}

// The remote's extra buttons (app buttons, Star, Person, Settings, Free TV, channel) were
// handled by the stock home screen, which is disabled. This service sees them system-wide,
// even while another app is playing, and gives them their jobs back. Enabled once over adb
// (see CLAUDE.md).
class RemoteButtons : AccessibilityService() {

    // The app in front, from window changes; system windows (keyboard, volume) don't count.
    private var foregroundPackage: String? = null
    private val main = Handler(Looper.getMainLooper())
    private var prompt: SwitchPrompt? = null
    private var panel: InfoOverlay? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    // Keys whose press this service took, so their release is kept from the app too.
    private val heldKeys = mutableSetOf<Int>()

    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS }
        // Keeps the info panel's weather ready to show.
        scope.launch {
            while (true) {
                GlanceStore.refreshIfStale()
                delay(15 * 60_000L)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // The prompt's own window reports as SimpleTV too; only SimpleTV's screen counts.
        val counts = if (pkg == packageName) event.className == MainActivity::class.java.name else packageManager.tvLaunchIntent(pkg) != null
        if (!counts || pkg == foregroundPackage) return
        foregroundPackage = pkg
        // Leaving the app a show was started in means that show is over.
        NowPlaying.read(this)?.let { if (it.packageName != pkg) NowPlaying.forget(this) }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && heldKeys.remove(event.keyCode)) return true
        if (event.action != KeyEvent.ACTION_DOWN) return buttonAction(event.keyCode) != null
        val taken = panel?.let { answerPanel(event) } ?: prompt?.let { answerPrompt(it, event) } ?: run {
            // Back inside an app leaves the show SimpleTV started (to its menus or another
            // show), so the info panel stops naming it. The app still gets the press.
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (foregroundPackage != packageName) NowPlaying.forget(this)
                return false
            }
            val action = buttonAction(event.keyCode) ?: return false
            // The job runs once, on the way down; holding the button doesn't repeat it.
            if (event.repeatCount == 0) perform(action)
            true
        }
        if (taken) heldKeys += event.keyCode
        return taken
    }

    // While "Switch to …?" is up: OK (or the same button again) switches, Back keeps
    // watching, and any other button closes it and does its usual thing.
    private fun answerPrompt(open: SwitchPrompt, event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return true
        val sameButton = buttonAction(event.keyCode) == open.action
        return when {
            event.keyCode in ConfirmKeys || sameButton -> {
                dismissPrompt()
                open.go()
                true
            }
            event.keyCode == KeyEvent.KEYCODE_BACK -> {
                dismissPrompt()
                true
            }
            else -> {
                dismissPrompt()
                buttonAction(event.keyCode)?.let { perform(it); true } ?: false
            }
        }
    }

    // While the info panel is up: Up and Down scroll a long description (and keep the panel
    // up a while longer), the TV button or Back closes it, and any other button closes it
    // and does its usual thing.
    private fun answerPanel(event: KeyEvent): Boolean {
        val open = panel ?: return false
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN || event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (panelCanScroll(open.view)) {
                scrollPanel(open.view, down = event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                main.removeCallbacks(open.timeout)
                main.postDelayed(open.timeout, PanelSeconds * 1_000L)
                return true
            }
        }
        if (event.repeatCount > 0) return true
        dismissPanel()
        val action = buttonAction(event.keyCode)
        return when {
            action == ButtonAction.Info || event.keyCode == KeyEvent.KEYCODE_BACK -> true
            action != null -> {
                perform(action)
                true
            }
            else -> false
        }
    }

    private fun perform(action: ButtonAction) {
        when (action) {
            is ButtonAction.OpenApp -> openApp(action.packageName)
            ButtonAction.Search -> openSimpleTV(OpenSearch)
            ButtonAction.Screensaver -> when (foregroundPackage) {
                null, packageName -> openSimpleTV(OpenScreensaver)
                else -> showPrompt("Photos", action) { openSimpleTV(OpenScreensaver) }
            }
            ButtonAction.ContinueWatching -> openSimpleTV(OpenContinueWatching)
            ButtonAction.Settings -> openSystemSettings()
            // Goes to whichever app is playing, through its media session.
            is ButtonAction.Media -> {
                val audio = getSystemService(AudioManager::class.java)
                val now = SystemClock.uptimeMillis()
                audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, action.keyCode, 0))
                audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, action.keyCode, 0))
            }
            ButtonAction.Info -> showPanel()
        }
    }

    private fun openSimpleTV(request: String) {
        startActivity(Intent(request).setClass(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // App buttons are easy to bump from the couch: over a playing app they ask first.
    // From SimpleTV's own screens they switch straight away.
    private fun openApp(target: String) {
        if (packageManager.tvLaunchIntent(target) == null) return
        when (foregroundPackage) {
            target -> Unit
            null, packageName -> launch(target)
            else -> showPrompt(appLabel(target), ButtonAction.OpenApp(target)) { launch(target) }
        }
    }

    private fun launch(target: String) {
        packageManager.tvLaunchIntent(target)?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun appLabel(target: String) = StreamingApps.firstOrNull { it.packageName == target }?.label
        ?: packageManager.getApplicationLabel(packageManager.getApplicationInfo(target, 0)).toString()

    // "Switch to <label>?" for a button whose job would take over the screen.
    private fun showPrompt(label: String, action: ButtonAction, go: () -> Unit) {
        dismissPrompt()
        val view = promptView(label)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // The app underneath keeps its focus; this service answers the keys.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }
        getSystemService(WindowManager::class.java).addView(view, params)
        view.alpha = 0f
        view.scaleX = 0.96f
        view.scaleY = 0.96f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
        val timeout = Runnable { dismissPrompt() }
        prompt = SwitchPrompt(action, go, view, timeout)
        main.postDelayed(timeout, PromptSeconds * 1_000L)
    }

    // Over a playing app: what's on (when SimpleTV started it), the time and the weather.
    // Closes by itself after a few seconds.
    private fun showPanel() {
        val foreground = foregroundPackage ?: return
        if (foreground == packageName) return
        val label = StreamingApps.firstOrNull { it.packageName == foreground }?.label
            ?: runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(foreground, 0)).toString() }.getOrDefault("TV")
        val started = NowPlaying.read(this)
        fun info(title: NowPlaying.Started?) =
            panelInfo(foreground, label, title, GlanceStore.glance.value.weather, LocalDateTime.now(), Instant.now())
        val view = infoPanelView(this, info(started))
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
            val margin = (48 * resources.displayMetrics.density).toInt()
            horizontalMargin = 0f
            y = margin / 2
            width = resources.displayMetrics.widthPixels - 2 * margin
        }
        getSystemService(WindowManager::class.java).addView(view, params)
        view.alpha = 0f
        view.translationY = 40f
        view.animate().alpha(1f).translationY(0f).setDuration(250).start()
        val timeout = Runnable { dismissPanel() }
        val open = InfoOverlay(view, timeout)
        panel = open
        main.postDelayed(timeout, PanelSeconds * 1_000L)
        // Older guide entries have no score yet; fetch it and fill it in if the panel is still up.
        val title = started?.title
        if (title != null && title.rating == 0.0 && title.tmdbId > 0 && title.kind != TitleKind.Video) {
            scope.launch {
                val rated = runCatching { Tmdb.details(title.kind, title.tmdbId) }.getOrNull() ?: return@launch
                if (panel !== open) return@launch
                val updated = info(started.copy(title = title.copy(rating = rated.rating)))
                (open.view as? android.view.ViewGroup)?.let { swapPanelContent(it, updated) }
            }
        }
        // A video's saved description can be an old, shortened copy (Continue Watching keeps
        // its own); fetch the current one and fill it in if the panel is still up.
        val videoId = title?.videoId
        if (title != null && videoId != null) {
            scope.launch {
                val token = runCatching { YouTubeAccount.token(this@RemoteButtons) }.getOrNull() ?: return@launch
                val full = runCatching { YouTubeApi.description(token, videoId) }.getOrNull() ?: return@launch
                if (panel !== open || full.isEmpty() || full == title.overview) return@launch
                val updated = info(started.copy(title = title.copy(overview = full)))
                (open.view as? android.view.ViewGroup)?.let { swapPanelContent(it, updated) }
            }
        }
    }

    private fun swapPanelContent(container: android.view.ViewGroup, info: PanelInfo) {
        val fresh = infoPanelView(this, info) as android.view.ViewGroup
        container.removeAllViews()
        while (fresh.childCount > 0) {
            val child = fresh.getChildAt(0)
            val params = child.layoutParams
            fresh.removeViewAt(0)
            container.addView(child, params)
        }
    }

    private fun dismissPanel() {
        val open = panel ?: return
        panel = null
        main.removeCallbacks(open.timeout)
        open.view.animate().alpha(0f).translationY(40f).setDuration(200).withEndAction {
            runCatching { getSystemService(WindowManager::class.java).removeView(open.view) }
        }.start()
    }

    private fun dismissPrompt() {
        val open = prompt ?: return
        prompt = null
        main.removeCallbacks(open.timeout)
        open.view.animate().alpha(0f).setDuration(200).withEndAction {
            runCatching { getSystemService(WindowManager::class.java).removeView(open.view) }
        }.start()
    }

    private fun promptView(label: String): View {
        fun dp(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
        val font = ResourcesCompat.getFont(this, R.font.manrope)
        fun text(value: String, sizeSp: Float, weight: Int, color: Int) = TextView(this).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(color)
            typeface = Typeface.create(font, weight, false)
            gravity = Gravity.CENTER
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32f).toInt(), dp(22f).toInt(), dp(32f).toInt(), dp(22f).toInt())
            background = GradientDrawable().apply {
                setColor(PromptFill)
                cornerRadius = dp(16f)
                setStroke(dp(1f).toInt(), PromptEdge)
            }
            elevation = dp(12f)
            addView(text("Switch to $label?", 22f, 800, PromptInk))
            addView(text("Press OK to switch  ·  Back to keep watching", 13f, 500, PromptInkSoft).apply {
                setPadding(0, dp(8f).toInt(), 0, 0)
            })
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        dismissPrompt()
        dismissPanel()
        scope.cancel()
        super.onDestroy()
    }

    private class SwitchPrompt(val action: ButtonAction, val go: () -> Unit, val view: View, val timeout: Runnable)
    private class InfoOverlay(val view: View, val timeout: Runnable)

    private companion object {
        const val PromptSeconds = 6
        const val PanelSeconds = 10
        val ConfirmKeys = setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)
        // The fall palette (ui/theme/Color.kt), as plain colors for a View.
        const val PromptFill = 0xFFF4ECE1.toInt()
        const val PromptEdge = 0xFF9C8C7E.toInt()
        const val PromptInk = 0xFF2A211B.toInt()
        const val PromptInkSoft = 0xFF6B5D52.toInt()
    }
}
