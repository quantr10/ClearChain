package com.clearchain.app.presentation.admin.analytics

sealed class AdminAnalyticsEvent {
    data object Load : AdminAnalyticsEvent()
    data object Refresh : AdminAnalyticsEvent()
    data class SelectPeriod(val period: StatsPeriodOption) : AdminAnalyticsEvent()
    data object ExportPdf : AdminAnalyticsEvent()
    data object ClearError : AdminAnalyticsEvent()
    /** The one-off jump from an admin home tile has been performed. */
    data object FocusConsumed : AdminAnalyticsEvent()
}
