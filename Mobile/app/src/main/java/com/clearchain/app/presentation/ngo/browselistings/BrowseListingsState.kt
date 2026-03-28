package com.clearchain.app.presentation.ngo.browselistings

import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.displayName
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
        FilterChipData(null, "All")
    ) + FoodCategory.entries.map {
        FilterChipData(it.name, it.displayName())
    },

    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,

    // ═══ NEW: Location preference fields (Part 2) ═══
    val userLat: Double? = null,
    val userLng: Double? = null,
    val radiusKm: Int = 10,
    val locationDisplayName: String = "",
    val isLocationSet: Boolean = false
) {
    val listings: List<Listing> get() = filteredListings
    val selectedCategoryEnum: FoodCategory? get() =
        selectedCategory?.let { FoodCategory.valueOf(it) }
}