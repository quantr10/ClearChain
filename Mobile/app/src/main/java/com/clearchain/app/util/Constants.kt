package com.clearchain.app.util

import com.clearchain.app.BuildConfig

object Constants {

    // API
    /**
     * Base URL for the REST API, taken from API_BASE_URL in local.properties.
     *
     * The trailing slash is enforced rather than assumed: Retrofit rejects a base
     * URL without one, and TokenAuthenticator appends its path straight onto this
     * string, so a missing slash would silently produce ".../apiauth/refresh".
     */
    val BASE_URL: String = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }

    /**
     * Server root, without the /api path. The SignalR hubs are served from /hubs
     * at the root, not under the REST prefix.
     */
    val SERVER_ROOT_URL: String = BASE_URL.trimEnd('/').removeSuffix("/api")

    // Database
    const val DATABASE_NAME = "clearchain_db"

    // Preferences
    const val PREFERENCES_NAME = "clearchain_preferences"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_ID = "user_id"

    // Timeouts
    const val NETWORK_TIMEOUT = 30L // seconds

    // Pagination
    const val PAGE_SIZE = 20

    // Image
    const val MAX_IMAGE_SIZE_MB = 5
    const val IMAGE_QUALITY = 80
}
