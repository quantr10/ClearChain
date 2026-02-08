package com.clearchain.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {

    private const val ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    private const val DISPLAY_DATE_FORMAT = "MMM dd, yyyy"
    private const val DISPLAY_TIME_FORMAT = "hh:mm a"
    private const val DISPLAY_DATETIME_FORMAT = "MMM dd, yyyy hh:mm a"

    fun formatDate(isoDate: String): String {
        return try {
            val date = SimpleDateFormat(ISO_8601_FORMAT, Locale.getDefault()).parse(isoDate)
            SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault()).format(date ?: Date())
        } catch (e: Exception) {
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

    fun getTimeAgo(isoDate: String): String {
        return try {
            val date = SimpleDateFormat(ISO_8601_FORMAT, Locale.getDefault()).parse(isoDate)
            val now = Date()
            val diff = now.time - (date?.time ?: 0)

            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
                hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
                days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
                else -> formatDate(isoDate)
            }
        } catch (e: Exception) {
            isoDate
        }
    }
}