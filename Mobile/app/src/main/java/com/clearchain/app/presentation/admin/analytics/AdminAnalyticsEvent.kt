package com.clearchain.app.presentation.admin.analytics

sealed class AdminAnalyticsEvent {
    data object Load : AdminAnalyticsEvent()
    data object Refresh : AdminAnalyticsEvent()
    data object ExportPdf : AdminAnalyticsEvent()

    // Advanced filter sheet
    data object ShowFilterSheet : AdminAnalyticsEvent()
    data object HideFilterSheet : AdminAnalyticsEvent()
    /** Switches to the current period of [type]. */
    data class PeriodTypeSelected(val type: StatsPeriodType) : AdminAnalyticsEvent()
    data object PreviousPeriod : AdminAnalyticsEvent()
    data object NextPeriod : AdminAnalyticsEvent()
    data object ClearFilters : AdminAnalyticsEvent()

    /** The one-off jump from an admin home tile has been performed. */
    data object FocusConsumed : AdminAnalyticsEvent()
}
