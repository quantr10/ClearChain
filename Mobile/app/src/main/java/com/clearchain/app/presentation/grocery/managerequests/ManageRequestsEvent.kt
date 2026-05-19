package com.clearchain.app.presentation.grocery.managerequests

import com.clearchain.app.presentation.components.SortOption

sealed class ManageRequestsEvent {
    object LoadRequests : ManageRequestsEvent()
    object RefreshRequests : ManageRequestsEvent()

    data class SearchQueryChanged(val query: String) : ManageRequestsEvent()
    data class SortOptionChanged(val option: SortOption) : ManageRequestsEvent()
    data class StatusFilterChanged(val status: String?) : ManageRequestsEvent()

    // Advanced filter sheet
    object ShowFilterSheet : ManageRequestsEvent()
    object HideFilterSheet : ManageRequestsEvent()
    data class FilterCategoryChanged(val category: String?) : ManageRequestsEvent()
    data class FilterPickupDatePresetChanged(val preset: String?) : ManageRequestsEvent()
    object ClearAdvancedFilters : ManageRequestsEvent()

    data class ApproveRequest(val requestId: String) : ManageRequestsEvent()
    data class RejectRequest(val requestId: String) : ManageRequestsEvent()
    data class MarkReady(val requestId: String) : ManageRequestsEvent()

    // Bulk selection
    object ToggleSelectionMode : ManageRequestsEvent()
    data class ToggleItemSelection(val requestId: String) : ManageRequestsEvent()
    object SelectAll : ManageRequestsEvent()
    object DeselectAll : ManageRequestsEvent()
    object BulkApprove : ManageRequestsEvent()
    data class BulkReject(val reason: String? = null) : ManageRequestsEvent()

    object ClearError : ManageRequestsEvent()
}
