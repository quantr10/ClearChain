package com.clearchain.app.presentation.ngo.inventory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.usecase.inventory.DistributeItemUseCase
import com.clearchain.app.domain.usecase.inventory.GetMyInventoryUseCase
import com.clearchain.app.domain.usecase.inventory.UpdateExpiredItemsUseCase
import com.clearchain.app.util.DownloadsExport
import com.clearchain.app.util.BulkResult
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class InventoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getMyInventoryUseCase: GetMyInventoryUseCase,
    private val updateExpiredItemsUseCase: UpdateExpiredItemsUseCase,
    private val distributeInventoryItemUseCase: DistributeItemUseCase,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadInventory()
        setupSignalR()
    }

    private fun setupSignalR() {
        viewModelScope.launch {
            signalRService.inventoryItemAdded.collect { item ->
                loadInventory()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_new_inventory_item, item.productName)))
            }
        }
        viewModelScope.launch { signalRService.inventoryItemDistributed.collect { loadInventory() } }
        viewModelScope.launch { signalRService.inventoryItemExpired.collect { loadInventory() } }
    }

    fun onEvent(event: InventoryEvent) {
        // While an action is in flight, the list and the selection it started from stay put.
        if (_state.value.isBulkOperating && event.isLockedWhileBusy()) return
        when (event) {
            InventoryEvent.LoadInventory -> loadInventory()
            InventoryEvent.RefreshInventory -> refreshInventory()

            // Search & Sort
            is InventoryEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is InventoryEvent.SortOptionChanged -> {
                _state.update { it.copy(selectedSort = event.option) }
                applyFilters()
            }

            // Status Tab
            is InventoryEvent.StatusTabChanged -> {
                _state.update { it.copy(selectedStatusTab = event.status) }
                applyFilters()
            }

            // Category Filter
            is InventoryEvent.CategoryFilterChanged -> {
                _state.update { it.copy(selectedCategory = event.category) }
                applyFilters()
            }

            is InventoryEvent.DistributeItem -> distributeItem(event.itemId)

            // Advanced filter sheet
            InventoryEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = true) }
            InventoryEvent.HideFilterSheet -> _state.update { it.copy(showFilterSheet = false) }
            is InventoryEvent.FilterExpiryWithinDaysChanged -> {
                _state.update { it.copy(filterExpiryWithinDays = event.days) }
                applyFilters()
            }
            is InventoryEvent.FilterMinQtyChanged -> {
                _state.update { it.copy(filterMinQty = event.min) }
                applyFilters()
            }
            is InventoryEvent.FilterMaxQtyChanged -> {
                _state.update { it.copy(filterMaxQty = event.max) }
                applyFilters()
            }
            InventoryEvent.ClearAdvancedFilters -> {
                _state.update { it.copy(selectedCategory = null, filterExpiryWithinDays = null, filterMinQty = 0.0, filterMaxQty = null) }
                applyFilters()
            }

            // Bulk selection
            InventoryEvent.ToggleSelectionMode ->
                _state.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIds = emptySet()) }
            is InventoryEvent.ToggleItemSelection ->
                _state.update {
                    val updated = if (event.itemId in it.selectedIds) {
                        it.selectedIds - event.itemId
                    } else {
                        it.selectedIds + event.itemId
                    }
                    it.copy(selectedIds = updated, isSelectionMode = updated.isNotEmpty())
                }
            InventoryEvent.SelectAll ->
                _state.update {
                    val selected = it.filteredItems.map { i -> i.id }.toSet()
                    it.copy(selectedIds = selected, isSelectionMode = selected.isNotEmpty())
                }
            InventoryEvent.DeselectAll ->
                _state.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
            InventoryEvent.BulkDistribute -> bulkDistribute()

            InventoryEvent.ExportCsv -> exportCsv()
        }
    }

    private fun exportCsv() {
        val current = _state.value
        val items = current.filteredItems
        if (items.isEmpty()) {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_no_inventory_export))) }
            return
        }
        val tag = current.selectedStatusTab?.name?.lowercase() ?: "all"
        viewModelScope.launch {
            val message = DownloadsExport.exportCsv(
                context,
                filePrefix = "clearchain_inventory_$tag",
                chooserTitle = "Share Inventory CSV",
                header = listOf("Product Name", "Category", "Quantity", "Unit", "Status", "Received At", "Expiry Date", "Distributed At"),
                rows = items.map { item ->
                    listOf(
                        item.productName,
                        item.category,
                        item.quantity.toString(),
                        item.unit,
                        item.status.name,
                        item.receivedAt.take(10),
                        item.expiryDate.take(10),
                        item.distributedAt?.take(10) ?: ""
                    )
                }
            )
            _uiEvent.send(UiEvent.ShowSnackbar(message))
        }
    }

    private fun loadInventory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // expired items first
            updateExpiredItemsUseCase()

            val result = getMyInventoryUseCase()

            result.fold(
                onSuccess = { items ->
                    _state.update {
                        it.copy(
                            allItems = items,
                            isLoading = false
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    val msg = error.message ?: context.getString(R.string.error_load_inventory)
                    _state.update { it.copy(isLoading = false, error = msg) }
                    // Data already on screen stays; the failure is reported, not hidden.
                    if (_state.value.allItems.isNotEmpty()) _uiEvent.send(UiEvent.ShowSnackbar(msg))
                }
            )
        }
    }

    private fun refreshInventory() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            // expired items first
            updateExpiredItemsUseCase()

            val result = getMyInventoryUseCase()

            result.fold(
                onSuccess = { items ->
                    _state.update {
                        it.copy(
                            allItems = items,
                            isRefreshing = false
                        )
                    }
                    applyFilters()
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_inventory_refreshed)))
                },
                onFailure = { error ->
                    _state.update { it.copy(isRefreshing = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: context.getString(R.string.error_refresh_inventory)))
                }
            )
        }
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allItems

        // Apply STATUS TAB filter (first priority)
        current.selectedStatusTab?.let { status ->
            filtered = filtered.filter { it.status == status }
        }

        // Apply search
        if (current.searchQuery.isNotBlank()) {
            val query = current.searchQuery.lowercase()
            filtered = filtered.filter { item ->
                item.productName.lowercase().contains(query) ||
                    item.category.lowercase().contains(query)
            }
        }

        // Apply CATEGORY CHIP filter
        current.selectedCategory?.let { category ->
            filtered = filtered.filter { item ->
                item.category.equals(category, ignoreCase = true) ||
                    item.category.lowercase().replace(" ", "_") == category.lowercase()
            }
        }

        // Advanced: expiry within N days
        current.filterExpiryWithinDays?.let { days ->
            val today = java.time.LocalDate.now()
            val threshold = today.plusDays(days.toLong())
            filtered = filtered.filter { item ->
                runCatching {
                    val exp = java.time.LocalDate.parse(item.expiryDate.take(10))
                    !exp.isBefore(today) && !exp.isAfter(threshold)
                }.getOrDefault(false)
            }
        }

        // Advanced: quantity range
        if (current.filterMinQty > 0.0) {
            filtered = filtered.filter { it.quantity >= current.filterMinQty }
        }
        current.filterMaxQty?.let { max ->
            filtered = filtered.filter { it.quantity <= max }
        }

        // Apply sort
        filtered = when (current.selectedSort.value) {
            "date_desc" -> filtered.sortedByDescending { it.receivedAt }
            "date_asc" -> filtered.sortedBy { it.receivedAt }
            "expiry_asc" -> filtered.sortedBy { it.expiryDate }
            "expiry_desc" -> filtered.sortedByDescending { it.expiryDate }
            "distributed_date" -> filtered.sortedByDescending { it.distributedAt ?: "" }
            "name_asc" -> filtered.sortedBy { it.productName }
            "name_desc" -> filtered.sortedByDescending { it.productName }
            else -> filtered
        }

        _state.update { it.copy(filteredItems = filtered) }
    }

    private fun distributeItem(itemId: String) {
        if (_state.value.distributingItemId != null) return
        viewModelScope.launch {
            // Only this item's dialog spins; the list stays on screen.
            _state.update { it.copy(distributingItemId = itemId) }
            val result = distributeInventoryItemUseCase(itemId)
            _state.update { it.copy(distributingItemId = null) }
            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_item_distributed)))
                    loadInventory()
                },
                onFailure = { error ->
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: context.getString(R.string.error_distribute_item_failed)))
                }
            )
        }
    }

    private fun bulkDistribute() {
        val ids = _state.value.selectedIds
            .filter { id -> _state.value.filteredItems.any { it.id == id && it.status.name == "ACTIVE" } }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isBulkOperating = true) }
            var succeeded = 0
            ids.forEach { id ->
                distributeInventoryItemUseCase(id).onSuccess { succeeded++ }
            }
            _state.update { it.copy(isBulkOperating = false, isSelectionMode = false, selectedIds = emptySet()) }
            _uiEvent.send(UiEvent.ShowSnackbar(BulkResult.message(context, succeeded, ids.size, R.plurals.snack_n_items_distributed)))
            loadInventory()
        }
    }

    private fun InventoryEvent.isLockedWhileBusy(): Boolean = when (this) {
            is InventoryEvent.ToggleItemSelection,
            is InventoryEvent.SelectAll,
            is InventoryEvent.DeselectAll,
            is InventoryEvent.ToggleSelectionMode,
            is InventoryEvent.StatusTabChanged,
            is InventoryEvent.SearchQueryChanged,
            is InventoryEvent.SortOptionChanged,
            is InventoryEvent.CategoryFilterChanged,
            is InventoryEvent.FilterExpiryWithinDaysChanged,
            is InventoryEvent.FilterMinQtyChanged,
            is InventoryEvent.FilterMaxQtyChanged,
            is InventoryEvent.ClearAdvancedFilters,
            is InventoryEvent.ShowFilterSheet,
            is InventoryEvent.RefreshInventory,
            is InventoryEvent.LoadInventory,
            is InventoryEvent.DistributeItem,
            is InventoryEvent.BulkDistribute -> true
        else -> false
    }
}
