package com.redmesastudios.simpletv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// Show artwork, kept in memory as 16-bit color (half the size; photos don't need alpha).
private val images = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 2
}

@Composable
fun rememberRemoteImage(url: String?): ImageBitmap? {
    // The state outlives a change of url, so each new url starts from its own cache entry.
    val image by produceState(url?.let { images.get(it) }, url) {
        value = url?.let { images.get(it) }
        if (url == null || value != null) return@produceState
        value = withContext(Dispatchers.IO) { runCatching { download(url) }.getOrNull() }?.also { images.put(url, it) }
    }
    return image
}

private fun download(url: String): ImageBitmap? {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 20_000
    }
    return try {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
        connection.inputStream.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
    } finally {
        connection.disconnect()
    }
}
