package com.clearchain.app.presentation.ngo.requestpickup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RequestPickupScreen(
    listingId: String,
    onNavigateBack: () -> Unit,
    viewModel: RequestPickupViewModel = hiltViewModel()
) {
    val state            by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(listingId) {
        viewModel.onEvent(RequestPickupEvent.LoadListing(listingId))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                is UiEvent.NavigateUp   -> onNavigateBack()
                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.listing == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null && state.listing == null -> {
                    ErrorState(
                        message = state.error ?: stringResource(R.string.error_failed_load_listing),
                        onRetry = onNavigateBack
                    )
                }

                state.listing != null -> {
                    RequestPickupContent(
                        listing = state.listing!!,
                        state   = state,
                        onEvent = { viewModel.onEvent(it) }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RequestPickupContent(
    listing: Listing,
    state: RequestPickupState,
    onEvent: (RequestPickupEvent) -> Unit
) {
    val isLoading = state.isLoading
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Listing summary card ───────────────────────────────────────
        InfoCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            Text(
                text       = listing.title,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            InfoRow(Icons.Default.Store, stringResource(R.string.label_grocery_store), listing.groceryName)
            InfoRow(Icons.Default.Place, stringResource(R.string.label_distance), listing.location)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column {
                    Text(
                        stringResource(R.string.label_available),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${listing.quantity} ${listing.unit}",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        stringResource(R.string.label_expiry_field),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        listing.expiryDate,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // ── Error banner ───────────────────────────────────────────────
        AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut()) {
            AlertBanner(
                message = state.error ?: "",
                type    = AlertType.ERROR,
                icon    = Icons.Default.ErrorOutline
            )
        }

        // ── Pickup details form ────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                stringResource(R.string.label_pickup_details_section),
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ClearChainTextField(
                value         = state.quantity,
                onValueChange = { onEvent(RequestPickupEvent.QuantityChanged(it)) },
                label         = stringResource(R.string.listing_quantity),
                placeholder   = stringResource(R.string.hint_max_quantity, listing.quantity.toString(), listing.unit),
                leadingIcon   = { Icon(Icons.Default.ShoppingCart, null) },
                keyboardType  = KeyboardType.Number,
                imeAction     = ImeAction.Next,
                enabled       = !isLoading
            )

            Text(
                stringResource(R.string.label_pickup_date_section),
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val expiryMaxDate = remember(listing.expiryDate) {
                runCatching { LocalDate.parse(listing.expiryDate.take(10)) }.getOrNull()
            }
            PickupCalendarPicker(
                selectedDate    = state.pickupDate,
                onDateSelected  = { onEvent(RequestPickupEvent.PickupDateChanged(it)) },
                maxDate         = expiryMaxDate,
                enabled         = !isLoading
            )

            // ── Time slot picker (predefined slots from listing window) ──
            if (state.availableTimeSlots.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.label_pickup_time_window, listing.pickupTimeStart.take(5), listing.pickupTimeEnd.take(5)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.availableTimeSlots) { slot ->
                            FilterChip(
                                selected = state.selectedTimeSlot == slot,
                                onClick  = { onEvent(RequestPickupEvent.TimeSlotSelected(slot)) },
                                label    = { Text(slot) },
                                enabled  = !isLoading,
                                leadingIcon = if (state.selectedTimeSlot == slot) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                    if (state.selectedTimeSlot == null) {
                        Text(
                            stringResource(R.string.hint_select_time_slot),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                TimePickerField(
                    value          = state.pickupTime,
                    onTimeSelected = { onEvent(RequestPickupEvent.PickupTimeChanged(it)) },
                    label          = stringResource(R.string.label_pickup_time_field),
                    enabled        = !isLoading
                )
            }
        }

        // ── Estimated trip info ────────────────────────────────────────
        state.estimatedTripMinutes?.let { eta ->
            Surface(
                shape  = RoundedCornerShape(12.dp),
                color  = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DirectionsCar, null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column {
                        Text(
                            stringResource(R.string.label_estimated_trip, eta),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        listing.distanceKm?.let { dist ->
                            val vehicleLabel = stringResource(state.vehicleType.labelResId)
                            Text(
                                stringResource(R.string.label_distance_via, dist.toString(), vehicleLabel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // ── Vehicle type selector ──────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.label_transport_method),
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                VehicleType.entries.forEach { vehicle ->
                    FilterChip(
                        selected = state.vehicleType == vehicle,
                        onClick  = { onEvent(RequestPickupEvent.VehicleTypeChanged(vehicle)) },
                        label    = { Text("${vehicle.icon} ${stringResource(vehicle.labelResId)}") },
                        enabled  = !isLoading
                    )
                }
            }
        }

        // ── Special handling checkboxes ────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.label_special_handling),
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SpecialHandlingRow(
                icon     = Icons.Default.AcUnit,
                label    = stringResource(R.string.label_needs_refrigeration),
                checked  = state.needsRefrigeration,
                enabled  = !isLoading,
                onToggle = { onEvent(RequestPickupEvent.ToggleRefrigeration) }
            )
            SpecialHandlingRow(
                icon     = Icons.Default.Warning,
                label    = stringResource(R.string.fragile_items),
                checked  = state.fragileItems,
                enabled  = !isLoading,
                onToggle = { onEvent(RequestPickupEvent.ToggleFragile) }
            )
            SpecialHandlingRow(
                icon     = Icons.Default.FitnessCenter,
                label    = stringResource(R.string.label_heavy_load),
                checked  = state.heavyLoad,
                enabled  = !isLoading,
                onToggle = { onEvent(RequestPickupEvent.ToggleHeavyLoad) }
            )
        }

        // ── Notes ──────────────────────────────────────────────────────
        ClearChainTextField(
            value         = state.notes,
            onValueChange = { onEvent(RequestPickupEvent.NotesChanged(it)) },
            label         = stringResource(R.string.label_notes_optional),
            placeholder   = stringResource(R.string.hint_additional_instructions),
            leadingIcon   = { Icon(Icons.Default.Edit, null) },
            imeAction     = ImeAction.Done,
            enabled       = !isLoading,
            singleLine    = false,
            minLines      = 3,
            maxLines      = 5
        )

        ClearChainButton(
            text     = stringResource(R.string.action_submit_request),
            onClick  = { onEvent(RequestPickupEvent.SubmitRequest) },
            loading  = isLoading,
            enabled  = !isLoading,
            icon     = Icons.Default.Send,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SpecialHandlingRow(
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    label:    String,
    checked:  Boolean,
    enabled:  Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
    }
}
