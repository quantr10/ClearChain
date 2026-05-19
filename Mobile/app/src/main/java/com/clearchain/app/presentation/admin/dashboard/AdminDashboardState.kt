package com.clearchain.app.presentation.admin.dashboard

import com.clearchain.app.data.remote.dto.AdminAlertItem
import com.clearchain.app.data.remote.dto.AdminHealthData
import com.clearchain.app.data.remote.dto.UserGrowthDay
import com.clearchain.app.domain.model.AdminStats

data class AdminDashboardState(
    val stats: AdminStats? = null,
    val recentActivities: List<AdminActivity> = emptyList(),
    val recentAlerts: List<SystemAlert> = emptyList(),
    val alertFeedItems: List<AdminAlertItem> = emptyList(),
    val healthData: AdminHealthData? = null,
    val userGrowthData: List<UserGrowthDay> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

data class AdminActivity(
    val id: String,
    val type: AdminActivityType,
    val title: String,
    val description: String,
    val timestamp: String,
    val icon: String = "📊"
)

enum class AdminActivityType {
    NEW_ORGANIZATION,
    TRANSACTION_COMPLETED,
    STATS_UPDATED
}

data class SystemAlert(
    val id: String,
    val level: AlertLevel,
    val message: String,
    val details: String?,
    val timestamp: String
)

enum class AlertLevel { INFO, WARNING, ERROR }
