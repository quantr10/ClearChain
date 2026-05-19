package com.clearchain.app.presentation.ngo.requestpickup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.usecase.listing.GetListingByIdUseCase
import com.clearchain.app.domain.usecase.pickuprequest.CreatePickupRequestUseCase
import com.clearchain.app.util.Resource
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class RequestPickupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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

            is RequestPickupEvent.VehicleTypeChanged -> {
                _state.update { s ->
                    s.copy(
                        vehicleType = event.vehicleType,
                        estimatedTripMinutes = s.listing?.distanceKm?.let { km ->
                            computeEtaMinutes(km, event.vehicleType)
                        }
                    )
                }
            }
            RequestPickupEvent.ToggleRefrigeration ->
                _state.update { it.copy(needsRefrigeration = !it.needsRefrigeration) }
            RequestPickupEvent.ToggleFragile ->
                _state.update { it.copy(fragileItems = !it.fragileItems) }
            RequestPickupEvent.ToggleHeavyLoad ->
                _state.update { it.copy(heavyLoad = !it.heavyLoad) }

            is RequestPickupEvent.TimeSlotSelected -> {
                _state.update { it.copy(selectedTimeSlot = event.slot, pickupTime = event.slot) }
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
                        val listing = result.data
                        _state.update {
                            it.copy(
                                listing = listing,
                                quantity = listing?.quantity?.toString() ?: "",
                                isLoading = false,
                                availableTimeSlots = listing?.let { l -> generateTimeSlots(l) } ?: emptyList(),
                                estimatedTripMinutes = listing?.distanceKm?.let { km ->
                                    computeEtaMinutes(km, it.vehicleType)
                                }
                            )
                        }
                    }

                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                error = result.message ?: context.getString(R.string.error_failed_load_listing),
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun generateTimeSlots(listing: Listing): List<String> {
        return runCatching {
            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            val start = LocalTime.parse(listing.pickupTimeStart.take(5), fmt)
            val end   = LocalTime.parse(listing.pickupTimeEnd.take(5), fmt)
            val slots = mutableListOf<String>()
            var current = start
            while (!current.isAfter(end.minusMinutes(30))) {
                slots.add(current.format(fmt))
                current = current.plusMinutes(30)
            }
            slots
        }.getOrDefault(emptyList())
    }

    private fun computeEtaMinutes(distanceKm: Double, vehicleType: VehicleType): Int {
        val speedKmh = when (vehicleType) {
            VehicleType.WALK       -> 5.0
            VehicleType.BICYCLE    -> 15.0
            VehicleType.MOTORCYCLE -> 40.0
            VehicleType.CAR        -> 50.0
            VehicleType.VAN        -> 40.0
        }
        return ((distanceKm / speedKmh) * 60).toInt().coerceAtLeast(1)
    }

    private fun submitRequest() {
    val listing = _state.value.listing
    if (listing == null) {
        _state.update { it.copy(error = context.getString(R.string.error_listing_not_found)) }
        return
    }

    val quantity = _state.value.quantity.toIntOrNull()
    if (quantity == null || quantity <= 0) {
        _state.update { it.copy(error = context.getString(R.string.error_quantity_invalid)) }
        return
    }

    if (quantity > listing.quantity) {
        _state.update { it.copy(error = context.getString(R.string.error_quantity_exceeds, listing.quantity, listing.unit)) }
        return
    }

    if (_state.value.pickupDate.isBlank()) {
        _state.update { it.copy(error = context.getString(R.string.error_pickup_date_required)) }
        return
    }

    if (_state.value.pickupTime.isBlank()) {
        _state.update { it.copy(error = context.getString(R.string.error_pickup_time_required)) }
        return
    }

    // Validate pickup date
    try {
        val pickupDate = java.time.LocalDate.parse(_state.value.pickupDate)
        val today = java.time.LocalDate.now()
        val expiryDate = java.time.LocalDate.parse(listing.expiryDate)

        if (pickupDate.isBefore(today)) {
            _state.update { it.copy(error = context.getString(R.string.error_pickup_date_past)) }
            return
        }

        if (pickupDate.isAfter(expiryDate)) {
            _state.update { it.copy(error = context.getString(R.string.error_pickup_date_after_expiry, listing.expiryDate)) }
            return
        }
    } catch (e: Exception) {
        _state.update { it.copy(error = context.getString(R.string.error_invalid_date_format)) }
        return
    }

    viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }

        val s = _state.value
        val result = createPickupRequestUseCase(
            listingId = listing.id,
            requestedQuantity = quantity,
            pickupDate = s.pickupDate,
            pickupTime = s.pickupTime,
            notes = s.notes.ifBlank { null },
            vehicleType = s.vehicleType.name.lowercase(),
            requiresRefrigeration = s.needsRefrigeration,
            isFragile = s.fragileItems,
            isHeavy = s.heavyLoad
        )

        result.fold(
            onSuccess = {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_request_submitted)))
                _uiEvent.send(UiEvent.NavigateUp)
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        error = error.message ?: context.getString(R.string.error_submit_request_failed),
                        isLoading = false
                    )
                }
            }
        )
    }
}
}