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
    private val distributeItemUseCase: DistributeItemUseCase,
    private val updateExpiredItemsUseCase: UpdateExpiredItemsUseCase
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
            InventoryEvent.UpdateExpired -> updateExpired()

            is InventoryEvent.StatusFilterChanged -> {
                _state.update { it.copy(selectedStatus = event.status) }
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

            // First update expired items
            updateExpiredItemsUseCase()

            // Then load inventory
            val result = getMyInventoryUseCase()

            result.fold(
                onSuccess = { items ->
                    _state.update {
                        it.copy(
                            allItems = items,
                            filteredItems = items,
                            isLoading = false
                        )
                    }
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

            // Update expired items
            updateExpiredItemsUseCase()

            // Reload inventory
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

    private fun updateExpired() {
        viewModelScope.launch {
            updateExpiredItemsUseCase()
            loadInventory()
        }
    }

    private fun applyFilters() {
        val currentState = _state.value
        var filtered = currentState.allItems

        currentState.selectedStatus?.let { status ->
            filtered = filtered.filter { it.status.name == status }
        }

        _state.update { it.copy(filteredItems = filtered) }
    }

    private fun distributeItem(itemId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = distributeItemUseCase(itemId)

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