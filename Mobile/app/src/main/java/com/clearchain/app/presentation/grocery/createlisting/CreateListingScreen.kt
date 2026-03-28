package com.clearchain.app.presentation.grocery.createlisting

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.displayName
import com.clearchain.app.presentation.components.ClearChainButton
import com.clearchain.app.presentation.components.ClearChainTextField
import com.clearchain.app.presentation.components.PhotoPickerDialog
import com.clearchain.app.presentation.components.DatePickerField
import com.clearchain.app.presentation.components.TimePickerField
import com.clearchain.app.util.UiEvent

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

            // ========================================
            // 🤖 AI IMAGE ANALYSIS SECTION (NEW)
            // ========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "AI Food Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        if (state.selectedImageUri != null) {
                            IconButton(
                                onClick = { viewModel.onEvent(CreateListingEvent.ClearImage) }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "Remove image",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Image Preview or Picker Button
                    if (state.selectedImageUri != null) {
                        // Image Preview
                        AsyncImage(
                            model = state.selectedImageUri,
                            contentDescription = "Selected food image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Analysis Status
                        when {
                            state.isAnalyzing -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "AI is analyzing your food...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            
                            state.analysisResult != null -> {
                                val result = state.analysisResult!!
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Detection Result
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Detected: ${result.title}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                        
                                        // Details
                                        Text(
                                            text = "Category: ${result.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "Quality: ${result.qualityGrade}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Text(
                                                text = "Freshness: ${result.freshnessScore}/100",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Text(
                                                text = "Confidence: ${(result.confidence * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Apply Button
                                        Button(
                                            onClick = { 
                                                viewModel.onEvent(CreateListingEvent.ApplyAISuggestions) 
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Apply AI Suggestions")
                                        }
                                    }
                                }
                            }
                            
                            state.analysisError != null -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = state.analysisError!!,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Pick Image Button
                        OutlinedButton(
                            onClick = { viewModel.onEvent(CreateListingEvent.ToggleImagePicker) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Take Photo or Choose Image")
                        }
                        
                        Text(
                            text = "Let AI analyze your food and auto-fill details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // ========================================
            // EXISTING FIELDS
            // ========================================

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
                enabled = !state.isLoading && !state.isAnalyzing
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
                enabled = !state.isLoading && !state.isAnalyzing,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = state.showCategoryDropdown,
                onExpandedChange = {
                    if (!state.isLoading && !state.isAnalyzing) {
                        viewModel.onEvent(CreateListingEvent.ToggleCategoryDropdown)
                    }
                }
            ) {
                OutlinedTextField(
                    value = FoodCategory.valueOf(state.category).displayName(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = state.showCategoryDropdown
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = !state.isLoading && !state.isAnalyzing,
                    colors = OutlinedTextFieldDefaults.colors()
                )

                ExposedDropdownMenu(
                    expanded = state.showCategoryDropdown,
                    onDismissRequest = { 
                        viewModel.onEvent(CreateListingEvent.ToggleCategoryDropdown) 
                    }
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
                    enabled = !state.isLoading && !state.isAnalyzing,
                    modifier = Modifier.weight(1f)
                )

                // Unit Dropdown
                ExposedDropdownMenuBox(
                    expanded = state.showUnitDropdown,
                    onExpandedChange = {
                        if (!state.isLoading && !state.isAnalyzing) {
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
                        trailingIcon = { 
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = state.showUnitDropdown
                            ) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !state.isLoading && !state.isAnalyzing,
                        isError = state.unitError != null,
                        supportingText = if (state.unitError != null) {
                            { Text(state.unitError!!) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    ExposedDropdownMenu(
                        expanded = state.showUnitDropdown,
                        onDismissRequest = { 
                            viewModel.onEvent(CreateListingEvent.ToggleUnitDropdown) 
                        }
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
            DatePickerField(
                value = state.expiryDate,
                onDateSelected = { viewModel.onEvent(CreateListingEvent.ExpiryDateChanged(it)) },
                label = "Expiry Date",
                isError = state.expiryDateError != null,
                errorMessage = state.expiryDateError,
                enabled = !state.isLoading && !state.isAnalyzing
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pickup Time Start
            TimePickerField(
                value = state.pickupTimeStart,
                onTimeSelected = { viewModel.onEvent(CreateListingEvent.PickupTimeStartChanged(it)) },
                label = "Pickup Start Time",
                enabled = !state.isLoading && !state.isAnalyzing
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pickup Time End
            TimePickerField(
                value = state.pickupTimeEnd,
                onTimeSelected = { viewModel.onEvent(CreateListingEvent.PickupTimeEndChanged(it)) },
                label = "Pickup End Time",
                enabled = !state.isLoading && !state.isAnalyzing
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Create Button
            ClearChainButton(
                text = "Create Listing",
                onClick = { viewModel.onEvent(CreateListingEvent.CreateListing) },
                loading = state.isLoading,
                enabled = !state.isLoading && !state.isAnalyzing
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
    
    // ========================================
    // PHOTO PICKER DIALOG
    // ========================================
    if (state.showImagePicker) {
        PhotoPickerDialog(
            onPhotoSelected = { uri ->
                viewModel.onEvent(CreateListingEvent.ImageSelected(uri))
            },
            onDismiss = { 
                viewModel.onEvent(CreateListingEvent.ToggleImagePicker) 
            }
        )
    }
}