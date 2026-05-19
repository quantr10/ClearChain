package com.clearchain.app.presentation.ngo.browselistings

import com.clearchain.app.presentation.components.SortOption

sealed class BrowseListingsEvent {
    object LoadListings : BrowseListingsEvent()
    object RefreshListings : BrowseListingsEvent()

    data class SearchQueryChanged(val query: String) : BrowseListingsEvent()
    data class SortOptionChanged(val option: SortOption) : BrowseListingsEvent()
    data class CategoryFilterChanged(val category: String?) : BrowseListingsEvent()

    data class NavigateToRequestPickup(val listingId: String) : BrowseListingsEvent()
    object ClearError : BrowseListingsEvent()

    // Favorites
    data class ToggleFavorite(val listingId: String) : BrowseListingsEvent()
    object ToggleFavoritesOnly : BrowseListingsEvent()

    // Advanced filters
    object ShowFilterSheet : BrowseListingsEvent()
    object HideFilterSheet : BrowseListingsEvent()
    data class FilterMinQuantityChanged(val min: Int) : BrowseListingsEvent()
    data class FilterMaxQuantityChanged(val max: Int?) : BrowseListingsEvent()
    data class FilterMinExpiryDaysChanged(val days: Int) : BrowseListingsEvent()
    data class FilterMaxExpiryDaysChanged(val days: Int?) : BrowseListingsEvent()
    data class FilterMaxDistanceChanged(val km: Int?) : BrowseListingsEvent()
    object ClearAdvancedFilters : BrowseListingsEvent()

    // Map/list toggle
    object ToggleMapView : BrowseListingsEvent()

    // Map pin interaction
    data class GroceryPinTapped(val key: String) : BrowseListingsEvent()
    object DismissGrocerySheet : BrowseListingsEvent()
}