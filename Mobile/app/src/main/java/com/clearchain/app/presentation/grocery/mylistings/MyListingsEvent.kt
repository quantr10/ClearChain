// ═══════════════════════════════════════════════════════════════════════════════
// MyListingsEvent.kt - UPDATED WITH CATEGORY FILTER & STATUS TAB
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.grocery.mylistings

import com.clearchain.app.domain.model.ListingStatus
import com.clearchain.app.presentation.components.SortOption

sealed class MyListingsEvent {
    object LoadListings : MyListingsEvent()
    object RefreshListings : MyListingsEvent()
    data class DeleteListing(val listingId: String) : MyListingsEvent()

    // Search & Sort
    data class SearchQueryChanged(val query: String) : MyListingsEvent()
    data class SortOptionChanged(val option: SortOption) : MyListingsEvent()

    // NEW: Status as TAB (changed from chip)
    data class StatusTabChanged(val status: ListingStatus?) : MyListingsEvent()

    // NEW: Category as CHIP (food categories)
    data class CategoryFilterChanged(val category: String?) : MyListingsEvent()

    data class UpdateListingQuantity(
        val listingId: String, 
        val newQuantity: Int
    ) : MyListingsEvent()
    
    object ClearError : MyListingsEvent()
}