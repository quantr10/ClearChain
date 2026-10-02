package com.clearchain.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens the phone dialer pre-filled with [phone]. This exact three-line pattern (tel: intent,
 * wrapped in runCatching so a device with no dialer app doesn't crash) used to be duplicated
 * verbatim across five screens.
 */
fun dialPhone(context: Context, phone: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }
}

/**
 * Opens an email composer addressed to [email], with an optional [subject] pre-filled. Same
 * duplication history as [dialPhone].
 */
fun sendEmail(context: Context, email: String, subject: String? = null) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
                subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            }
        )
    }
}
