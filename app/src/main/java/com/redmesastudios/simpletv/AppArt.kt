package com.redmesastudios.simpletv

import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

// TV banners are 16:9 artwork with the service's logo; this is their native xhdpi size,
// which is about what a tile shows on the stick.
private const val BannerWidthPx = 320
private const val BannerHeightPx = 180
private const val IconPx = 96

sealed interface AppArt {
    val image: ImageBitmap

    // The app's own TV home-screen banner.
    data class Banner(override val image: ImageBitmap) : AppArt

    // Only a launcher icon: shown smaller, with the app's name.
    data class Icon(override val image: ImageBitmap) : AppArt
}

// Decoded at tile size rather than full resolution, to keep memory down on the stick.
fun PackageManager.appArt(packageName: String): AppArt? {
    val banner = tvLaunchIntent(packageName)?.let { runCatching { getActivityBanner(it) }.getOrNull() }
        ?: runCatching { getApplicationBanner(packageName) }.getOrNull()
    if (banner != null) return AppArt.Banner(banner.toBitmap(BannerWidthPx, BannerHeightPx).asImageBitmap())
    val icon = runCatching { getApplicationIcon(packageName) }.getOrNull() ?: return null
    return AppArt.Icon(icon.toBitmap(IconPx, IconPx).asImageBitmap())
}
