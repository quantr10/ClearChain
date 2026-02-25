package com.clearchain.app.presentation.ngo.myrequests

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.pickuprequest.CancelPickupRequestUseCase
import com.clearchain.app.domain.usecase.pickuprequest.ConfirmPickupUseCase
import com.clearchain.app.domain.usecase.pickuprequest.GetMyPickupRequestsUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyRequestsViewModel @Inject constructor(
    private val getMyPickupRequestsUseCase: GetMyPickupRequestsUseCase,
    private val cancelPickupRequestUseCase: CancelPickupRequestUseCase,
    private val confirmPickupUseCase: ConfirmPickupUseCase
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

            // ✅ NEW: Retry mechanism
            MyRequestsEvent.RetryFailedUpload -> retryFailedUpload()
            MyRequestsEvent.DismissUploadError -> dismissUploadError()

            MyRequestsEvent.ClearError -> {
                _state.update { it.copy(error = null) }
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
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load requests"
                        )
                    }
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
                    _uiEvent.send(UiEvent.ShowSnackbar("Requests refreshed"))
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = error.message ?: "Failed to refresh requests"
                        )
                    }
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
                request.listingTitle.lowercase().contains(query) ||
                        request.groceryName.lowercase().contains(query) ||
                        request.notes?.lowercase()?.contains(query) == true
            }
        }

        current.selectedStatus?.let { status ->
            filtered = filtered.filter { it.status.name == status }
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
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = cancelPickupRequestUseCase(requestId)

            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar("Request cancelled successfully"))
                    loadRequests()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Failed to cancel request",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    // ✅ UPDATED: Photo upload with retry state
    private fun confirmPickupWithPhoto(requestId: String, photoUri: Uri) {
        viewModelScope.launch {
            val currentAttempts = _state.value.uploadAttempts + 1
            
            _state.update { 
                it.copy(
                    isUploading = true, 
                    uploadError = null,
                    uploadAttempts = currentAttempts
                ) 
            }

            val result = confirmPickupUseCase(requestId, photoUri)

            result.fold(
                onSuccess = {
                    _state.update { 
                        it.copy(
                            isUploading = false,
                            uploadAttempts = 0,
                            failedUploadRequestId = null,
                            failedUploadPhotoUri = null
                        ) 
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar("Pickup confirmed with photo!"))
                    loadRequests()
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: "Failed to upload photo"
                    
                    _state.update {
                        it.copy(
                            isUploading = false,
                            uploadError = if (currentAttempts < MAX_UPLOAD_ATTEMPTS) {
                                "$errorMessage\nAttempt $currentAttempts of $MAX_UPLOAD_ATTEMPTS"
                            } else {
                                "$errorMessage\nMaximum retry attempts reached"
                            },
                            failedUploadRequestId = requestId,
                            failedUploadPhotoUri = photoUri
                        )
                    }
                }
            )
        }
    }

    // ✅ NEW: Retry failed upload
    private fun retryFailedUpload() {
        val currentState = _state.value
        
        if (currentState.failedUploadRequestId != null && 
            currentState.failedUploadPhotoUri != null &&
            currentState.uploadAttempts < MAX_UPLOAD_ATTEMPTS) {
            
            confirmPickupWithPhoto(
                currentState.failedUploadRequestId,
                currentState.failedUploadPhotoUri
            )
        } else {
            viewModelScope.launch {
                _uiEvent.send(
                    UiEvent.ShowSnackbar("Cannot retry: Maximum attempts reached")
                )
            }
        }
    }

    // ✅ NEW: Dismiss upload error
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
}