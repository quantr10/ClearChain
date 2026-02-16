package com.clearchain.app.presentation.ngo.inventory

sealed class InventoryEvent {
    object LoadInventory : InventoryEvent()
    object RefreshInventory : InventoryEvent()
    object UpdateExpired : InventoryEvent()
    data class StatusFilterChanged(val status: String?) : InventoryEvent()
    data class DistributeItem(val itemId: String) : InventoryEvent()
    object ClearError : InventoryEvent()
}