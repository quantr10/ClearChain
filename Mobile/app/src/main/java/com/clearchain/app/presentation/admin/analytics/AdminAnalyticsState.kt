package com.clearchain.app.presentation.admin.analytics

import androidx.annotation.StringRes
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.AdminDetailedStatsData

data class AdminAnalyticsState(
    val data: AdminDetailedStatsData? = null,
    val period: StatsPeriodOption = StatsPeriodOption.ALL_TIME,

    /** Section the screen was opened at, from an admin home tile. Scrolled to once, then highlighted. */
    val focusedSection: AnalyticsSection? = null,

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null
)

/**
 * Ranges the API understands. They map straight onto the `preset` query parameter, so
 * adding one here means adding it to ResolveRange on the server as well.
 */
enum class StatsPeriodOption(val preset: String, @StringRes val labelRes: Int) {
    TODAY("today", R.string.preset_today),
    THIS_WEEK("week", R.string.preset_this_week),
    THIS_MONTH("month", R.string.preset_this_month),
    THIS_QUARTER("quarter", R.string.preset_this_quarter),
    ALL_TIME("all", R.string.preset_all_time)
}

/**
 * The sections the screen is built from, in the order they are laid out. The admin home
 * links into them by key, and the order doubles as the scroll index, so a section is
 * always rendered - an empty one says so rather than disappearing and shifting the rest.
 */
enum class AnalyticsSection(val key: String) {
    REQUESTS("requests"),
    TIMING("timing"),
    BACKLOG("backlog"),
    LEADERBOARDS("leaderboards"),
    QUALITY("quality"),
    ORGANIZATIONS("organizations");

    companion object {
        fun fromKey(key: String?): AnalyticsSection? =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
    }
}
