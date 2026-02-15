package com.clearchain.app.presentation.admin.statistics

import com.clearchain.app.domain.model.AdminStats

data class StatisticsState(
    val stats: AdminStats? = null,

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)