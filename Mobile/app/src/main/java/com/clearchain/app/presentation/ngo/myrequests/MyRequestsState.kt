package com.clearchain.app.presentation.ngo.myrequests

import android.net.Uri
import com.clearchain.app.R
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus
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
        FilterChipData(null, labelResId = R.string.filter_all)
    ) + PickupRequestStatus.entries.map {
        FilterChipData(it.name, labelResId = it.labelResId)
    },

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,

    // Upload retry
    val uploadError: String? = null,
    val failedUploadRequestId: String? = null,
    val failedUploadPhotoUri: Uri? = null,
    val uploadAttempts: Int = 0,

    // Rate & review
    val showReviewDialogForId: String? = null,
    val reviewRating: Int = 5,
    val reviewComment: String = "",
    val isSubmittingReview: Boolean = false,

    // PDF receipt
    val isGeneratingReceipt: Boolean = false,

    // Advanced filter sheet
    val showFilterSheet: Boolean = false,
    val filterCategory: String? = null,
    val filterPickupDatePreset: String? = null  // "TODAY"|"WEEK"|"MONTH"|"PAST"|null
) {
    val requests: List<PickupRequest> get() = filteredRequests
    val activeFilterCount: Int get() =
        (if (filterCategory != null) 1 else 0) +
        (if (filterPickupDatePreset != null) 1 else 0)
}