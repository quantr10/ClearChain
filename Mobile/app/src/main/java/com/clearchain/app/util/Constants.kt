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
}
