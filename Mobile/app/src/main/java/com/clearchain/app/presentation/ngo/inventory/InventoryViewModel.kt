package com.clearchain.app.presentation.ngo.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.data.remote.signalr.SignalRService  // ✅ ADD
import com.clearchain.app.domain.usecase.inventory.DistributeItemUseCase
import com.clearchain.app.domain.usecase.inventory.GetMyInventoryUseCase
import com.clearchain.app.domain.usecase.inventory.UpdateExpiredItemsUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getMyInventoryUseCase: GetMyInventoryUseCase,
    private val updateExpiredItemsUseCase: UpdateExpiredItemsUseCase,
    private val distributeInventoryItemUseCase: DistributeItemUseCase,
    private val signalRService: SignalRService  // ✅ ADD
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadInventory()
        setupSignalR()  // ✅ ADD
    }

    // ✅ NEW: Setup SignalR real-time updates
    private fun setupSignalR() {
        // Connect to SignalR (reuse existing connection)
        viewModelScope.launch {
            signalRService.connect()
        }

        // Listen for connection state
        viewModelScope.launch {
            signalRService.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        _uiEvent.send(UiEvent.ShowSnackbar("✅ Real-time updates enabled"))
                    }
                    is ConnectionState.Error -> {
                        // Silent fail - app works without real-time
                    }
                    else -> {}
                }
            }
        }

        // ✅ Listen for NEW inventory items (from confirmed pickups)
        viewModelScope.launch {
            signalRService.inventoryItemAdded.collect { item ->
                loadInventory() // Auto-refresh
                _uiEvent.send(
                    UiEvent.ShowSnackbar("📦 New item received: ${item.productName}")
                )
            }
        }

        // ✅ Listen for distributed items
        viewModelScope.launch {
            signalRService.inventoryItemDistributed.collect { notification ->
                loadInventory()
                _uiEvent.send(
                    UiEvent.ShowSnackbar("✅ Item marked as distributed")
                )
            }
        }

        // ✅ Listen for expired items
        viewModelScope.launch {
            signalRService.inventoryItemExpired.collect { item ->
                loadInventory()
                _uiEvent.send(
                    UiEvent.ShowSnackbar("⚠️ Item expired: ${item.productName}")
                )
            }
        }

        // ✅ Listen for item updates
        viewModelScope.launch {
            signalRService.inventoryItemUpdated.collect { item ->
                loadInventory()
                _uiEvent.send(
                    UiEvent.ShowSnackbar("Item updated: ${item.productName}")
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            signalRService.disconnect()
        }
    }

    fun onEvent(event: InventoryEvent) {
        when (event) {
            InventoryEvent.LoadInventory -> loadInventory()
            InventoryEvent.RefreshInventory -> refreshInventory()
            InventoryEvent.UpdateExpired -> updateExpiredItems()
            
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

            InventoryEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun loadInventory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Update expired items first
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
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load inventory"
                        )
                    }
                }
            )
        }
    }

    private fun refreshInventory() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            // Update expired items first
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
                    _uiEvent.send(UiEvent.ShowSnackbar("Inventory refreshed"))
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = error.message ?: "Failed to refresh inventory"
                        )
                    }
                }
            )
        }
    }

    private fun updateExpiredItems() {
        viewModelScope.launch {
            updateExpiredItemsUseCase()
            loadInventory()
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
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = distributeInventoryItemUseCase(itemId)

            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar("Item marked as distributed"))
                    loadInventory() // Reload list
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Failed to distribute item",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }
}