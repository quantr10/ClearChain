// ═══════════════════════════════════════════════════════════════════════════════
// InventoryViewModel.kt - UPDATED WITH CATEGORY FILTER & STATUS TAB
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.ngo.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val distributeInventoryItemUseCase: DistributeItemUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadInventory()
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
            
            // NEW: Status Tab (changed from chip)
            is InventoryEvent.StatusTabChanged -> {
                _state.update { it.copy(selectedStatusTab = event.status) }
                applyFilters()
            }
            
            // NEW: Category Filter (food categories)
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

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATED: Filter and Sort Logic with Status TAB + Category CHIP
    // ═══════════════════════════════════════════════════════════════════════════
    
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
                // Match by FoodCategory enum name
                item.category.equals(category, ignoreCase = true) ||
                // Or if category is stored as display name, try matching that too
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