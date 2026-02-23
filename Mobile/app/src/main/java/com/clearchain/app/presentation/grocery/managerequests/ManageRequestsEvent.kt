package com.clearchain.app.presentation.grocery.managerequests

import com.clearchain.app.presentation.components.SortOption

sealed class ManageRequestsEvent {
    object LoadRequests : ManageRequestsEvent()
    object RefreshRequests : ManageRequestsEvent()

    // NEW: Search, Sort, Filter
    data class SearchQueryChanged(val query: String) : ManageRequestsEvent()
    data class SortOptionChanged(val option: SortOption) : ManageRequestsEvent()
    data class StatusFilterChanged(val status: String?) : ManageRequestsEvent()

    data class ApproveRequest(val requestId: String) : ManageRequestsEvent()
    data class RejectRequest(val requestId: String) : ManageRequestsEvent()
    data class MarkReady(val requestId: String) : ManageRequestsEvent()
    object ClearError : ManageRequestsEvent()
}