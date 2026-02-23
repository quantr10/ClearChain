package com.clearchain.app.presentation.ngo.myrequests

import com.clearchain.app.presentation.components.SortOption

sealed class MyRequestsEvent {
    object LoadRequests : MyRequestsEvent()
    object RefreshRequests : MyRequestsEvent()

    // NEW: Search, Sort, Filter
    data class SearchQueryChanged(val query: String) : MyRequestsEvent()
    data class SortOptionChanged(val option: SortOption) : MyRequestsEvent()
    data class StatusFilterChanged(val status: String?) : MyRequestsEvent()

    data class CancelRequest(val requestId: String) : MyRequestsEvent()
    data class ConfirmPickup(val requestId: String) : MyRequestsEvent()
    object ClearError : MyRequestsEvent()
}
