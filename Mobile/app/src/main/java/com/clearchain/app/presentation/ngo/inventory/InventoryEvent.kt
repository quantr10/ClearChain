// ═══════════════════════════════════════════════════════════════════════════════
// InventoryEvent.kt - UPDATED WITH CATEGORY FILTER & STATUS TAB
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.ngo.inventory

import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.presentation.components.SortOption

sealed class InventoryEvent {
    object LoadInventory : InventoryEvent()
    object RefreshInventory : InventoryEvent()
    object UpdateExpired : InventoryEvent()
    
    // Search & Sort
    data class SearchQueryChanged(val query: String) : InventoryEvent()
    data class SortOptionChanged(val option: SortOption) : InventoryEvent()
    
    // NEW: Status as TAB (changed from chip)
    data class StatusTabChanged(val status: InventoryStatus?) : InventoryEvent()
    
    // NEW: Category as CHIP (food categories)
    data class CategoryFilterChanged(val category: String?) : InventoryEvent()
    
    data class DistributeItem(val itemId: String) : InventoryEvent()
    object ClearError : InventoryEvent()
}