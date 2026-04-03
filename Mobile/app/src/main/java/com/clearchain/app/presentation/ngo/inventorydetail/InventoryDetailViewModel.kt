package com.clearchain.app.presentation.ngo.inventorydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.InventoryApi
import com.clearchain.app.data.remote.api.PickupRequestApi
import com.clearchain.app.data.remote.dto.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══ ViewModel ═══
@HiltViewModel
class InventoryDetailViewModel @Inject constructor(
    private val inventoryApi: InventoryApi,
    private val pickupRequestApi: PickupRequestApi
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryDetailState())
    val state: StateFlow<InventoryDetailState> = _state.asStateFlow()

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = inventoryApi.getInventoryItemById(itemId)
                val item = response.data.toDomain()
                _state.update { it.copy(item = item, isLoading = false) }

                // Load related pickup request if available
                item.pickupRequestId?.let { requestId ->
                    loadRelatedRequest(requestId)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load item", isLoading = false) }
            }
        }
    }

    private fun loadRelatedRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRequest = true) }
            try {
                val response = pickupRequestApi.getPickupRequestById(requestId)
                _state.update { it.copy(relatedRequest = response.data.toDomain(), isLoadingRequest = false) }
            } catch (e: Exception) {
                // Non-fatal — just don't show request info
                _state.update { it.copy(isLoadingRequest = false) }
            }
        }
    }
}