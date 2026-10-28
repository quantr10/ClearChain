package com.clearchain.app.presentation.ngo.inventory

import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.presentation.components.SortOption

sealed class InventoryEvent {
    object LoadInventory : InventoryEvent()
    object RefreshInventory : InventoryEvent()

    // Search and sort
    data class SearchQueryChanged(val query: String) : InventoryEvent()
    data class SortOptionChanged(val option: SortOption) : InventoryEvent()

    // Status tabs
    data class StatusTabChanged(val status: InventoryStatus?) : InventoryEvent()

    // Category chips
    data class CategoryFilterChanged(val category: String?) : InventoryEvent()

    // Advanced filter sheet
    object ShowFilterSheet : InventoryEvent()
    object HideFilterSheet : InventoryEvent()
    data class FilterExpiresByChanged(val date: String?) : InventoryEvent()
    object ClearAdvancedFilters : InventoryEvent()

    data class DistributeItem(val itemId: String) : InventoryEvent()

    // Bulk selection
    object ToggleSelectionMode : InventoryEvent()
    data class ToggleItemSelection(val itemId: String) : InventoryEvent()
    object SelectAll : InventoryEvent()
    object DeselectAll : InventoryEvent()
    object BulkDistribute : InventoryEvent()

    object ExportCsv : InventoryEvent()
}
