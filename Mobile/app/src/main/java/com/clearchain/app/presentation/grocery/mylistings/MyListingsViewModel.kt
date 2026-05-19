package com.clearchain.app.presentation.grocery.mylistings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.ListingApi
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.ListingStatus
import com.clearchain.app.domain.model.displayName
import com.clearchain.app.domain.usecase.listing.DeleteListingUseCase
import com.clearchain.app.domain.usecase.listing.GetMyListingsUseCase
import com.clearchain.app.domain.usecase.listing.UpdateListingQuantityUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyListingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getMyListingsUseCase: GetMyListingsUseCase,
    private val deleteListingUseCase: DeleteListingUseCase,
    private val updateListingQuantityUseCase: UpdateListingQuantityUseCase,
    private val listingApi: ListingApi,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(MyListingsState())
    val state: StateFlow<MyListingsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadListings()
        setupSignalR()
    }

    private fun setupSignalR() {
        viewModelScope.launch { signalRService.connect() }

        viewModelScope.launch {
            signalRService.listingCreated.collect { loadListings() }
        }
        viewModelScope.launch {
            signalRService.listingDeleted.collect { loadListings() }
        }
        viewModelScope.launch {
            signalRService.listingUpdated.collect { loadListings() }
        }
        viewModelScope.launch {
            signalRService.pickupRequestCreated.collect { loadListings() }
        }
        viewModelScope.launch {
            signalRService.pickupRequestCancelled.collect { loadListings() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { signalRService.disconnect() }
    }

    fun onEvent(event: MyListingsEvent) {
        when (event) {
            MyListingsEvent.LoadListings -> loadListings()
            MyListingsEvent.RefreshListings -> refreshListings()
            is MyListingsEvent.DeleteListing -> deleteListing(event.listingId)
            is MyListingsEvent.ArchiveListing -> archiveListing(event.listingId)
            is MyListingsEvent.UnarchiveListing -> unarchiveListing(event.listingId)

            is MyListingsEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is MyListingsEvent.SortOptionChanged -> {
                _state.update { it.copy(selectedSort = event.option) }
                applyFilters()
            }
            is MyListingsEvent.TabChanged -> {
                _state.update { it.copy(activeTab = event.tab, isSelectionMode = false, selectedIds = emptySet()) }
                applyFilters()
            }
            is MyListingsEvent.CategoryFilterChanged -> {
                _state.update { it.copy(selectedCategory = event.category) }
                applyFilters()
            }

            // Advanced filter sheet
            MyListingsEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = true) }
            MyListingsEvent.HideFilterSheet -> _state.update { it.copy(showFilterSheet = false) }
            is MyListingsEvent.FilterExpiryWithinDaysChanged -> {
                _state.update { it.copy(filterExpiryWithinDays = event.days) }
                applyFilters()
            }
            is MyListingsEvent.FilterHasRequestsChanged -> {
                _state.update { it.copy(filterHasRequests = event.enabled) }
                applyFilters()
            }
            MyListingsEvent.ClearAdvancedFilters -> {
                _state.update { it.copy(selectedCategory = null, filterExpiryWithinDays = null, filterHasRequests = false) }
                applyFilters()
            }

            // Bulk selection
            MyListingsEvent.ToggleSelectionMode ->
                _state.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIds = emptySet()) }
            is MyListingsEvent.ToggleItemSelection ->
                _state.update {
                    val updated = if (event.listingId in it.selectedIds)
                        it.selectedIds - event.listingId
                    else
                        it.selectedIds + event.listingId
                    it.copy(selectedIds = updated, isSelectionMode = updated.isNotEmpty())
                }
            MyListingsEvent.SelectAll ->
                _state.update { it.copy(selectedIds = it.filteredListings.map { l -> l.id }.toSet()) }
            MyListingsEvent.DeselectAll ->
                _state.update { it.copy(selectedIds = emptySet()) }
            MyListingsEvent.BulkDelete -> bulkDelete()
            MyListingsEvent.BulkArchive -> bulkArchive()
            MyListingsEvent.BulkRestore -> bulkRestore()

            is MyListingsEvent.UpdateListingQuantity ->
                updateListingQuantity(event.listingId, event.newQuantity)
            MyListingsEvent.ClearError ->
                _state.update { it.copy(error = null) }
        }
    }

    private fun loadListings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getMyListingsUseCase().fold(
                onSuccess = { listings ->
                    _state.update { it.copy(allListings = listings, isLoading = false) }
                    applyFilters()
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load listings") }
                }
            )
        }
    }

    private fun refreshListings() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            getMyListingsUseCase().fold(
                onSuccess = { listings ->
                    _state.update { it.copy(allListings = listings, isRefreshing = false) }
                    applyFilters()
                },
                onFailure = { _state.update { it.copy(isRefreshing = false) } }
            )
        }
    }

    private fun deleteListing(listingId: String) {
        viewModelScope.launch {
            deleteListingUseCase(listingId).fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_listing_deleted)))
                    loadListings()
                },
                onFailure = { error ->
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: context.getString(R.string.error_delete_listing_failed)))
                }
            )
        }
    }

    private fun archiveListing(listingId: String) {
        viewModelScope.launch {
            try {
                listingApi.archiveListing(listingId)
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_listing_archived)))
                loadListings()
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_archive_listing_failed)))
            }
        }
    }

    private fun unarchiveListing(listingId: String) {
        viewModelScope.launch {
            try {
                listingApi.unarchiveListing(listingId)
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_listing_restored)))
                loadListings()
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_restore_listing_failed)))
            }
        }
    }

    private fun bulkDelete() {
        val ids = _state.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isBulkOperating = true) }
            var success = 0
            ids.forEach { id ->
                deleteListingUseCase(id).onSuccess { success++ }
            }
            _state.update { it.copy(isBulkOperating = false, isSelectionMode = false, selectedIds = emptySet()) }
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_n_listings_deleted, success)))
            loadListings()
        }
    }

    private fun bulkArchive() {
        val ids = _state.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isBulkOperating = true) }
            var success = 0
            ids.forEach { id ->
                try { listingApi.archiveListing(id); success++ } catch (_: Exception) {}
            }
            _state.update { it.copy(isBulkOperating = false, isSelectionMode = false, selectedIds = emptySet()) }
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_n_listings_archived, success)))
            loadListings()
        }
    }

    private fun bulkRestore() {
        val ids = _state.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isBulkOperating = true) }
            var success = 0
            ids.forEach { id ->
                try { listingApi.unarchiveListing(id); success++ } catch (_: Exception) {}
            }
            _state.update { it.copy(isBulkOperating = false, isSelectionMode = false, selectedIds = emptySet()) }
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_n_listings_restored, success)))
            loadListings()
        }
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allListings

        filtered = when (current.activeTab) {
            MyListingsTab.AVAILABLE -> filtered.filter { !it.isArchived && it.status == ListingStatus.AVAILABLE }
            MyListingsTab.ARCHIVED  -> filtered.filter { it.isArchived }
            MyListingsTab.RESERVED  -> filtered.filter { !it.isArchived && it.status == ListingStatus.RESERVED }
            MyListingsTab.EXPIRED   -> filtered.filter { !it.isArchived && it.status == ListingStatus.EXPIRED }
        }

        if (current.searchQuery.isNotBlank()) {
            val query = current.searchQuery.lowercase()
            filtered = filtered.filter { listing ->
                listing.title.lowercase().contains(query) ||
                listing.description.lowercase().contains(query) ||
                listing.location.lowercase().contains(query) ||
                listing.category.displayName().lowercase().contains(query)
            }
        }

        current.selectedCategory?.let { category ->
            filtered = filtered.filter { it.category.name == category }
        }

        current.filterExpiryWithinDays?.let { days ->
            val threshold = java.time.LocalDate.now().plusDays(days.toLong())
            filtered = filtered.filter { listing ->
                runCatching {
                    !java.time.LocalDate.parse(listing.expiryDate.take(10)).isAfter(threshold)
                }.getOrDefault(true)
            }
        }

        if (current.filterHasRequests) {
            filtered = filtered.filter { it.requestCount > 0 }
        }

        filtered = when (current.selectedSort.value) {
            "date_desc"     -> filtered.sortedByDescending { it.createdAt }
            "date_asc"      -> filtered.sortedBy { it.createdAt }
            "name_asc"      -> filtered.sortedBy { it.title }
            "name_desc"     -> filtered.sortedByDescending { it.title }
            "quantity_desc" -> filtered.sortedByDescending { it.quantity }
            "quantity_asc"  -> filtered.sortedBy { it.quantity }
            "expiry_asc"    -> filtered.sortedBy { it.expiryDate }
            "expiry_desc"   -> filtered.sortedByDescending { it.expiryDate }
            else            -> filtered
        }

        _state.update { it.copy(filteredListings = filtered) }
    }

    private fun updateListingQuantity(listingId: String, newQuantity: Int) {
        viewModelScope.launch {
            updateListingQuantityUseCase(listingId, newQuantity).fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_quantity_updated)))
                    loadListings()
                },
                onFailure = { error ->
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: "Failed to update quantity"))
                }
            )
        }
    }
}
