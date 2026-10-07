package com.clearchain.app.presentation.ngo.cart

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.CartApi
import com.clearchain.app.data.remote.dto.CheckoutCartGroupRequest
import com.clearchain.app.data.remote.dto.UpdateCartItemRequest
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
            is CartEvent.PickupDateChanged -> onPickupDateChanged(event.value)
            is CartEvent.PickupTimeChanged -> updatePickupTime(event.value)
            is CartEvent.NotesChanged -> _state.update { it.copy(notes = event.value) }
            CartEvent.ToggleRefrigeration -> _state.update { it.copy(requiresRefrigeration = !it.requiresRefrigeration) }
            CartEvent.ToggleFragile -> _state.update { it.copy(isFragile = !it.isFragile) }
            CartEvent.ToggleHeavy -> _state.update { it.copy(isHeavy = !it.isHeavy) }
            CartEvent.SubmitCheckout -> submitCheckout()
        }
    }

    private fun loadCart() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { cartApi.getCart() }
                .onSuccess { response -> _state.update { it.copy(groups = response.data, isLoading = false) } }
                .onFailure { error ->
                    val msg = error.message ?: context.getString(R.string.error_generic)
                    _state.update { it.copy(isLoading = false, error = msg) }
                    // Data already on screen stays; the failure is reported, not hidden.
                    if (_state.value.groups.isNotEmpty()) _uiEvent.send(UiEvent.ShowSnackbar(msg))
                }
        }
    }

    private fun updateItem(itemId: String, quantity: Int) {
        if (_state.value.updatingItemId != null || _state.value.removingItemId != null) return
        viewModelScope.launch {
            _state.update {
                if (quantity == 0) it.copy(removingItemId = itemId) else it.copy(updatingItemId = itemId)
            }
            runCatching { cartApi.updateItem(itemId, UpdateCartItemRequest(quantity = quantity)) }
                .onSuccess { response -> _state.update { it.copy(groups = response.data) } }
                .onFailure { error -> _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: context.getString(R.string.error_update_cart_failed))) }
            _state.update { it.copy(updatingItemId = null, removingItemId = null) }
        }
    }

    private fun onPickupDateChanged(value: String) {
        val s = _state.value
        val group = s.groups.firstOrNull { it.groceryId == s.checkoutGroceryId }
        // A time picked for the old date may no longer be far enough in the future
        // (or in the store's hours) once the date itself changes - drop it rather
        // than silently keep an invalid selection.
        val keepTime = s.pickupTime.takeIf {
            it.isNotBlank() && isPickupTimeAllowed(value, it, group?.pickupTimeStart, group?.pickupTimeEnd) == null
        }.orEmpty()
        _state.update { it.copy(pickupDate = value, pickupTime = keepTime, pickupDateError = null, pickupTimeError = null) }
    }

    private fun updatePickupTime(value: String) {
        val s = _state.value
        val group = s.groups.firstOrNull { it.groceryId == s.checkoutGroceryId }
        val error = value.takeIf { it.isNotBlank() }
            ?.let { isPickupTimeAllowed(s.pickupDate, it, group?.pickupTimeStart, group?.pickupTimeEnd) }
        if (error != null) {
            _state.update { it.copy(pickupTimeError = context.getString(error)) }
            return
        }
        _state.update { it.copy(pickupTime = value, pickupTimeError = null) }
    }

    private fun submitCheckout() {
        val s = _state.value
        val groceryId = s.checkoutGroceryId ?: return
        val group = s.groups.firstOrNull { it.groceryId == groceryId }
        // Form problems are shown under the field they belong to.
        val dateError = when {
            s.pickupDate.isBlank() -> context.getString(R.string.error_pickup_date_required)
            !isPickupDateAllowed(s.pickupDate, group?.earliestExpiryDate) ->
                context.getString(R.string.error_pickup_date_after_expiry, group?.earliestExpiryDate.orEmpty())
            else -> null
        }
        val timeError = when {
            s.pickupTime.isBlank() -> context.getString(R.string.error_pickup_time_required)
            dateError != null -> null
            else -> isPickupTimeAllowed(s.pickupDate, s.pickupTime, group?.pickupTimeStart, group?.pickupTimeEnd)
                ?.let { context.getString(it) }
        }
        if (dateError != null || timeError != null) {
            _state.update { it.copy(pickupDateError = dateError, pickupTimeError = timeError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
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
                _state.update { it.copy(isSubmitting = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: context.getString(R.string.error_checkout_failed)))
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

    /** Returns the string-resource id of the reason [value] is not a valid pickup time, or null if it is valid. */
    private fun isPickupTimeAllowed(pickupDate: String, value: String, startValue: String?, endValue: String?): Int? {
        return runCatching {
            val pickupTime = LocalTime.parse(value.take(5))
            if (!startValue.isNullOrBlank() && !endValue.isNullOrBlank()) {
                val start = LocalTime.parse(startValue.take(5))
                val end = LocalTime.parse(endValue.take(5))
                if (pickupTime.isBefore(start) || pickupTime.isAfter(end)) {
                    return@runCatching R.string.error_pickup_time_outside_window
                }
            }
            val isToday = runCatching { LocalDate.parse(pickupDate.take(10)) == LocalDate.now() }.getOrDefault(false)
            if (isToday && pickupTime.isBefore(LocalTime.now().plusHours(2))) {
                return@runCatching R.string.error_pickup_time_too_soon
            }
            null
        }.getOrDefault(R.string.error_invalid_date_format)
    }
}
