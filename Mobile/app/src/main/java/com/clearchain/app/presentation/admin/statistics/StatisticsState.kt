package com.clearchain.app.presentation.admin.statistics

import com.clearchain.app.data.remote.dto.AdminDetailedStatsData
import com.clearchain.app.domain.model.AdminStats
import com.clearchain.app.domain.model.Organization

data class StatisticsState(
    val stats: AdminStats? = null,

    // Date-range detailed stats (leaderboards, category breakdown, impact)
    val selectedPreset: String = "all",
    val detailedStats: AdminDetailedStatsData? = null,
    val isLoadingDetailed: Boolean = false,

    // Period-over-period comparison (previous period)
    val previousDetailedStats: AdminDetailedStatsData? = null,
    val isLoadingPrevious: Boolean = false,

    // Geographic heatmap data
    val orgLocations: List<Organization> = emptyList(),

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)