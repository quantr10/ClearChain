package com.clearchain.app.presentation.admin.dashboard

sealed class AdminDashboardEvent {
    object RefreshStats : AdminDashboardEvent()
}
