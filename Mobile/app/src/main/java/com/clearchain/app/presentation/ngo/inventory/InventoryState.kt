package com.clearchain.app.presentation.ngo.inventory

import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.domain.model.displayName
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.FilterChipData
import com.clearchain.app.presentation.components.SortOption

data class InventoryState(
    val allItems: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),
    
    // Search, Sort, Filter
    val searchQuery: String = "",
    val selectedSort: SortOption = CommonSortOptions.DATE_DESC,
    val availableSortOptions: List<SortOption> = listOf(
        CommonSortOptions.DATE_DESC,
        CommonSortOptions.EXPIRY_ASC,
        SortOption("distributed_date", "Recently Distributed"),
        CommonSortOptions.NAME_ASC
    ),
    
    // NEW: Status as TABS (not chips)
    val selectedStatusTab: InventoryStatus? = null, // null = All
    
    // NEW: Category as CHIPS (food categories)
    val selectedCategory: String? = null,
    val availableCategoryFilters: List<FilterChipData> = listOf(
        FilterChipData(null, "All")
    ) + FoodCategory.entries.map { 
        FilterChipData(it.name, it.displayName()) 
    },

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    // Helper to get counts for each tab
    fun getStatusCounts(): Map<InventoryStatus?, Int> {
        return mapOf(
            null to allItems.size, // All
            InventoryStatus.ACTIVE to allItems.count { it.status == InventoryStatus.ACTIVE },
            InventoryStatus.DISTRIBUTED to allItems.count { it.status == InventoryStatus.DISTRIBUTED },
            InventoryStatus.EXPIRED to allItems.count { it.status == InventoryStatus.EXPIRED }
        )
    }
}