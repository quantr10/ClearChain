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
    val selectedSort: SortOption = CommonSortOptions.CREATED_DATE_DESC,
    val availableSortOptions: List<SortOption> = listOf(
        CommonSortOptions.PICKUP_DATE_DESC,
        CommonSortOptions.PICKUP_DATE_ASC,
        CommonSortOptions.NAME_ASC,
        CommonSortOptions.NAME_DESC,
        CommonSortOptions.EXPIRY_ASC,
        CommonSortOptions.EXPIRY_DESC,

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