package com.redmesastudios.simpletv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import org.json.JSONObject
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

// Portrait and square photos show whole rather than cropped to the screen's shape.
fun showsWhole(width: Int, height: Int) = width.toFloat() / height < 1.2f

// How long each photo stays up, chosen with Up and Down during the slideshow and kept on the box.
object SlideshowTiming {
    val Choices = listOf(5, 8, 12, 20, 30, 60, 120)
    private const val Default = 12

    fun seconds(context: Context): Int =
        context.getSharedPreferences("screensaver", Context.MODE_PRIVATE).getInt("seconds", Default).takeIf { it in Choices } ?: Default

    fun change(context: Context, current: Int, longer: Boolean): Int {
        val next = nextChoice(current, longer)
        context.getSharedPreferences("screensaver", Context.MODE_PRIVATE).edit().putInt("seconds", next).apply()
        return next
    }

    fun nextChoice(current: Int, longer: Boolean): Int {
        val i = Choices.indexOf(current).takeIf { it >= 0 } ?: Choices.indexOf(Default)
        return Choices[(if (longer) i + 1 else i - 1).coerceIn(0, Choices.lastIndex)]
    }

    fun describe(seconds: Int): String = when {
        seconds >= 60 && seconds % 60 == 0 -> if (seconds == 60) "1 minute" else "${seconds / 60} minutes"
        else -> "$seconds seconds"
    }
}

// Only names this store made itself are ever read or deleted.
fun isPhotoName(name: String) = name.matches(Regex("[a-f0-9-]{36}\\.jpg"))

private const val RemovedFile = "photos_removed.json"

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
        Sync.changed(context)
        return name
    }

    // Removing a photo leaves a note of it, so the other sticks remove it too (see Sync).
    fun delete(context: Context, name: String): Boolean {
        val deleted = isPhotoName(name) && File(folder(context), name).delete()
        if (deleted) {
            synchronized(this) { writeRemoved(context, removed(context) + (name to System.currentTimeMillis())) }
            Sync.changed(context)
        }
        return deleted
    }

    // Photos removed on any stick: name to when.
    fun removed(context: Context): Map<String, Long> = runCatching {
        val json = JSONObject(File(context.filesDir, RemovedFile).readText())
        json.keys().asSequence().filter(::isPhotoName).associateWith { json.getLong(it) }
    }.getOrDefault(emptyMap())

    fun writeRemoved(context: Context, removed: Map<String, Long>) {
        File(context.filesDir, RemovedFile).writeText(JSONObject(removed).toString())
    }

    fun bytes(context: Context, name: String): ByteArray? =
        if (isPhotoName(name)) File(folder(context), name).takeIf { it.exists() }?.readBytes() else null

    // A photo another stick already resized, kept as it came with its own date.
    fun adopt(context: Context, name: String, bytes: ByteArray, modified: Long) {
        if (!isPhotoName(name)) return
        val target = File(folder(context), name)
        val partial = File(folder(context), "$name.part")
        partial.writeBytes(bytes)
        partial.renameTo(target)
        target.setLastModified(modified)
    }

    // A photo removed elsewhere, without leaving a new note.
    fun drop(context: Context, name: String) {
        if (isPhotoName(name)) File(folder(context), name).delete()
    }

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
