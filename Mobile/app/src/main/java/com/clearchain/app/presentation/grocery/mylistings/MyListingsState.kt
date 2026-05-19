package com.clearchain.app.presentation.grocery.mylistings

import com.clearchain.app.R
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.FilterChipData
import com.clearchain.app.presentation.components.SortOption

enum class MyListingsTab { AVAILABLE, ARCHIVED, RESERVED, EXPIRED }

data class MyListingsState(
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
        CommonSortOptions.NAME_DESC,
    ),

    val activeTab: MyListingsTab = MyListingsTab.AVAILABLE,
    val selectedCategory: String? = null,
    val availableCategoryFilters: List<FilterChipData> = listOf(
        FilterChipData(null, labelResId = R.string.filter_all)
    ) + FoodCategory.entries.map {
        FilterChipData(it.name, labelResId = it.labelResId)
    },

    // Advanced filter sheet
    val showFilterSheet: Boolean = false,
    val filterExpiryWithinDays: Int? = null,
    val filterHasRequests: Boolean = false,

    // Bulk selection
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),

    val isLoading: Boolean = false,
    val isBulkOperating: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
) {
    val listings: List<Listing> get() = filteredListings
    val selectedCount: Int get() = selectedIds.size
    val allSelected: Boolean get() = filteredListings.isNotEmpty() && selectedIds.containsAll(filteredListings.map { it.id })
    val activeFilterCount: Int get() =
        (if (selectedCategory != null) 1 else 0) +
        (if (filterExpiryWithinDays != null) 1 else 0) +
        (if (filterHasRequests) 1 else 0)
}
