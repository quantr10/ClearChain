package com.clearchain.app.presentation.admin.analytics

import com.clearchain.app.data.remote.dto.AdminDetailedStatsData
import java.time.LocalDate

data class AdminAnalyticsState(
    val data: AdminDetailedStatsData? = null,

    // Advanced filter sheet
    val showFilterSheet: Boolean = false,
    val period: AnalyticsPeriod = AnalyticsPeriod(),
    /** First UTC day with any period-scoped activity; stepping back stops at its period. */
    val firstDataDate: LocalDate? = null,

    /** Section the screen was opened at, from an admin home tile. Scrolled to once, then highlighted. */
    val focusedSection: AnalyticsSection? = null,

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null
) {
    val activeFilterCount: Int get() = if (period.type != StatsPeriodType.ALL) 1 else 0

    /** The days sent to the server as "yyyy-MM-dd", or null for all time. */
    val requestRange: Pair<String, String>? get() =
        period.range()?.let { (from, to) -> from.toString() to to.toString() }
}

/**
 * The sections the screen is built from, in the order they are laid out. The admin home
 * links into them by key, and the order doubles as the scroll index, so a section is
 * always rendered - an empty one says so rather than disappearing and shifting the rest.
 */
enum class AnalyticsSection(val key: String) {
    REQUESTS("requests"),
    BACKLOG("backlog"),
    LEADERBOARDS("leaderboards"),
    QUALITY("quality"),
    ORGANIZATIONS("organizations");

    companion object {
        fun fromKey(key: String?): AnalyticsSection? =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
    }
}
