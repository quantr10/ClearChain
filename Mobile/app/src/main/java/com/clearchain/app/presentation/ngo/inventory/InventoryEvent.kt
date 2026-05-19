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

    // Advanced filter sheet
    object ShowFilterSheet : InventoryEvent()
    object HideFilterSheet : InventoryEvent()
    data class FilterExpiryWithinDaysChanged(val days: Int?) : InventoryEvent()
    data class FilterMinQtyChanged(val min: Double) : InventoryEvent()
    data class FilterMaxQtyChanged(val max: Double?) : InventoryEvent()
    object ClearAdvancedFilters : InventoryEvent()

    data class DistributeItem(val itemId: String) : InventoryEvent()
    object ClearError : InventoryEvent()

    // Bulk selection
    object ToggleSelectionMode : InventoryEvent()
    data class ToggleItemSelection(val itemId: String) : InventoryEvent()
    object SelectAll : InventoryEvent()
    object DeselectAll : InventoryEvent()
    object BulkDistribute : InventoryEvent()

    // Manual add
    object ShowManualAddSheet : InventoryEvent()
    object HideManualAddSheet : InventoryEvent()
    data class ManualProductNameChanged(val name: String) : InventoryEvent()
    data class ManualCategoryChanged(val category: String) : InventoryEvent()
    data class ManualQuantityChanged(val qty: String) : InventoryEvent()
    data class ManualUnitChanged(val unit: String) : InventoryEvent()
    data class ManualExpiryDateChanged(val date: String) : InventoryEvent()
    object SubmitManualAdd : InventoryEvent()

    // Beneficiary count
    data class ShowBeneficiaryDialog(val itemId: String) : InventoryEvent()
    object DismissBeneficiaryDialog : InventoryEvent()
    data class BeneficiaryCountChanged(val count: String) : InventoryEvent()
    object ConfirmDistribute : InventoryEvent()

    object ExportCsv : InventoryEvent()
}