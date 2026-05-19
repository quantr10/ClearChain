package com.clearchain.app.presentation.grocery.mylistings

import com.clearchain.app.presentation.components.SortOption

sealed class MyListingsEvent {
    object LoadListings : MyListingsEvent()
    object RefreshListings : MyListingsEvent()
    data class DeleteListing(val listingId: String) : MyListingsEvent()

    // Search & Sort
    data class SearchQueryChanged(val query: String) : MyListingsEvent()
    data class SortOptionChanged(val option: SortOption) : MyListingsEvent()

    // Filters
    data class TabChanged(val tab: MyListingsTab) : MyListingsEvent()
    data class CategoryFilterChanged(val category: String?) : MyListingsEvent()

    // Advanced filter sheet
    object ShowFilterSheet : MyListingsEvent()
    object HideFilterSheet : MyListingsEvent()
    data class FilterExpiryWithinDaysChanged(val days: Int?) : MyListingsEvent()
    data class FilterHasRequestsChanged(val enabled: Boolean) : MyListingsEvent()
    object ClearAdvancedFilters : MyListingsEvent()

    // Archive
    data class ArchiveListing(val listingId: String) : MyListingsEvent()
    data class UnarchiveListing(val listingId: String) : MyListingsEvent()

    // Bulk selection
    object ToggleSelectionMode : MyListingsEvent()
    data class ToggleItemSelection(val listingId: String) : MyListingsEvent()
    object SelectAll : MyListingsEvent()
    object DeselectAll : MyListingsEvent()
    object BulkDelete : MyListingsEvent()
    object BulkArchive : MyListingsEvent()
    object BulkRestore : MyListingsEvent()

    data class UpdateListingQuantity(val listingId: String, val newQuantity: Int) : MyListingsEvent()
    object ClearError : MyListingsEvent()
}
