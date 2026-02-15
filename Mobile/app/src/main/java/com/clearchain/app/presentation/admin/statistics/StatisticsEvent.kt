package com.clearchain.app.presentation.admin.statistics

sealed class StatisticsEvent {
    object LoadStatistics : StatisticsEvent()
    object RefreshStatistics : StatisticsEvent()
    object ClearError : StatisticsEvent()
}