package com.clearchain.app.presentation.admin.analytics

sealed class AdminAnalyticsEvent {
    data object Load : AdminAnalyticsEvent()
    data object Refresh : AdminAnalyticsEvent()
    data object ExportPdf : AdminAnalyticsEvent()

    /** The one-off jump from an admin home tile has been performed. */
    data object FocusConsumed : AdminAnalyticsEvent()
}
