package com.clearchain.app.presentation.ngo.inventorydetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.ListingApi
import com.clearchain.app.data.remote.api.PickupRequestApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.di.ApplicationScope
import com.clearchain.app.domain.repository.InventoryRepository
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────
@HiltViewModel
class InventoryDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inventoryRepository: InventoryRepository,
    private val pickupRequestApi: PickupRequestApi,
    private val listingApi: ListingApi,
    private val signalRService: SignalRService,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryDetailState())
    val state: StateFlow<InventoryDetailState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    /** Room this screen joined, so it can be left when the screen goes away. */
    private var joinedItemId: String? = null

    override fun onCleared() {
        super.onCleared()
        joinedItemId?.let { itemId ->
            applicationScope.launch {
                signalRService.leaveInventoryItemRoom(itemId)
            }
        }
    }

    /**
     * The server broadcasts per-item changes to `item_{id}`, a group nothing joined before — so
     * an item edited on another device left this screen showing stale values indefinitely.
     */
    private fun observeSignalR(itemId: String) {
        if (joinedItemId == itemId) return

        viewModelScope.launch { signalRService.joinInventoryItemRoom(itemId) }
        joinedItemId = itemId

        viewModelScope.launch {
            signalRService.inventoryItemUpdated.collect { data ->
                if (data.id == itemId) {
                    _state.update { it.copy(item = data.toDomain()) }
                }
            }
        }
        viewModelScope.launch {
            signalRService.inventoryItemExpired.collect { data ->
                if (data.id == itemId) {
                    _state.update { it.copy(item = data.toDomain()) }
                }
            }
        }
    }

    fun loadItem(itemId: String) {
        observeSignalR(itemId)
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            inventoryRepository.getInventoryItemById(itemId).fold(
                onSuccess = { item ->
                    _state.update { it.copy(item = item, isLoading = false) }
                    item.pickupRequestId?.let { requestId ->
                        loadRelatedRequest(requestId)
                    }
                },
                onFailure = { e ->
                    val msg = e.message ?: context.getString(R.string.error_load_item_failed)
                    _state.update { it.copy(error = msg, isLoading = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar(msg))
                }
            )
        }
    }

    fun showQrSheet() = _state.update { it.copy(showQrSheet = true) }
    fun dismissQrSheet() = _state.update { it.copy(showQrSheet = false) }

    private fun loadRelatedRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRequest = true) }
            try {
                val response = pickupRequestApi.getPickupRequestById(requestId)
                val request = response.data.toDomain()
                _state.update { it.copy(relatedRequest = request, isLoadingRequest = false) }
                loadMoreFromStore(request.groceryId, request.listingId)
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingRequest = false) }
            }
        }
    }

    private fun loadMoreFromStore(groceryId: String, excludeListingId: String) {
        viewModelScope.launch {
            runCatching {
                listingApi.getAllListings(status = "open", groceryId = groceryId, pageSize = 6)
                    .data
                    .map { it.toDomain() }
                    .filterNot { it.id == excludeListingId }
            }.onSuccess { listings ->
                _state.update { it.copy(moreFromStore = listings) }
            }
        }
    }
}
