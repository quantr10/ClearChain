package com.clearchain.app.presentation.admin.dashboard

sealed class AdminDashboardEvent {
    object LoadStats    : AdminDashboardEvent()
    object RefreshStats : AdminDashboardEvent()
    object ClearError   : AdminDashboardEvent()
}
