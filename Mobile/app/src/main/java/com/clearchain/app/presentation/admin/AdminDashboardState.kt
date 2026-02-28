package com.clearchain.app.presentation.admin

import com.clearchain.app.domain.model.AdminStats

data class AdminDashboardState(
    val stats: AdminStats? = null,
    
    // ✅ ADD: Recent activities/events
    val recentActivities: List<AdminActivity> = emptyList(),
    val recentAlerts: List<SystemAlert> = emptyList(),

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

// ✅ NEW: Activity types for admin dashboard
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

enum class AlertLevel {
    INFO,
    WARNING,
    ERROR
}