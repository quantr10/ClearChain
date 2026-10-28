package com.clearchain.app.presentation.ngo.browselistings

import com.clearchain.app.data.remote.dto.CartItemData
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.SortOption

data class BrowseListingsState(
    val allListings: List<Listing> = emptyList(),
    val filteredListings: List<Listing> = emptyList(),

    // Search, sort and filter
    val searchQuery: String = "",
    val selectedSort: SortOption = CommonSortOptions.CREATED_DATE_DESC,
    val availableSortOptions: List<SortOption> = listOf(
        CommonSortOptions.CREATED_DATE_DESC,
        CommonSortOptions.CREATED_DATE_ASC,
        CommonSortOptions.EXPIRY_ASC,
        CommonSortOptions.EXPIRY_DESC,
        CommonSortOptions.NAME_ASC,
        CommonSortOptions.NAME_DESC
    ),
    val selectedCategory: String? = null,

    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,

    // Location preference fields
    val userLat: Double? = null,
    val userLng: Double? = null,
    val radiusKm: Int = 10,
    val locationDisplayName: String = "",
    val isLocationSet: Boolean = false,
    val isCheckingLocation: Boolean = true,

    // Favorites (in-memory; persisted via DataStore in ViewModel)
    val favoritedIds: Set<String> = emptySet(),
    val showFavoritesOnly: Boolean = false,

    // Advanced filter bottom sheet
    val showFilterSheet: Boolean = false,
    val filterExpiresBy: String? = null, // yyyy-MM-dd; null = any date

    val showMapView: Boolean = false,

    // Map view — all listings without radius filter
    val allMapListings: List<Listing> = emptyList(),
    val mapFilteredListings: List<Listing> = emptyList(),
    val isLoadingMapListings: Boolean = false,
    val selectedGroceryKey: String? = null,

    // Backend cart
    val cartItemsByListingId: Map<String, CartItemData> = emptyMap(),
    /** The listing whose cart call is in flight; every cart control waits for it. */
    val updatingCartListingId: String? = null
) {
    val isUpdatingCart: Boolean get() = updatingCartListingId != null
    val listings: List<Listing> get() = filteredListings
    val activeFilterCount: Int get() =
        (if (selectedCategory != null) 1 else 0) +
            (if (filterExpiresBy != null) 1 else 0) +
            (if (showFavoritesOnly) 1 else 0)
    val cartItemCount: Int get() = cartItemsByListingId.values.sumOf { it.requestedQuantity }
}
