package com.clearchain.app.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Opens the phone dialer pre-filled with [phone]. Wrapped in runCatching so a device with no
 * dialer app doesn't crash.
 */
fun dialPhone(context: Context, phone: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
    }
}

/** Opens an email composer addressed to [email], with an optional [subject] pre-filled. */
fun sendEmail(context: Context, email: String, subject: String? = null) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri()).apply {
                subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            }
        )
    }
}
