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
    
    // ✅ NEW: Retry failed upload
    object RetryFailedUpload : MyRequestsEvent()
    object DismissUploadError : MyRequestsEvent()
    
    object ClearError : MyRequestsEvent()
}