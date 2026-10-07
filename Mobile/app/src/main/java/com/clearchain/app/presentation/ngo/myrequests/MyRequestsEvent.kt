package com.clearchain.app.presentation.ngo.myrequests

import android.net.Uri
import com.clearchain.app.presentation.components.SortOption

sealed class MyRequestsEvent {
    object LoadRequests : MyRequestsEvent()
    object RefreshRequests : MyRequestsEvent()

    // Search, Sort, Filter
    data class SearchQueryChanged(val query: String) : MyRequestsEvent()
    data class SortOptionChanged(val option: SortOption) : MyRequestsEvent()
    data class StatusFilterChanged(val status: String?) : MyRequestsEvent()

    data class CancelRequest(val requestId: String) : MyRequestsEvent()

    data class ConfirmPickupWithPhoto(
        val requestId: String,
        val photoUri: Uri
    ) : MyRequestsEvent()

    object RetryFailedUpload : MyRequestsEvent()
    object DismissUploadError : MyRequestsEvent()

    // CSV export
    object ExportCsv : MyRequestsEvent()

    // Advanced filter sheet
    object ShowFilterSheet : MyRequestsEvent()
    object HideFilterSheet : MyRequestsEvent()
    data class FilterCategoryChanged(val category: String?) : MyRequestsEvent()
    data class FilterPickupDatePresetChanged(val preset: String?) : MyRequestsEvent()
    object ClearAdvancedFilters : MyRequestsEvent()
}
