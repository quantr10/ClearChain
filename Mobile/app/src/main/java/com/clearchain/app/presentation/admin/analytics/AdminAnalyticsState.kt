package com.clearchain.app.presentation.admin.analytics

import com.clearchain.app.data.remote.dto.AdminDetailedStatsData

data class AdminAnalyticsState(
    val data: AdminDetailedStatsData? = null,

    /** Section the screen was opened at, from an admin home tile. Scrolled to once, then highlighted. */
    val focusedSection: AnalyticsSection? = null,

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null
)

/**
 * The sections the screen is built from, in the order they are laid out. The admin home
 * links into them by key, and the order doubles as the scroll index, so a section is
 * always rendered - an empty one says so rather than disappearing and shifting the rest.
 */
enum class AnalyticsSection(val key: String) {
    REQUESTS("requests"),
    LEADERBOARDS("leaderboards"),
    QUALITY("quality"),
    ORGANIZATIONS("organizations");

    companion object {
        fun fromKey(key: String?): AnalyticsSection? =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
    }
}
