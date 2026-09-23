package com.clearchain.app.util

import android.content.Context
import com.clearchain.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {

    private const val ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    private const val DISPLAY_DATE_FORMAT = "MMM dd, yyyy"
    private const val DISPLAY_TIME_FORMAT = "hh:mm a"
    private const val DISPLAY_DATETIME_FORMAT = "MMM dd, yyyy 'at' hh:mm a"

    fun formatDate(isoDate: String): String {
        val formats = listOf(ISO_8601_FORMAT, "yyyy-MM-dd")
        for (fmt in formats) {
            try {
                val date = SimpleDateFormat(fmt, Locale.getDefault()).parse(isoDate) ?: continue
                return SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault()).format(date)
            } catch (_: Exception) {}
        }
        return isoDate
    }

    fun formatTime(isoDate: String): String {
        return try {
            val date = SimpleDateFormat(ISO_8601_FORMAT, Locale.getDefault()).parse(isoDate)
            SimpleDateFormat(DISPLAY_TIME_FORMAT, Locale.getDefault()).format(date ?: Date())
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatDateTime(isoDate: String): String {
        return try {
            val date = SimpleDateFormat(ISO_8601_FORMAT, Locale.getDefault()).parse(isoDate)
            SimpleDateFormat(DISPLAY_DATETIME_FORMAT, Locale.getDefault()).format(date ?: Date())
        } catch (e: Exception) {
            isoDate
        }
    }

    fun getTimeAgo(context: Context, isoDate: String): String {
        return try {
            val date = SimpleDateFormat(ISO_8601_FORMAT, Locale.getDefault()).parse(isoDate)
            val now = Date()
            val diff = now.time - (date?.time ?: 0)

            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff).toInt()
            val hours = TimeUnit.MILLISECONDS.toHours(diff).toInt()
            val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()

            when {
                seconds < 60 -> context.getString(R.string.time_just_now)
                minutes < 60 -> context.resources.getQuantityString(R.plurals.time_minutes_ago, minutes, minutes)
                hours < 24 -> context.resources.getQuantityString(R.plurals.time_hours_ago, hours, hours)
                days < 7 -> context.resources.getQuantityString(R.plurals.time_days_ago, days, days)
                else -> formatDate(isoDate)
            }
        } catch (e: Exception) {
            isoDate
        }
    }
}
