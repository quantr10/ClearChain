package com.clearchain.app.presentation.ngo.myrequests

sealed class MyRequestsEvent {
    object LoadRequests : MyRequestsEvent()
    object RefreshRequests : MyRequestsEvent()
    data class StatusFilterChanged(val status: String?) : MyRequestsEvent()
    data class CancelRequest(val requestId: String) : MyRequestsEvent()
    object ClearError : MyRequestsEvent()
}