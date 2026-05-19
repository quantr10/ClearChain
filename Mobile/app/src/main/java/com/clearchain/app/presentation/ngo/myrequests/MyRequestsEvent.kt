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

    // Rate & review
    data class ShowReviewDialog(val requestId: String) : MyRequestsEvent()
    object DismissReviewDialog : MyRequestsEvent()
    data class ReviewRatingChanged(val rating: Int) : MyRequestsEvent()
    data class ReviewCommentChanged(val comment: String) : MyRequestsEvent()
    object SubmitReview : MyRequestsEvent()

    // Dispute
    data class DisputeRequest(val requestId: String) : MyRequestsEvent()

    // PDF receipt
    data class GenerateReceipt(val requestId: String) : MyRequestsEvent()

    object ClearError : MyRequestsEvent()

    // Advanced filter sheet
    object ShowFilterSheet : MyRequestsEvent()
    object HideFilterSheet : MyRequestsEvent()
    data class FilterCategoryChanged(val category: String?) : MyRequestsEvent()
    data class FilterPickupDatePresetChanged(val preset: String?) : MyRequestsEvent()
    object ClearAdvancedFilters : MyRequestsEvent()
}