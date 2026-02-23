// ═══════════════════════════════════════════════════════════════════════════════
// MyListingsState.kt - UPDATED WITH FOOD CATEGORY FILTERS & STATUS TABS
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.grocery.mylistings

import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.ListingStatus
import com.clearchain.app.domain.model.displayName
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.FilterChipData
import com.clearchain.app.presentation.components.SortOption

data class MyListingsState(
    val allListings: List<Listing> = emptyList(),
    val filteredListings: List<Listing> = emptyList(),

    // Search, Sort, Filter
    val searchQuery: String = "",
    val selectedSort: SortOption = CommonSortOptions.DATE_DESC,
    val availableSortOptions: List<SortOption> = listOf(
        CommonSortOptions.DATE_DESC,
        CommonSortOptions.DATE_ASC,
        CommonSortOptions.NAME_ASC,
        CommonSortOptions.NAME_DESC,
        CommonSortOptions.EXPIRY_ASC,
        CommonSortOptions.QUANTITY_DESC
    ),

    // NEW: Status as TABS (not chips)
    val selectedStatusTab: ListingStatus? = null, // null = All

    // NEW: Category as CHIPS (food categories)
    val selectedCategory: String? = null,
    val availableCategoryFilters: List<FilterChipData> = listOf(
        FilterChipData(null, "All Categories")
    ) + FoodCategory.entries.map {
        FilterChipData(it.name, it.displayName())
    },

    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
) {
    // Backward compatibility
    val listings: List<Listing> get() = filteredListings

    // Helper to get counts for each tab
    fun getStatusCounts(): Map<ListingStatus?, Int> {
        return mapOf(
            null to allListings.size, // All
            ListingStatus.AVAILABLE to allListings.count { it.status == ListingStatus.AVAILABLE },
            ListingStatus.RESERVED to allListings.count { it.status == ListingStatus.RESERVED },
            ListingStatus.COMPLETED to allListings.count { it.status == ListingStatus.COMPLETED },
            ListingStatus.EXPIRED to allListings.count { it.status == ListingStatus.EXPIRED }
        )
    }
}