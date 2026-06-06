package com.clearchain.app.presentation.ngo.listingdetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.ListingApi
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.api.ReportApi
import com.clearchain.app.data.remote.api.SavedListingApi
import com.clearchain.app.data.remote.dto.SubmitReportRequest
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.ListingStatus
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.repository.ListingRepository
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.listing.DeleteListingUseCase
import com.clearchain.app.domain.usecase.listing.UpdateListingQuantityUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val listingRepository: ListingRepository,
    private val listingApi: ListingApi,
    private val reportApi: ReportApi,
    private val savedListingApi: SavedListingApi,
    private val organizationApi: OrganizationApi,
    private val deleteListingUseCase: DeleteListingUseCase,
    private val updateListingQuantityUseCase: UpdateListingQuantityUseCase,
    private val signalRService: SignalRService,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ListingDetailState())
    val state: StateFlow<ListingDetailState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().first()?.let { user ->
                _state.update { it.copy(currentUserType = user.type) }
            }
        }
    }

    fun onEvent(event: ListingDetailEvent) {
        when (event) {
            is ListingDetailEvent.LoadListing        -> loadListing(event.listingId)
            ListingDetailEvent.ShowReportDialog      -> showReportDialog()
            ListingDetailEvent.DismissReportDialog   -> dismissReportDialog()
            is ListingDetailEvent.ReportReasonChanged -> onReportReasonChanged(event.reason)
            ListingDetailEvent.SubmitReport          -> submitReport()
            ListingDetailEvent.ToggleSave            -> toggleSave()
            ListingDetailEvent.ArchiveListing        -> archiveListing()
            ListingDetailEvent.UnarchiveListing      -> unarchiveListing()
            ListingDetailEvent.ShowDeleteConfirm     -> _state.update { it.copy(showDeleteConfirm = true) }
            ListingDetailEvent.DismissDeleteConfirm  -> _state.update { it.copy(showDeleteConfirm = false) }
            ListingDetailEvent.DeleteListing         -> deleteListing()
            is ListingDetailEvent.UpdateQuantity     -> updateQuantity(event.newQuantity)
        }
    }

    private fun loadListing(listingId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            listingRepository.getListingById(listingId).fold(
                onSuccess = { listing ->
                    _state.update { it.copy(listing = listing, isLoading = false) }
                    loadSimilarListings(listing.category.name, listingId)
                    loadGroceryProfile(listing.groceryId)
                    if (_state.value.currentUserType == OrganizationType.NGO) {
                        loadSavedStatus(listingId)
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message ?: "Failed to load listing", isLoading = false) }
                }
            )
        }
        observeSignalR(listingId)
    }

    private fun loadSimilarListings(category: String, excludeId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingSimilar = true) }
            listingRepository.getAllListings(status = "available", category = category, pageSize = 10)
                .onSuccess { listings ->
                    _state.update {
                        it.copy(
                            similarListings = listings.filter { l -> l.id != excludeId && l.status == ListingStatus.AVAILABLE }.take(5),
                            isLoadingSimilar = false
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isLoadingSimilar = false) } }
        }
    }

    private fun loadGroceryProfile(orgId: String) {
        viewModelScope.launch {
            runCatching { organizationApi.getPublicProfile(orgId).data }
                .onSuccess { profile -> _state.update { it.copy(groceryProfile = profile) } }
        }
    }

    private fun loadSavedStatus(listingId: String) {
        viewModelScope.launch {
            runCatching { savedListingApi.getSavedListingIds().data }
                .onSuccess { ids -> _state.update { it.copy(isSaved = listingId in ids) } }
        }
    }

    private fun observeSignalR(listingId: String) {
        viewModelScope.launch {
            signalRService.listingUpdated.collect { data ->
                if (data.id == listingId) {
                    listingRepository.getListingById(listingId).onSuccess { listing ->
                        _state.update { it.copy(listing = listing) }
                    }
                }
            }
        }
        viewModelScope.launch {
            signalRService.listingQuantityChanged.collect { notification ->
                if (notification.listing.id == listingId) {
                    _state.update { it.copy(availabilityOverride = notification.listing.quantity) }
                }
            }
        }
    }

    // ── Report ────────────────────────────────────────────────────────────────

    private fun showReportDialog() = _state.update { it.copy(showReportDialog = true, reportReason = "", reportSubmitted = false) }
    private fun dismissReportDialog() = _state.update { it.copy(showReportDialog = false) }
    private fun onReportReasonChanged(reason: String) = _state.update { it.copy(reportReason = reason) }

    private fun submitReport() {
        val listing = _state.value.listing ?: return
        val reason = _state.value.reportReason.trim()
        if (reason.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingReport = true) }
            runCatching {
                reportApi.submitReport(SubmitReportRequest(listingId = listing.id, reason = reason))
            }.onSuccess {
                _state.update { it.copy(isSubmittingReport = false, reportSubmitted = true, showReportDialog = false) }
            }.onFailure {
                _state.update { it.copy(isSubmittingReport = false) }
            }
        }
    }

    // ── Save/Favourite ────────────────────────────────────────────────────────

    private fun toggleSave() {
        val listing = _state.value.listing ?: return
        if (_state.value.isTogglingFave) return
        viewModelScope.launch {
            _state.update { it.copy(isTogglingFave = true) }
            runCatching {
                if (_state.value.isSaved) savedListingApi.unsaveListing(listing.id)
                else savedListingApi.saveListing(listing.id)
            }.onSuccess {
                _state.update { it.copy(isSaved = !it.isSaved, isTogglingFave = false) }
            }.onFailure {
                _state.update { it.copy(isTogglingFave = false) }
            }
        }
    }

    // ── Grocery actions ───────────────────────────────────────────────────────

    private fun archiveListing() {
        val listing = _state.value.listing ?: return
        viewModelScope.launch {
            _state.update { it.copy(isArchiving = true) }
            runCatching { listingApi.archiveListing(listing.id) }.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_listing_archived)))
                    loadListing(listing.id)
                    _state.update { it.copy(isArchiving = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isArchiving = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_archive_listing_failed)))
                }
            )
        }
    }

    private fun unarchiveListing() {
        val listing = _state.value.listing ?: return
        viewModelScope.launch {
            _state.update { it.copy(isArchiving = true) }
            runCatching { listingApi.unarchiveListing(listing.id) }.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_listing_restored)))
                    loadListing(listing.id)
                    _state.update { it.copy(isArchiving = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isArchiving = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_restore_listing_failed)))
                }
            )
        }
    }

    private fun deleteListing() {
        val listing = _state.value.listing ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, showDeleteConfirm = false) }
            deleteListingUseCase(listing.id).fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_listing_deleted)))
                    _uiEvent.send(UiEvent.NavigateUp)
                },
                onFailure = { e ->
                    _state.update { it.copy(isDeleting = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_delete_listing_failed)))
                }
            )
        }
    }

    private fun updateQuantity(newQuantity: Int) {
        val listing = _state.value.listing ?: return
        viewModelScope.launch {
            updateListingQuantityUseCase(listing.id, newQuantity).fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_quantity_updated)))
                    loadListing(listing.id)
                },
                onFailure = { e ->
                    _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: "Failed to update quantity"))
                }
            )
        }
    }
}
