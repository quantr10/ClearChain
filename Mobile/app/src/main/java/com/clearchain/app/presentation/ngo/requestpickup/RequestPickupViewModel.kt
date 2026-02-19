package com.clearchain.app.presentation.ngo.requestpickup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.listing.GetListingByIdUseCase
import com.clearchain.app.domain.usecase.pickuprequest.CreatePickupRequestUseCase
import com.clearchain.app.util.Resource
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestPickupViewModel @Inject constructor(
    private val getListingByIdUseCase: GetListingByIdUseCase,
    private val createPickupRequestUseCase: CreatePickupRequestUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RequestPickupState())
    val state: StateFlow<RequestPickupState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEvent(event: RequestPickupEvent) {
        when (event) {
            is RequestPickupEvent.LoadListing -> loadListing(event.listingId)

            is RequestPickupEvent.QuantityChanged -> {
                _state.update { it.copy(quantity = event.quantity) }
            }

            is RequestPickupEvent.PickupDateChanged -> {
                _state.update { it.copy(pickupDate = event.date) }
            }

            is RequestPickupEvent.PickupTimeChanged -> {
                _state.update { it.copy(pickupTime = event.time) }
            }

            is RequestPickupEvent.NotesChanged -> {
                _state.update { it.copy(notes = event.notes) }
            }

            RequestPickupEvent.SubmitRequest -> submitRequest()

            RequestPickupEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun loadListing(listingId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getListingByIdUseCase(listingId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }

                    is Resource.Success -> {
                        _state.update {
                            it.copy(
                                listing = result.data,
                                quantity = result.data?.quantity?.toString() ?: "",
                                isLoading = false
                            )
                        }
                    }

                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                error = result.message ?: "Failed to load listing",
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun submitRequest() {
    val listing = _state.value.listing
    if (listing == null) {
        _state.update { it.copy(error = "Listing not found") }
        return
    }

    val quantity = _state.value.quantity.toIntOrNull()
    if (quantity == null || quantity <= 0) {
        _state.update { it.copy(error = "Please enter a valid quantity") }
        return
    }

    if (quantity > listing.quantity) {
        _state.update { it.copy(error = "Quantity exceeds available amount (${listing.quantity} ${listing.unit})") }
        return
    }

    if (_state.value.pickupDate.isBlank()) {
        _state.update { it.copy(error = "Please select a pickup date") }
        return
    }

    if (_state.value.pickupTime.isBlank()) {
        _state.update { it.copy(error = "Please select a pickup time") }
        return
    }

    // Validate pickup date
    try {
        val pickupDate = java.time.LocalDate.parse(_state.value.pickupDate)
        val today = java.time.LocalDate.now()
        val expiryDate = java.time.LocalDate.parse(listing.expiryDate)
        
        // Check if pickup date is in the past
        if (pickupDate.isBefore(today)) {
            _state.update { 
                it.copy(error = "Pickup date cannot be in the past") 
            }
            return
        }
        
        // Check if pickup date is after expiry date
        if (pickupDate.isAfter(expiryDate)) {
            _state.update { 
                it.copy(error = "Pickup date cannot be after expiry date (${listing.expiryDate})") 
            }
            return
        }
    } catch (e: Exception) {
        _state.update { it.copy(error = "Invalid date format") }
        return
    }

    viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }

        val result = createPickupRequestUseCase(
            listingId = listing.id,
            requestedQuantity = quantity,
            pickupDate = _state.value.pickupDate,
            pickupTime = _state.value.pickupTime,
            notes = _state.value.notes.ifBlank { null }
        )

        result.fold(
            onSuccess = {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                _uiEvent.send(UiEvent.ShowSnackbar("Pickup request submitted successfully!"))
                _uiEvent.send(UiEvent.NavigateUp)
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        error = error.message ?: "Failed to submit request",
                        isLoading = false
                    )
                }
            }
        )
    }
}
}