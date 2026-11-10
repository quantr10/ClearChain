package com.clearchain.app.util

import android.graphics.Paint

/** [text], cut with an ellipsis where it would run past [maxWidth] in this paint. */
internal fun Paint.ellipsize(text: String, maxWidth: Float): String {
    if (measureText(text) <= maxWidth) return text
    val ellipsis = "…"
    val kept = breakText(text, true, maxWidth - measureText(ellipsis), null)
    return text.take(kept) + ellipsis
}
