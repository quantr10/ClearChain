package com.clearchain.app.presentation.grocery.managerequests

import com.clearchain.app.R
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.FilterChipData
import com.clearchain.app.presentation.components.RequestAction
import com.clearchain.app.presentation.components.SortOption

data class ManageRequestsState(
    val allRequests: List<PickupRequest> = emptyList(),
    val filteredRequests: List<PickupRequest> = emptyList(),

    // Search, Sort, Filter
    val searchQuery: String = "",
    val selectedSort: SortOption = CommonSortOptions.CREATED_DATE_DESC,
    val availableSortOptions: List<SortOption> = listOf(
        CommonSortOptions.CREATED_DATE_DESC,
        CommonSortOptions.CREATED_DATE_ASC,
        CommonSortOptions.EXPIRY_ASC,
        CommonSortOptions.EXPIRY_DESC,
        CommonSortOptions.NAME_ASC,
        CommonSortOptions.NAME_DESC
    ),
    val selectedStatus: String? = null,
    val availableStatusFilters: List<FilterChipData> = listOf(
        FilterChipData(null, labelResId = R.string.filter_all)
    ) + PickupRequestStatus.entries.map {
        FilterChipData(it.name, labelResId = it.labelResId)
    },

    // Advanced filter sheet
    val showFilterSheet: Boolean = false,
    val filterCategory: String? = null,
    val filterPickupDatePreset: String? = null, // null/"today"/"this_week"/"next_30"

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    /** Request id → the status change waiting on the backend for that card. */
    val pendingActions: Map<String, RequestAction> = emptyMap()
) {
    val requests: List<PickupRequest> get() = filteredRequests
    val activeFilterCount: Int get() =
        (if (filterCategory != null) 1 else 0) +
            (if (filterPickupDatePreset != null) 1 else 0)
}
