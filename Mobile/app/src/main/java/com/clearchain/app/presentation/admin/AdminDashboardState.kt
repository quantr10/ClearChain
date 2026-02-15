package com.clearchain.app.presentation.admin

import com.clearchain.app.domain.model.AdminStats

data class AdminDashboardState(
    val stats: AdminStats? = null,

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)