package com.clearchain.app.presentation.ngo.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.CartApi
import com.clearchain.app.data.remote.dto.CheckoutCartGroupRequest
import com.clearchain.app.data.remote.dto.UpdateCartItemRequest
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartApi: CartApi,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(CartState(isLoading = true))
    val state: StateFlow<CartState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadCart()
    }

    fun onEvent(event: CartEvent) {
        when (event) {
            CartEvent.LoadCart -> loadCart()
            is CartEvent.SearchQueryChanged -> _state.update { it.copy(searchQuery = event.query) }
            is CartEvent.SortOptionChanged -> _state.update { it.copy(selectedSort = event.sort) }
            is CartEvent.IncrementItem -> updateItem(event.itemId, event.quantity + 1)
            is CartEvent.DecrementItem -> updateItem(event.itemId, event.quantity - 1)
            is CartEvent.RemoveItem -> updateItem(event.itemId, 0)
            is CartEvent.ShowCheckout -> _state.update {
                it.copy(
                    checkoutGroceryId = event.groceryId,
                    pickupDate = "",
                    pickupTime = "",
                    notes = "",
                    requiresRefrigeration = false,
                    isFragile = false,
                    isHeavy = false
                )
            }
            CartEvent.DismissCheckout -> _state.update { it.copy(checkoutGroceryId = null) }
            is CartEvent.PickupDateChanged -> _state.update { it.copy(pickupDate = event.value, error = null) }
            is CartEvent.PickupTimeChanged -> updatePickupTime(event.value)
            is CartEvent.NotesChanged -> _state.update { it.copy(notes = event.value) }
            CartEvent.ToggleRefrigeration -> _state.update { it.copy(requiresRefrigeration = !it.requiresRefrigeration) }
            CartEvent.ToggleFragile -> _state.update { it.copy(isFragile = !it.isFragile) }
            CartEvent.ToggleHeavy -> _state.update { it.copy(isHeavy = !it.isHeavy) }
            CartEvent.SubmitCheckout -> submitCheckout()
            CartEvent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadCart() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { cartApi.getCart() }
                .onSuccess { response -> _state.update { it.copy(groups = response.data, isLoading = false) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, error = error.message) } }
        }
    }

    private fun updateItem(itemId: String, quantity: Int) {
        viewModelScope.launch {
            runCatching { cartApi.updateItem(itemId, UpdateCartItemRequest(quantity = quantity)) }
                .onSuccess { response -> _state.update { it.copy(groups = response.data) } }
                .onFailure { error -> _state.update { it.copy(error = error.message) } }
        }
    }

    private fun updatePickupTime(value: String) {
        val s = _state.value
        val group = s.groups.firstOrNull { it.groceryId == s.checkoutGroceryId }
        if (value.isNotBlank() && !isPickupTimeAllowed(value, group?.pickupTimeStart, group?.pickupTimeEnd)) {
            _state.update { it.copy(error = context.getString(R.string.error_pickup_time_outside_window)) }
            return
        }
        _state.update { it.copy(pickupTime = value, error = null) }
    }

    private fun submitCheckout() {
        val s = _state.value
        val groceryId = s.checkoutGroceryId ?: return
        val group = s.groups.firstOrNull { it.groceryId == groceryId }
        if (s.pickupDate.isBlank() || s.pickupTime.isBlank()) {
            _state.update { it.copy(error = context.getString(R.string.cart_checkout_missing_fields)) }
            return
        }
        if (!isPickupDateAllowed(s.pickupDate, group?.earliestExpiryDate)) {
            _state.update { it.copy(error = context.getString(R.string.error_pickup_date_after_expiry, group?.earliestExpiryDate.orEmpty())) }
            return
        }
        if (!isPickupTimeAllowed(s.pickupTime, group?.pickupTimeStart, group?.pickupTimeEnd)) {
            _state.update { it.copy(error = context.getString(R.string.error_pickup_time_outside_window)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            runCatching {
                cartApi.checkout(
                    CheckoutCartGroupRequest(
                        groceryId = groceryId,
                        pickupDate = s.pickupDate,
                        pickupTime = s.pickupTime,
                        notes = s.notes.ifBlank { null },
                        requiresRefrigeration = s.requiresRefrigeration,
                        isFragile = s.isFragile,
                        isHeavy = s.isHeavy
                    )
                )
            }.onSuccess { response ->
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.cart_pickup_request_created)))
                _uiEvent.send(UiEvent.Navigate(Screen.RequestDetail.createRoute(response.data.id)))
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        checkoutGroceryId = null,
                        pickupDate = "",
                        pickupTime = "",
                        notes = ""
                    )
                }
                loadCart()
            }.onFailure { error ->
                _state.update { it.copy(isSubmitting = false, error = error.message) }
            }
        }
    }

    private fun isPickupDateAllowed(value: String, maxDate: String?): Boolean {
        return runCatching {
            val pickupDate = LocalDate.parse(value.take(10))
            val today = LocalDate.now()
            val expiryDate = maxDate?.let { LocalDate.parse(it.take(10)) }
            !pickupDate.isBefore(today) && (expiryDate == null || !pickupDate.isAfter(expiryDate))
        }.getOrDefault(false)
    }

    private fun isPickupTimeAllowed(value: String, startValue: String?, endValue: String?): Boolean {
        return runCatching {
            if (startValue.isNullOrBlank() || endValue.isNullOrBlank()) return@runCatching true
            val pickupTime = LocalTime.parse(value.take(5))
            val start = LocalTime.parse(startValue.take(5))
            val end = LocalTime.parse(endValue.take(5))
            !pickupTime.isBefore(start) && !pickupTime.isAfter(end)
        }.getOrDefault(false)
    }
}
