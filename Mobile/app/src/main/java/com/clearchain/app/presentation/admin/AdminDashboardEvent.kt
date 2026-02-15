package com.clearchain.app.presentation.admin

sealed class AdminDashboardEvent {
    object LoadStats : AdminDashboardEvent()
    object RefreshStats : AdminDashboardEvent()
    object ClearError : AdminDashboardEvent()
}