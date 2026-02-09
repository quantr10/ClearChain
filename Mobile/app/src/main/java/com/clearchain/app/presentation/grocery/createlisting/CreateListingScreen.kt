package com.clearchain.app.presentation.grocery.createlisting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.displayName
import com.clearchain.app.presentation.components.ClearChainButton
import com.clearchain.app.presentation.components.ClearChainTextField
import com.clearchain.app.util.UiEvent
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    navController: NavController,
    viewModel: CreateListingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.NavigateUp -> {
                    navController.navigateUp()
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Listing") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "List your surplus food items for NGOs to collect",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            ClearChainTextField(
                value = state.title,
                onValueChange = { viewModel.onEvent(CreateListingEvent.TitleChanged(it)) },
                label = "Title",
                placeholder = "e.g., Fresh Apples",
                leadingIcon = { Icon(Icons.Default.ShoppingCart, null) },
                imeAction = ImeAction.Next,
                isError = state.titleError != null,
                errorMessage = state.titleError,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            ClearChainTextField(
                value = state.description,
                onValueChange = { viewModel.onEvent(CreateListingEvent.DescriptionChanged(it)) },
                label = "Description",
                placeholder = "Describe the food items...",
                leadingIcon = { Icon(Icons.Default.Description, null) },
                imeAction = ImeAction.Next,
                isError = state.descriptionError != null,
                errorMessage = state.descriptionError,
                enabled = !state.isLoading,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = state.showCategoryDropdown,
                onExpandedChange = {
                    if (!state.isLoading) {
                        viewModel.onEvent(CreateListingEvent.ToggleCategoryDropdown)
                    }
                }
            ) {
                OutlinedTextField(
                    value = FoodCategory.valueOf(state.category).displayName(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.showCategoryDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = !state.isLoading,
                    colors = OutlinedTextFieldDefaults.colors()
                )

                ExposedDropdownMenu(
                    expanded = state.showCategoryDropdown,
                    onDismissRequest = { viewModel.onEvent(CreateListingEvent.ToggleCategoryDropdown) }
                ) {
                    FoodCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName()) },
                            onClick = {
                                viewModel.onEvent(CreateListingEvent.CategoryChanged(category.name))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity and Unit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quantity
                ClearChainTextField(
                    value = state.quantity,
                    onValueChange = { viewModel.onEvent(CreateListingEvent.QuantityChanged(it)) },
                    label = "Quantity",
                    placeholder = "10",
                    leadingIcon = { Icon(Icons.Default.Numbers, null) },
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                    isError = state.quantityError != null,
                    errorMessage = state.quantityError,
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f)
                )

                // Unit Dropdown
                ExposedDropdownMenuBox(
                    expanded = state.showUnitDropdown,
                    onExpandedChange = {
                        if (!state.isLoading) {
                            viewModel.onEvent(CreateListingEvent.ToggleUnitDropdown)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.showUnitDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !state.isLoading,
                        isError = state.unitError != null,
                        supportingText = if (state.unitError != null) {
                            { Text(state.unitError!!) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    ExposedDropdownMenu(
                        expanded = state.showUnitDropdown,
                        onDismissRequest = { viewModel.onEvent(CreateListingEvent.ToggleUnitDropdown) }
                    ) {
                        listOf("kg", "g", "L", "mL", "pieces", "boxes", "bags").forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit) },
                                onClick = {
                                    viewModel.onEvent(CreateListingEvent.UnitChanged(unit))
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expiry Date
            ClearChainTextField(
                value = state.expiryDate,
                onValueChange = { viewModel.onEvent(CreateListingEvent.ExpiryDateChanged(it)) },
                label = "Expiry Date",
                placeholder = "YYYY-MM-DD",
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                imeAction = ImeAction.Next,
                isError = state.expiryDateError != null,
                errorMessage = state.expiryDateError,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pickup Time Start
            ClearChainTextField(
                value = state.pickupTimeStart,
                onValueChange = { viewModel.onEvent(CreateListingEvent.PickupTimeStartChanged(it)) },
                label = "Pickup Start Time",
                placeholder = "HH:MM (e.g., 09:00)",
                leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                imeAction = ImeAction.Next,
                isError = state.pickupTimeStartError != null,
                errorMessage = state.pickupTimeStartError,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pickup Time End
            ClearChainTextField(
                value = state.pickupTimeEnd,
                onValueChange = { viewModel.onEvent(CreateListingEvent.PickupTimeEndChanged(it)) },
                label = "Pickup End Time",
                placeholder = "HH:MM (e.g., 17:00)",
                leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                imeAction = ImeAction.Next,
                isError = state.pickupTimeEndError != null,
                errorMessage = state.pickupTimeEndError,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Image URL (Optional)
            ClearChainTextField(
                value = state.imageUrl,
                onValueChange = { viewModel.onEvent(CreateListingEvent.ImageUrlChanged(it)) },
                label = "Image URL (Optional)",
                placeholder = "https://example.com/image.jpg",
                leadingIcon = { Icon(Icons.Default.Image, null) },
                imeAction = ImeAction.Done,
                onImeAction = { viewModel.onEvent(CreateListingEvent.CreateListing) },
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Create Button
            ClearChainButton(
                text = "Create Listing",
                onClick = { viewModel.onEvent(CreateListingEvent.CreateListing) },
                loading = state.isLoading,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            if (state.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = state.error!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}