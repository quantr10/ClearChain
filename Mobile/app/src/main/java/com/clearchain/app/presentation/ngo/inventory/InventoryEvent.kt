// ── InventoryEvent.kt - UPDATED WITH CATEGORY FILTER & STATUS TAB ────────────

package com.clearchain.app.presentation.ngo.inventory

import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.presentation.components.SortOption

sealed class InventoryEvent {
    object LoadInventory : InventoryEvent()
    object RefreshInventory : InventoryEvent()

    // Search & Sort
    data class SearchQueryChanged(val query: String) : InventoryEvent()
    data class SortOptionChanged(val option: SortOption) : InventoryEvent()

    // Status as TAB (changed from chip)
    data class StatusTabChanged(val status: InventoryStatus?) : InventoryEvent()

    // Category as CHIP (food categories)
    data class CategoryFilterChanged(val category: String?) : InventoryEvent()

    // Advanced filter sheet
    object ShowFilterSheet : InventoryEvent()
    object HideFilterSheet : InventoryEvent()
    data class FilterExpiryWithinDaysChanged(val days: Int?) : InventoryEvent()
    data class FilterMinQtyChanged(val min: Double) : InventoryEvent()
    data class FilterMaxQtyChanged(val max: Double?) : InventoryEvent()
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
