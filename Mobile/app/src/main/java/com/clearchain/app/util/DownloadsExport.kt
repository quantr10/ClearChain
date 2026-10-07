package com.clearchain.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.clearchain.app.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Saving an export into the public Downloads folder and handing it to the share sheet, shared
 * by every screen that exports a CSV or PDF.
 */
object DownloadsExport {

    /** "yyyyMMdd_HHmm" stamp that keeps successive exports from overwriting each other. */
    fun timestamp(): String = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())

    /**
     * Creates [fileName] in Downloads and lets [write] fill it. Returns null when the Downloads
     * entry could not be created.
     */
    fun save(context: Context, fileName: String, mimeType: String, write: (OutputStream) -> Unit): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.also { uri ->
                context.contentResolver.openOutputStream(uri)?.use(write)
            }
        } else {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            FileOutputStream(file).use(write)
            Uri.fromFile(file)
        }

    /** Opens the system share sheet for a saved file. */
    fun share(context: Context, uri: Uri, mimeType: String, chooserTitle: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Writes [header] and [rows] as `<filePrefix>_<timestamp>.csv` in Downloads and shares it.
     * Returns the snackbar message describing the outcome.
     */
    suspend fun exportCsv(
        context: Context,
        filePrefix: String,
        chooserTitle: String,
        header: List<String>,
        rows: List<List<String>>
    ): String = withContext(Dispatchers.IO) {
        try {
            val csv = buildString {
                appendLine(header.joinToString(","))
                rows.forEach { row -> appendLine(row.joinToString(",") { csvField(it) }) }
            }
            val uri = save(context, "${filePrefix}_${timestamp()}.csv", "text/csv") { it.write(csv.toByteArray()) }
            if (uri != null) {
                share(context, uri, "text/csv", chooserTitle)
                context.getString(R.string.snack_csv_saved)
            } else {
                context.getString(R.string.snack_csv_failed)
            }
        } catch (e: Exception) {
            context.getString(R.string.snack_export_failed, e.message ?: "")
        }
    }

    /** Quotes a CSV field when it contains a comma or a double quote. */
    private fun csvField(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value
}
