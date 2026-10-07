package com.clearchain.app.presentation.ngo.myrequests

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.itemTitles
import com.clearchain.app.domain.model.searchText
import com.clearchain.app.domain.usecase.pickuprequest.CancelPickupRequestUseCase
import com.clearchain.app.domain.usecase.pickuprequest.ConfirmPickupUseCase
import com.clearchain.app.domain.usecase.pickuprequest.GetMyPickupRequestsUseCase
import com.clearchain.app.presentation.components.RequestAction
import com.clearchain.app.util.DownloadsExport
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class MyRequestsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getMyPickupRequestsUseCase: GetMyPickupRequestsUseCase,
    private val cancelPickupRequestUseCase: CancelPickupRequestUseCase,
    private val confirmPickupUseCase: ConfirmPickupUseCase,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(MyRequestsState())
    val state: StateFlow<MyRequestsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    companion object {
        private const val MAX_UPLOAD_ATTEMPTS = 3
    }

    init {
        loadRequests()
        setupSignalR()
    }

    // Setup SignalR real-time updates
    private fun setupSignalR() {
        // Listen for status changes
        viewModelScope.launch {
            signalRService.pickupRequestStatusChanged.collect { notification ->
                // Auto-refresh list
                loadRequests()

                // Show notification to user
                val statusMessage = when (notification.newStatus.lowercase()) {
                    "approved" -> context.getString(R.string.snack_your_request_approved)
                    "ready" -> context.getString(R.string.snack_food_ready_pickup)
                    "completed" -> context.getString(R.string.snack_pickup_completed)
                    "rejected" -> context.getString(R.string.snack_request_rejected_by_grocery)
                    else -> context.getString(R.string.snack_status_updated_to, notification.newStatus)
                }

                _uiEvent.send(UiEvent.ShowSnackbar(statusMessage))
            }
        }

        // Listen for cancellations
        viewModelScope.launch {
            signalRService.pickupRequestCancelled.collect {
                loadRequests()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_request_cancelled)))
            }
        }
    }

    fun onEvent(event: MyRequestsEvent) {
        when (event) {
            MyRequestsEvent.LoadRequests -> loadRequests()
            MyRequestsEvent.RefreshRequests -> refreshRequests()

            is MyRequestsEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is MyRequestsEvent.SortOptionChanged -> {
                _state.update { it.copy(selectedSort = event.option) }
                applyFilters()
            }
            is MyRequestsEvent.StatusFilterChanged -> {
                _state.update { it.copy(selectedStatus = event.status) }
                applyFilters()
            }

            is MyRequestsEvent.CancelRequest -> cancelRequest(event.requestId)

            is MyRequestsEvent.ConfirmPickupWithPhoto ->
                confirmPickupWithPhoto(event.requestId, event.photoUri)

            MyRequestsEvent.RetryFailedUpload -> retryFailedUpload()
            MyRequestsEvent.DismissUploadError -> dismissUploadError()

            MyRequestsEvent.ExportCsv -> exportCsv()

            MyRequestsEvent.ShowFilterSheet ->
                _state.update { it.copy(showFilterSheet = true) }
            MyRequestsEvent.HideFilterSheet ->
                _state.update { it.copy(showFilterSheet = false) }
            is MyRequestsEvent.FilterCategoryChanged -> {
                _state.update { it.copy(filterCategory = event.category) }
                applyFilters()
            }
            is MyRequestsEvent.FilterPickupDatePresetChanged -> {
                _state.update { it.copy(filterPickupDatePreset = event.preset) }
                applyFilters()
            }
            MyRequestsEvent.ClearAdvancedFilters -> {
                _state.update { it.copy(filterCategory = null, filterPickupDatePreset = null) }
                applyFilters()
            }
        }
    }

    private fun loadRequests() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = getMyPickupRequestsUseCase()

            result.fold(
                onSuccess = { requests ->
                    _state.update {
                        it.copy(
                            allRequests = requests,
                            isLoading = false
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    val msg = error.message ?: context.getString(R.string.error_load_requests)
                    _state.update { it.copy(isLoading = false, error = msg) }
                    // Data already on screen stays; the failure is reported, not hidden.
                    if (_state.value.allRequests.isNotEmpty()) _uiEvent.send(UiEvent.ShowSnackbar(msg))
                }
            )
        }
    }

    private fun refreshRequests() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            val result = getMyPickupRequestsUseCase()

            result.fold(
                onSuccess = { requests ->
                    _state.update {
                        it.copy(
                            allRequests = requests,
                            isRefreshing = false
                        )
                    }
                    applyFilters()
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_requests_refreshed)))
                },
                onFailure = { error ->
                    _state.update { it.copy(isRefreshing = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: context.getString(R.string.error_refresh_requests)))
                }
            )
        }
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allRequests

        if (current.searchQuery.isNotBlank()) {
            val query = current.searchQuery.lowercase()
            filtered = filtered.filter { request ->
                request.searchText.contains(query)
            }
        }

        current.selectedStatus?.let { status ->
            filtered = filtered.filter { it.status.name == status }
        }

        current.filterCategory?.let { category ->
            filtered = filtered.filter { it.listingCategory == category }
        }

        current.filterPickupDatePreset?.let { preset ->
            val today = java.time.LocalDate.now()
            filtered = filtered.filter { request ->
                runCatching {
                    val pickupDate = java.time.LocalDate.parse(request.pickupDate.take(10))
                    when (preset) {
                        "TODAY" -> pickupDate == today
                        "WEEK" -> !pickupDate.isBefore(today) && !pickupDate.isAfter(today.plusDays(7))
                        "MONTH" -> !pickupDate.isBefore(today) && !pickupDate.isAfter(today.plusDays(30))
                        "PAST" -> pickupDate.isBefore(today)
                        else -> true
                    }
                }.getOrDefault(true)
            }
        }

        filtered = when (current.selectedSort.value) {
            "date_desc" -> filtered.sortedByDescending { it.createdAt }
            "date_asc" -> filtered.sortedBy { it.createdAt }
            "pickup_date_asc" -> filtered.sortedBy { it.pickupDate }
            "pickup_date_desc" -> filtered.sortedByDescending { it.pickupDate }
            else -> filtered
        }

        _state.update { it.copy(filteredRequests = filtered) }
    }

    private fun cancelRequest(requestId: String) {
        withPendingAction(requestId, RequestAction.CANCEL) {
            cancelPickupRequestUseCase(requestId).fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_request_cancelled)))
                    loadRequests()
                },
                onFailure = { error ->
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: context.getString(R.string.error_cancel_request_failed)))
                }
            )
        }
    }

    /** Marks [requestId] busy with [action] while [block] runs, so only its card spins. */
    private fun withPendingAction(requestId: String, action: RequestAction, block: suspend () -> Unit) {
        if (requestId in _state.value.pendingActions) return
        viewModelScope.launch {
            _state.update { it.copy(pendingActions = it.pendingActions + (requestId to action)) }
            try {
                block()
            } finally {
                _state.update { it.copy(pendingActions = it.pendingActions - requestId) }
            }
        }
    }

    private fun confirmPickupWithPhoto(requestId: String, photoUri: Uri) {
        withPendingAction(requestId, RequestAction.CONFIRM_PICKUP) {
            val currentAttempts = _state.value.uploadAttempts + 1

            // uploadError is left as-is so a retry keeps its banner (and spinning Retry
            // button) on screen until the new attempt settles.
            _state.update {
                it.copy(
                    isUploading = true,
                    uploadAttempts = currentAttempts
                )
            }

            val result = confirmPickupUseCase(requestId, photoUri)

            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isUploading = false,
                            uploadError = null,
                            uploadAttempts = 0,
                            failedUploadRequestId = null,
                            failedUploadPhotoUri = null
                        )
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_pickup_confirmed_photo)))
                    loadRequests()
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: context.getString(R.string.error_upload_photo_failed)
                    val uploadError = if (currentAttempts < MAX_UPLOAD_ATTEMPTS) {
                        "$errorMessage\n${context.getString(R.string.msg_attempt_n_of_n, currentAttempts, MAX_UPLOAD_ATTEMPTS)}"
                    } else {
                        "$errorMessage\n${context.getString(R.string.msg_max_retry_reached)}"
                    }
                    _state.update {
                        it.copy(
                            isUploading = false,
                            uploadError = uploadError,
                            failedUploadRequestId = requestId,
                            failedUploadPhotoUri = photoUri
                        )
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar(errorMessage))
                }
            )
        }
    }

    private fun retryFailedUpload() {
        val currentState = _state.value

        if (currentState.failedUploadRequestId != null &&
            currentState.failedUploadPhotoUri != null &&
            currentState.uploadAttempts < MAX_UPLOAD_ATTEMPTS
        ) {
            confirmPickupWithPhoto(
                currentState.failedUploadRequestId,
                currentState.failedUploadPhotoUri
            )
        } else {
            viewModelScope.launch {
                _uiEvent.send(
                    UiEvent.ShowSnackbar(context.getString(R.string.snack_cannot_retry))
                )
            }
        }
    }

    private fun dismissUploadError() {
        _state.update {
            it.copy(
                uploadError = null,
                uploadAttempts = 0,
                failedUploadRequestId = null,
                failedUploadPhotoUri = null
            )
        }
    }

    private fun exportCsv() {
        val current = _state.value
        val requests = current.filteredRequests
        if (requests.isEmpty()) {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_no_requests_export))) }
            return
        }
        val tag = current.selectedStatus?.lowercase() ?: "all"
        viewModelScope.launch {
            val message = DownloadsExport.exportCsv(
                context,
                filePrefix = "clearchain_requests_$tag",
                chooserTitle = "Share Requests CSV",
                header = listOf("Items", "Category", "Quantity", "Unit", "Status", "Grocery", "Pickup Date", "Pickup Time", "Created At", "Notes"),
                rows = requests.map { request ->
                    listOf(
                        request.itemTitles.joinToString("; "),
                        request.listingCategory,
                        request.requestedQuantity.toString(),
                        request.listingUnit,
                        request.status.name,
                        request.groceryName,
                        request.pickupDate.take(10),
                        request.pickupTime,
                        request.createdAt.take(10),
                        request.notes ?: ""
                    )
                }
            )
            _uiEvent.send(UiEvent.ShowSnackbar(message))
        }
    }
}
