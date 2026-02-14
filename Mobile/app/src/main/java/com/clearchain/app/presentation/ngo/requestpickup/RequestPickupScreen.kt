package com.clearchain.app.presentation.ngo.requestpickup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestPickupScreen(
    listingId: String,
    onNavigateBack: () -> Unit,
    viewModel: RequestPickupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(listingId) {
        viewModel.onEvent(RequestPickupEvent.LoadListing(listingId))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is UiEvent.NavigateUp -> {
                    onNavigateBack()
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Pickup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.listing == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.listing != null -> {
                    RequestPickupContent(
                        listing = state.listing!!,
                        quantity = state.quantity,
                        pickupDate = state.pickupDate,
                        pickupTime = state.pickupTime,
                        notes = state.notes,
                        isLoading = state.isLoading,
                        error = state.error,
                        onQuantityChange = {
                            viewModel.onEvent(RequestPickupEvent.QuantityChanged(it))
                        },
                        onPickupDateChange = {
                            viewModel.onEvent(RequestPickupEvent.PickupDateChanged(it))
                        },
                        onPickupTimeChange = {
                            viewModel.onEvent(RequestPickupEvent.PickupTimeChanged(it))
                        },
                        onNotesChange = {
                            viewModel.onEvent(RequestPickupEvent.NotesChanged(it))
                        },
                        onSubmit = {
                            viewModel.onEvent(RequestPickupEvent.SubmitRequest)
                        },
                        onClearError = {
                            viewModel.onEvent(RequestPickupEvent.ClearError)
                        }
                    )
                }

                state.error != null && state.listing == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.error ?: "Failed to load listing",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestPickupContent(
    listing: Listing,
    quantity: String,
    pickupDate: String,
    pickupTime: String,
    notes: String,
    isLoading: Boolean,
    error: String?,
    onQuantityChange: (String) -> Unit,
    onPickupDateChange: (String) -> Unit,
    onPickupTimeChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Listing Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = listing.groceryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = listing.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Available:", fontWeight = FontWeight.Medium)
                    Text(
                        text = "${listing.quantity} ${listing.unit}",
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Expiry Date:", fontWeight = FontWeight.Medium)
                    Text(
                        text = listing.expiryDate,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Error message
        error?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearError) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Request Form
        Text(
            text = "Pickup Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Quantity
        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Quantity") },
            leadingIcon = { Icon(Icons.Default.ShoppingCart, null) },
            supportingText = { Text("Max: ${listing.quantity} ${listing.unit}") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        // Pickup Date
        OutlinedTextField(
            value = pickupDate,
            onValueChange = onPickupDateChange,
            label = { Text("Pickup Date") },
            leadingIcon = { Icon(Icons.Default.DateRange, null) },
            placeholder = { Text("YYYY-MM-DD") },
            supportingText = { Text("Format: YYYY-MM-DD (e.g., 2026-02-20)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        // Pickup Time
        OutlinedTextField(
            value = pickupTime,
            onValueChange = onPickupTimeChange,
            label = { Text("Pickup Time") },
            leadingIcon = { Icon(Icons.Default.Schedule, null) },
            placeholder = { Text("HH:MM") },
            supportingText = { Text("Format: HH:MM (e.g., 14:30)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes (Optional)") },
            leadingIcon = { Icon(Icons.Default.Edit, null) },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Submit Button
        Button(
            onClick = onSubmit,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submitting...")
            } else {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Request")
            }
        }
    }
}