package com.clearchain.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    private const val MAX_WIDTH = 1280
    private const val MAX_HEIGHT = 1280
    private const val QUALITY = 80  // JPEG quality %

    /**
     * Compress an image from a Uri to a temporary File.
     * Down-scales to MAX_WIDTH/MAX_HEIGHT while preserving aspect ratio,
     * then re-encodes as JPEG at QUALITY%.
     */
    fun compressImage(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open input stream for $uri")

        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val compressed = scaleBitmap(original)
        if (compressed !== original) original.recycle()

        val outFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            compressed.compress(Bitmap.CompressFormat.JPEG, QUALITY, fos)
        }
        compressed.recycle()

        return outFile
    }

    /**
     * Compress to a ByteArray (useful for direct upload streams).
     */
    fun compressToBytes(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open input stream for $uri")

        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val compressed = scaleBitmap(original)
        if (compressed !== original) original.recycle()

        val bos = ByteArrayOutputStream()
        compressed.compress(Bitmap.CompressFormat.JPEG, QUALITY, bos)
        compressed.recycle()

        return bos.toByteArray()
    }

    private fun scaleBitmap(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= MAX_WIDTH && h <= MAX_HEIGHT) return source

        val ratio = minOf(MAX_WIDTH.toFloat() / w, MAX_HEIGHT.toFloat() / h)
        val newW = (w * ratio).toInt()
        val newH = (h * ratio).toInt()
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
