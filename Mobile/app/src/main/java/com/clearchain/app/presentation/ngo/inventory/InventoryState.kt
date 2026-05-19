package com.clearchain.app.presentation.ngo.inventory

import com.clearchain.app.R
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.FilterChipData
import com.clearchain.app.presentation.components.SortOption

data class InventoryState(
    val allItems: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),
    
    // Search, Sort, Filter
    val searchQuery: String = "",
    val selectedSort: SortOption = CommonSortOptions.DISTRIBUTED_DATE_DESC,
    val availableSortOptions: List<SortOption> = listOf(
        CommonSortOptions.DISTRIBUTED_DATE_ASC,
        CommonSortOptions.DISTRIBUTED_DATE_DESC,
        CommonSortOptions.NAME_ASC,
        CommonSortOptions.NAME_DESC,
        CommonSortOptions.EXPIRY_ASC,
        CommonSortOptions.EXPIRY_DESC,
    ),
    
    val selectedStatusTab: InventoryStatus? = InventoryStatus.ACTIVE,
    
    // NEW: Category as CHIPS (food categories)
    val selectedCategory: String? = null,
    val availableCategoryFilters: List<FilterChipData> = listOf(
        FilterChipData(null, labelResId = R.string.filter_all)
    ) + FoodCategory.entries.map {
        FilterChipData(it.name, labelResId = it.labelResId)
    },

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // Advanced filter sheet
    val showFilterSheet: Boolean = false,
    val filterExpiryWithinDays: Int? = null,
    val filterMinQty: Double = 0.0,
    val filterMaxQty: Double? = null,

    // Bulk selection
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isBulkOperating: Boolean = false,

    // Manual add sheet
    val showManualAddSheet: Boolean = false,
    val manualProductName: String = "",
    val manualCategory: String = "",
    val manualQuantity: String = "",
    val manualUnit: String = "kg",
    val manualExpiryDate: String = "",
    val isSubmittingManual: Boolean = false,

    // Beneficiary count dialog
    val showBeneficiaryDialogForId: String? = null,
    val beneficiaryCount: String = ""
) {
    val selectedCount: Int get() = selectedIds.size
    val activeSelectedCount: Int get() = filteredItems.count { it.id in selectedIds && it.status == InventoryStatus.ACTIVE }
    val allSelected: Boolean get() = filteredItems.isNotEmpty() && selectedIds.containsAll(filteredItems.map { it.id })
    val activeFilterCount: Int get() =
        (if (selectedCategory != null) 1 else 0) +
        (if (filterExpiryWithinDays != null) 1 else 0) +
        (if (filterMinQty > 0.0) 1 else 0) +
        (if (filterMaxQty != null) 1 else 0)

    val expiringItems: List<InventoryItem> get() {
        val today = java.time.LocalDate.now()
        val threshold = today.plusDays(3)
        return allItems.filter { item ->
            item.status == InventoryStatus.ACTIVE &&
            runCatching {
                val exp = java.time.LocalDate.parse(item.expiryDate.take(10))
                !exp.isBefore(today) && !exp.isAfter(threshold)
            }.getOrDefault(false)
        }
    }

    // Items expiring within 48 hours (critical level)
    val criticalExpiryItems: List<InventoryItem> get() {
        val today = java.time.LocalDate.now()
        val threshold = today.plusDays(2)
        return allItems.filter { item ->
            item.status == InventoryStatus.ACTIVE &&
            runCatching {
                val exp = java.time.LocalDate.parse(item.expiryDate.take(10))
                !exp.isBefore(today) && !exp.isAfter(threshold)
            }.getOrDefault(false)
        }
    }

    val categoryBreakdown: List<Pair<String, Int>> get() =
        allItems.filter { it.status == InventoryStatus.ACTIVE }
            .groupBy { it.category }
            .map { (cat, items) -> cat to items.size }
            .sortedByDescending { it.second }
            .take(5)

    fun getStatusCounts(): Map<InventoryStatus?, Int> = mapOf(
        null to allItems.size,
        InventoryStatus.ACTIVE to allItems.count { it.status == InventoryStatus.ACTIVE },
        InventoryStatus.DISTRIBUTED to allItems.count { it.status == InventoryStatus.DISTRIBUTED },
        InventoryStatus.EXPIRED to allItems.count { it.status == InventoryStatus.EXPIRED }
    )
}