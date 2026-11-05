package de.elpifon.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Kontaktfotos werden verkleinert in den internen App-Speicher kopiert.
 * So bleiben sie auch erhalten, wenn das Original in der Galerie gelöscht wird.
 */
object Photos {
    private const val MAX_SIZE = 600

    private fun dir(context: Context) = File(context.filesDir, "fotos").apply { mkdirs() }

    fun file(context: Context, name: String?): File? =
        name?.let { File(dir(context), it) }?.takeIf { it.exists() }

    /** Kopiert das Bild hinter [uri] und gibt den neuen Dateinamen zurück (null bei Fehler). */
    suspend fun save(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_SIZE && bounds.outHeight / (sample * 2) >= MAX_SIZE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching null

            // Handyfotos sind oft nur per EXIF-Angabe gedreht – hier richtig herum drehen.
            val rotation = runCatching {
                resolver.openInputStream(uri)?.use {
                    when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                }
            }.getOrNull() ?: 0f
            if (rotation != 0f) {
                val m = Matrix().apply { postRotate(rotation) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            }

            val name = "foto-${System.currentTimeMillis()}.jpg"
            FileOutputStream(File(dir(context), name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            name
        }.getOrNull()
    }

    fun delete(context: Context, name: String?) {
        file(context, name)?.delete()
    }
}
