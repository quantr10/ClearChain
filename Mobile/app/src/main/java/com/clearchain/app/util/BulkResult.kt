package com.clearchain.app.util

import android.content.Context
import androidx.annotation.PluralsRes
import com.clearchain.app.R

/**
 * The one snackbar every bulk action reports with, so a partial or total failure is
 * never dressed up as success ("0 listings deleted"):
 * - all succeeded  -> "3 listings deleted"
 * - some failed    -> "2 listings deleted · 1 failed"
 * - none succeeded -> a plain error
 */
object BulkResult {
    fun message(context: Context, succeeded: Int, total: Int, @PluralsRes successRes: Int): String {
        val res = context.resources
        return when {
            succeeded >= total -> res.getQuantityString(successRes, succeeded, succeeded)
            succeeded == 0 -> context.getString(R.string.error_bulk_all_failed)
            else -> context.getString(
                R.string.snack_bulk_partial,
                res.getQuantityString(successRes, succeeded, succeeded),
                res.getQuantityString(R.plurals.snack_bulk_n_failed, total - succeeded, total - succeeded)
            )
        }
    }
}
