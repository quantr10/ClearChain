package com.clearchain.app.presentation.ngo.myrequests

import android.net.Uri
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.FilterChipData
import com.clearchain.app.presentation.components.SortOption

data class MyRequestsState(
    val allRequests: List<PickupRequest> = emptyList(),
    val filteredRequests: List<PickupRequest> = emptyList(),

    // Search, Sort, Filter
    val searchQuery: String = "",
    val selectedSort: SortOption = CommonSortOptions.DATE_DESC,
    val availableSortOptions: List<SortOption> = listOf(
        CommonSortOptions.DATE_DESC,
        CommonSortOptions.DATE_ASC,
        SortOption("pickup_date_asc", "Pickup Date (Soon)"),
        SortOption("pickup_date_desc", "Pickup Date (Later)")
    ),
    val selectedStatus: String? = null,
    val availableStatusFilters: List<FilterChipData> = listOf(
        FilterChipData(null, "All"),
        FilterChipData("PENDING", "Pending"),
        FilterChipData("APPROVED", "Approved"),
        FilterChipData("READY", "Ready"),
        FilterChipData("COMPLETED", "Completed"),
        FilterChipData("REJECTED", "Rejected"),
    ),

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    
    // ✅ NEW: Retry mechanism state
    val uploadError: String? = null,
    val failedUploadRequestId: String? = null,
    val failedUploadPhotoUri: Uri? = null,
    val uploadAttempts: Int = 0
) {
    val requests: List<PickupRequest> get() = filteredRequests
}