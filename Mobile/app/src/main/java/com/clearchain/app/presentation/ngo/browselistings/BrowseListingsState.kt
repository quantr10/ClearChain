package com.clearchain.app.presentation.ngo.browselistings

import com.clearchain.app.R
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.FilterChipData
import com.clearchain.app.presentation.components.SortOption

data class BrowseListingsState(
    val allListings: List<Listing> = emptyList(),
    val filteredListings: List<Listing> = emptyList(),

    // Search, Sort, Filter
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
    val availableCategoryFilters: List<FilterChipData> = listOf(
        FilterChipData(null, labelResId = R.string.filter_all)
    ) + FoodCategory.entries.map {
        FilterChipData(it.name, labelResId = it.labelResId)
    },

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
    val filterMinQuantity: Int = 0,
    val filterMaxQuantity: Int? = null,     // null = no upper limit
    val filterMinExpiryDays: Int = 0,       // 0 = include expired/today; >0 = must have N+ days remaining
    val filterMaxExpiryDays: Int? = null,   // null = no limit
    val filterMaxDistanceKm: Int? = null,   // null = use radiusKm from location pref

    // Map / list toggle
    val showMapView: Boolean = false,

    // Map view — all listings without radius filter
    val allMapListings: List<Listing> = emptyList(),
    val mapFilteredListings: List<Listing> = emptyList(),
    val isLoadingMapListings: Boolean = false,
    val selectedGroceryKey: String? = null
) {
    val listings: List<Listing> get() = filteredListings
    val selectedCategoryEnum: FoodCategory? get() =
        selectedCategory?.let { FoodCategory.valueOf(it) }
    val activeFilterCount: Int get() =
        (if (selectedCategory != null) 1 else 0) +
        (if (filterMinQuantity > 0) 1 else 0) +
        (if (filterMaxQuantity != null) 1 else 0) +
        (if (filterMinExpiryDays > 0) 1 else 0) +
        (if (filterMaxExpiryDays != null) 1 else 0) +
        (if (filterMaxDistanceKm != null) 1 else 0) +
        (if (showFavoritesOnly) 1 else 0)
}