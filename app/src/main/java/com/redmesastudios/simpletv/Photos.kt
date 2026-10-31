package com.redmesastudios.simpletv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

// The screensaver's photos, kept in the app's own storage on the stick, already sized for
// the TV. They never leave the stick.
private const val MaxWidth = 1920
private const val MaxHeight = 1080
private const val ThumbSize = 360

// The size that fits within the box without stretching or growing.
fun fitWithin(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Pair<Int, Int> {
    val scale = min(1.0, min(maxWidth.toDouble() / width, maxHeight.toDouble() / height))
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}

// Only names this store made itself are ever read or deleted.
fun isPhotoName(name: String) = name.matches(Regex("[a-f0-9-]{36}\\.jpg"))

object PhotoStore {
    fun folder(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    // Newest first.
    fun list(context: Context): List<File> =
        folder(context).listFiles { f -> isPhotoName(f.name) }.orEmpty().sortedByDescending { it.lastModified() }

    // Decodes whatever a phone sends (JPEG, PNG, HEIC), turns it upright, shrinks it to TV
    // size and saves it as a JPEG. Returns the new file's name, or null if it isn't a picture.
    fun save(context: Context, bytes: ByteArray): String? {
        val bitmap = runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
                val (w, h) = fitWithin(info.size.width, info.size.height, MaxWidth, MaxHeight)
                decoder.setTargetSize(w, h)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.getOrNull() ?: return null
        val name = "${UUID.randomUUID()}.jpg"
        File(folder(context), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
        return name
    }

    fun delete(context: Context, name: String): Boolean = isPhotoName(name) && File(folder(context), name).delete()

    fun thumbnail(context: Context, name: String): ByteArray? {
        if (!isPhotoName(name)) return null
        val file = File(folder(context), name).takeIf { it.exists() } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val sample = Integer.highestOneBit(maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / ThumbSize))
        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        return java.io.ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}
