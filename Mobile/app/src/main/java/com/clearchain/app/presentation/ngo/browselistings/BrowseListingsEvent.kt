package com.clearchain.app.presentation.ngo.browselistings

import com.clearchain.app.presentation.components.SortOption

sealed class BrowseListingsEvent {
    object LoadListings : BrowseListingsEvent()
    object RefreshListings : BrowseListingsEvent()

    // NEW: Search, Sort, Filter
    data class SearchQueryChanged(val query: String) : BrowseListingsEvent()
    data class SortOptionChanged(val option: SortOption) : BrowseListingsEvent()
    data class CategoryFilterChanged(val category: String?) : BrowseListingsEvent()

    data class NavigateToRequestPickup(val listingId: String) : BrowseListingsEvent()
    object ClearError : BrowseListingsEvent()
}