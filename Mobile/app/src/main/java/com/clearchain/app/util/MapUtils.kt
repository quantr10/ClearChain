package com.clearchain.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens [query] in the Google Maps app, falling back to the browser map when
 * Maps isn't installed. [query] may be a "lat,lng" pair or a free-text address.
 */
fun openInGoogleMaps(context: Context, query: String) {
    val encoded = Uri.encode(query)
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")).apply {
                setPackage("com.google.android.apps.maps")
            }
        )
    }.onFailure {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$encoded"))
        )
    }
}

/**
 * Best directions target for an org: exact coordinates when known, otherwise the
 * text address the caller already assembled.
 */
fun mapsQuery(latitude: Double?, longitude: Double?, addressFallback: String): String =
    if (latitude != null && longitude != null) "$latitude,$longitude" else addressFallback
