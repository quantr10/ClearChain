package com.clearchain.app.presentation.ngo.listingdetail

sealed class ListingDetailEvent {
    data class LoadListing(val listingId: String) : ListingDetailEvent()

    // Report
    object ShowReportDialog : ListingDetailEvent()
    object DismissReportDialog : ListingDetailEvent()
    data class ReportReasonChanged(val reason: String) : ListingDetailEvent()
    object SubmitReport : ListingDetailEvent()

    // NGO: save/favourite
    object ToggleSave : ListingDetailEvent()
    data class AddToCart(val listingId: String) : ListingDetailEvent()
    data class IncrementCartItem(val listingId: String) : ListingDetailEvent()
    data class DecrementCartItem(val listingId: String) : ListingDetailEvent()

    // Grocery: actions
    object ShowDeleteConfirm : ListingDetailEvent()
    object DismissDeleteConfirm : ListingDetailEvent()
    object DeleteListing : ListingDetailEvent()
    data class UpdateQuantity(val newQuantity: Int) : ListingDetailEvent()
}
