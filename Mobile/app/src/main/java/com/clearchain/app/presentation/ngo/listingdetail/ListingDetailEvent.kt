package com.clearchain.app.presentation.ngo.listingdetail

sealed class ListingDetailEvent {
    data class LoadListing(val listingId: String) : ListingDetailEvent()

    // NGO: save/favourite
    object ToggleSave : ListingDetailEvent()
    data class AddToCart(val listingId: String) : ListingDetailEvent()
    data class IncrementCartItem(val listingId: String) : ListingDetailEvent()
    data class DecrementCartItem(val listingId: String) : ListingDetailEvent()
    data class RemoveCartItem(val listingId: String) : ListingDetailEvent()

    // Grocery: actions
    object ShowDeleteConfirm : ListingDetailEvent()
    object DismissDeleteConfirm : ListingDetailEvent()
    object DeleteListing : ListingDetailEvent()
    object ShowArchiveConfirm : ListingDetailEvent()
    object DismissArchiveConfirm : ListingDetailEvent()
    object ArchiveListing : ListingDetailEvent()
    object ShowRestoreConfirm : ListingDetailEvent()
    object DismissRestoreConfirm : ListingDetailEvent()
    object RestoreListing : ListingDetailEvent()
    data class UpdateQuantity(val newQuantity: Int) : ListingDetailEvent()
}
