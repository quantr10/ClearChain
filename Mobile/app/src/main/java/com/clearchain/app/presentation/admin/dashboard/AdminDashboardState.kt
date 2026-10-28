package com.clearchain.app.presentation.admin.dashboard

import com.clearchain.app.data.remote.dto.AdminAlertItem
import com.clearchain.app.domain.model.AdminStats

data class AdminDashboardState(
    val stats: AdminStats? = null,
    val recentAlerts: List<SystemAlert> = emptyList(),
    val alertFeedItems: List<AdminAlertItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

data class SystemAlert(
    val id: String,
    val level: AlertLevel,
    val message: String,
    val details: String?,
    val timestamp: String
)

enum class AlertLevel { INFO, WARNING, ERROR }
