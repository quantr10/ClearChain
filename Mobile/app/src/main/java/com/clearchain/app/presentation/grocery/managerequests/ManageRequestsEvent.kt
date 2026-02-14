package com.clearchain.app.presentation.grocery.managerequests

sealed class ManageRequestsEvent {
    object LoadRequests : ManageRequestsEvent()
    object RefreshRequests : ManageRequestsEvent()
    data class StatusFilterChanged(val status: String?) : ManageRequestsEvent()
    data class ApproveRequest(val requestId: String) : ManageRequestsEvent()
    data class RejectRequest(val requestId: String) : ManageRequestsEvent()
    data class MarkReady(val requestId: String) : ManageRequestsEvent()
    data class MarkPickedUp(val requestId: String) : ManageRequestsEvent()
    object ClearError : ManageRequestsEvent()
}