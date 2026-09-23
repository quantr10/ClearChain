package com.clearchain.app.presentation.ngo.listingdetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.local.LocationPreferenceStore
import com.clearchain.app.data.remote.api.*
import com.clearchain.app.data.remote.dto.AddCartItemRequest
import com.clearchain.app.data.remote.dto.CartGroupData
import com.clearchain.app.data.remote.dto.SubmitReportRequest
import com.clearchain.app.data.remote.dto.UpdateCartItemRequest
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.di.ApplicationScope
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.repository.ListingRepository
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.listing.DeleteListingUseCase
import com.clearchain.app.domain.usecase.listing.UpdateListingQuantityUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val listingRepository: ListingRepository,
    private val listingApi: ListingApi,
    private val cartApi: CartApi,
    private val reportApi: ReportApi,
    private val savedListingApi: SavedListingApi,
    private val organizationApi: OrganizationApi,
    private val locationPreferenceStore: LocationPreferenceStore,
    private val deleteListingUseCase: DeleteListingUseCase,
    private val updateListingQuantityUseCase: UpdateListingQuantityUseCase,
    private val signalRService: SignalRService,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val _state = MutableStateFlow(ListingDetailState())
    val state: StateFlow<ListingDetailState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    /** Room this screen joined, so it can be left when the screen goes away. */
    private var joinedListingId: String? = null

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().first()?.let { user ->
                _state.update { it.copy(currentUserType = user.type) }
                if (user.type == OrganizationType.NGO) loadCart()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Leaving the room is a fire-and-forget send on the shared connection — it releases a
        // server-side group membership without touching the connection itself.
        joinedListingId?.let { listingId ->
            applicationScope.launch {
                signalRService.leaveListingRoom(listingId)
            }
        }
    }

    fun onEvent(event: ListingDetailEvent) {
        when (event) {
            is ListingDetailEvent.LoadListing -> loadListing(event.listingId)
            ListingDetailEvent.ShowReportDialog -> showReportDialog()
            ListingDetailEvent.DismissReportDialog -> dismissReportDialog()
            is ListingDetailEvent.ReportReasonChanged -> onReportReasonChanged(event.reason)
            ListingDetailEvent.SubmitReport -> submitReport()
            ListingDetailEvent.ToggleSave -> toggleSave()
            ListingDetailEvent.ShowDeleteConfirm -> _state.update { it.copy(showDeleteConfirm = true) }
            ListingDetailEvent.DismissDeleteConfirm -> _state.update { it.copy(showDeleteConfirm = false) }
            ListingDetailEvent.DeleteListing -> deleteListing()
            is ListingDetailEvent.UpdateQuantity -> updateQuantity(event.newQuantity)
            is ListingDetailEvent.AddToCart -> addToCart(event.listingId)
            is ListingDetailEvent.IncrementCartItem -> addToCart(event.listingId)
            is ListingDetailEvent.DecrementCartItem -> decrementCartItem(event.listingId)
        }
    }

    private fun loadListing(listingId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            listingRepository.getListingById(listingId).fold(
                onSuccess = { listing ->
                    _state.update { it.copy(listing = listing, isLoading = false) }
                    loadSimilarListings(listingId)
                    loadGroceryProfile(listing.groceryId)
                    if (_state.value.currentUserType == OrganizationType.NGO) {
                        loadSavedStatus(listingId)
                        loadCart()
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message ?: context.getString(R.string.error_failed_load_listing), isLoading = false) }
                }
            )
        }
        observeSignalR(listingId)
    }

    private fun loadSimilarListings(excludeId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingSimilar = true) }
            val locationPreference = locationPreferenceStore.locationPreference.first()
            if (locationPreference == null) {
                _state.update { it.copy(similarListings = emptyList(), isLoadingSimilar = false) }
                return@launch
            }

            listingRepository.getAllListings(
                status = "open",
                lat = locationPreference.latitude,
                lng = locationPreference.longitude,
                radiusKm = locationPreference.radiusKm,
                pageSize = 10
            )
                .onSuccess { listings ->
                    _state.update {
                        it.copy(
                            similarListings = listings
                                .filter { listing ->
                                    listing.id != excludeId &&
                                        isWithinSelectedRadius(
                                            listing = listing,
                                            latitude = locationPreference.latitude,
                                            longitude = locationPreference.longitude,
                                            radiusKm = locationPreference.radiusKm
                                        )
                                }
                                .take(5),
                            isLoadingSimilar = false
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isLoadingSimilar = false) } }
        }
    }

    private fun isWithinSelectedRadius(
        listing: Listing,
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): Boolean {
        val groceryLat = listing.groceryLatitude
        val groceryLng = listing.groceryLongitude
        if (groceryLat != null && groceryLng != null) {
            return distanceKm(latitude, longitude, groceryLat, groceryLng) <= radiusKm
        }

        return listing.distanceKm?.let { it <= radiusKm } ?: false
    }

    private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
        return 2 * radius * atan2(sqrt(a), sqrt(1 - a))
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

    private fun loadCart() {
        viewModelScope.launch {
            runCatching { cartApi.getCart() }
                .onSuccess { response -> updateCartState(response.data) }
        }
    }

    private fun addToCart(listingId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdatingCart = true) }
            runCatching { cartApi.addItem(AddCartItemRequest(listingId = listingId, quantity = 1)) }
                .onSuccess { response ->
                    updateCartState(response.data)
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_item_added)))
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message ?: context.getString(R.string.error_generic)) }
                }
            _state.update { it.copy(isUpdatingCart = false) }
        }
    }

    private fun decrementCartItem(listingId: String) {
        val item = _state.value.cartItemsByListingId[listingId] ?: return
        viewModelScope.launch {
            _state.update { it.copy(isUpdatingCart = true) }
            runCatching { cartApi.updateItem(item.id, UpdateCartItemRequest(quantity = item.requestedQuantity - 1)) }
                .onSuccess { response -> updateCartState(response.data) }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message ?: context.getString(R.string.error_generic)) }
                }
            _state.update { it.copy(isUpdatingCart = false) }
        }
    }

    private fun updateCartState(groups: List<CartGroupData>) {
        _state.update {
            it.copy(cartItemsByListingId = groups.flatMap { group -> group.items }.associateBy { item -> item.listingId })
        }
    }

    private fun observeSignalR(listingId: String) {
        // loadListing() calls this on every reload (including after a successful quantity
        // update), so without this guard each reload added another permanent room join plus
        // two more listingUpdated/listingQuantityChanged collectors that were never cancelled.
        if (joinedListingId == listingId) return

        // The server's `listing_{id}` group has no members until someone joins it, so without
        // this the per-listing broadcasts never reach anyone. Left in leaveListingRoom below.
        viewModelScope.launch { signalRService.joinListingRoom(listingId) }
        joinedListingId = listingId

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

    // ── Report ───────────────────────────────────────────────────────────────

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

    // ── Save/Favourite ───────────────────────────────────────────────────────

    private fun toggleSave() {
        val listing = _state.value.listing ?: return
        if (_state.value.isTogglingFave) return
        viewModelScope.launch {
            _state.update { it.copy(isTogglingFave = true) }
            runCatching {
                if (_state.value.isSaved) {
                    savedListingApi.unsaveListing(listing.id)
                } else {
                    savedListingApi.saveListing(listing.id)
                }
            }.onSuccess {
                _state.update { it.copy(isSaved = !it.isSaved, isTogglingFave = false) }
            }.onFailure {
                _state.update { it.copy(isTogglingFave = false) }
            }
        }
    }

    // ── Grocery actions ──────────────────────────────────────────────────────

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
                    _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_update_quantity_failed)))
                }
            )
        }
    }
}
