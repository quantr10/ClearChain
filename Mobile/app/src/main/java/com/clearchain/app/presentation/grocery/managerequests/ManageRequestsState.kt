package com.clearchain.app.presentation.grocery.managerequests

import com.clearchain.app.R
import com.clearchain.app.data.remote.api.NgoReputationData
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.FilterChipData
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
        CommonSortOptions.NAME_DESC,
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

    // Bulk selection
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isBulkOperating: Boolean = false,

    val ngoReputations: Map<String, NgoReputationData> = emptyMap(),

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val requests: List<PickupRequest> get() = filteredRequests
    val selectedCount: Int get() = selectedIds.size
    val pendingSelectedCount: Int get() = filteredRequests.count { it.id in selectedIds && it.status.name == "PENDING" }
    val allSelected: Boolean get() = filteredRequests.isNotEmpty() && selectedIds.containsAll(filteredRequests.map { it.id })
    val activeFilterCount: Int get() =
        (if (filterCategory != null) 1 else 0) +
        (if (filterPickupDatePreset != null) 1 else 0)
}
