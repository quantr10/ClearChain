package com.clearchain.app.presentation.ngo.browselistings

import com.clearchain.app.domain.model.FoodCategory

sealed class BrowseListingsEvent {
    object LoadListings : BrowseListingsEvent()
    object RefreshListings : BrowseListingsEvent()
    data class SearchQueryChanged(val query: String) : BrowseListingsEvent()
    data class CategoryFilterChanged(val category: FoodCategory?) : BrowseListingsEvent()
    data class NavigateToRequestPickup(val listingId: String) : BrowseListingsEvent()
    object ClearError : BrowseListingsEvent()
}