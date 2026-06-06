package com.clearchain.app.presentation.grocery.createlisting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.ListingStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.ShapeMedium
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    navController: NavController,
    viewModel: CreateListingViewModel = hiltViewModel()
) {
    val state             by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.NavigateUp   -> navController.navigateUp()
                else -> Unit
            }
        }
    }

    BackHandler(state.isPreviewMode) { viewModel.onEvent(CreateListingEvent.TogglePreview) }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.create_listing),
                onNavigateBack = {
                    if (state.isPreviewMode) viewModel.onEvent(CreateListingEvent.TogglePreview)
                    else navController.navigateUp()
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(CreateListingEvent.TogglePreview) },
                containerColor = if (state.isPreviewMode)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    if (state.isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                    contentDescription = if (state.isPreviewMode)
                        stringResource(R.string.cd_edit)
                    else
                        stringResource(R.string.cd_preview)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Draft saved chip ───────────────────────────────────────────────
            state.draftSavedAt?.let { savedAt ->
                Surface(
                    color  = MaterialTheme.colorScheme.surface,
                    shape  = RoundedCornerShape(50),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Save, null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(
                                R.string.create_listing_draft_saved,
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(savedAt))
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.isPreviewMode) {
                // ── Preview Mode ───────────────────────────────────────────────
                val previewListing = remember(
                    state.title, state.description, state.category,
                    state.quantity, state.unit, state.expiryDate,
                    state.selectedImages, state.imageUrl, state.groceryHours
                ) {
                    Listing(
                        id              = "",
                        groceryId       = "",
                        groceryName     = "",
                        title           = state.title.ifBlank { "—" },
                        description     = state.description,
                        category        = runCatching { FoodCategory.valueOf(state.category) }
                            .getOrDefault(FoodCategory.OTHER),
                        quantity        = state.quantity.toIntOrNull() ?: 0,
                        unit            = state.unit,
                        expiryDate      = state.expiryDate,
                        pickupTimeStart = "",
                        pickupTimeEnd   = "",
                        status          = ListingStatus.AVAILABLE,
                        imageUrl        = state.selectedImages.firstOrNull()?.toString()
                            ?: state.imageUrl.ifBlank { null },
                        imageUrls       = state.selectedImages.map { it.toString() },
                        location        = "",
                        createdAt       = "",
                        groceryHours    = state.groceryHours
                    )
                }
                ListingCard(listing = previewListing)
                Spacer(Modifier.height(4.dp))
                ClearChainButton(
                    text     = stringResource(R.string.btn_create_listing),
                    onClick  = { viewModel.onEvent(CreateListingEvent.CreateListing) },
                    loading  = state.isLoading,
                    enabled  = !state.isLoading && !state.isAnalyzing,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val titleInvalid  = state.title.isBlank() || state.title.length < 3
                val descInvalid   = state.description.isBlank()
                val qtyInvalid    = (state.quantity.toIntOrNull() ?: 0) <= 0
                val expiryInvalid = state.expiryDate.isBlank()
                val canCreate = !titleInvalid && !descInvalid && !qtyInvalid && !expiryInvalid

                // ── AI Photo Analysis ──────────────────────────────────────────
                AiAnalysisCard(state = state, viewModel = viewModel)

                // ── Title ─────────────────────────────────────────────────────
                FieldCard(label = stringResource(R.string.label_title)) {
                    ClearChainTextField(
                        value         = state.title,
                        onValueChange = { viewModel.onEvent(CreateListingEvent.TitleChanged(it)) },
                        placeholder   = stringResource(R.string.label_title_placeholder),
                        leadingIcon   = Icons.Default.ShoppingCart,
                        imeAction     = ImeAction.Next,
                        isError       = state.titleError != null,
                        errorMessage  = state.titleError,
                        enabled       = !state.isLoading && !state.isAnalyzing
                    )
                }

                // ── Description ────────────────────────────────────────────────
                FieldCard(label = stringResource(R.string.label_description)) {
                    ClearChainTextField(
                        value         = state.description,
                        onValueChange = { viewModel.onEvent(CreateListingEvent.DescriptionChanged(it)) },
                        placeholder   = stringResource(R.string.label_description_placeholder),
                        leadingIcon   = Icons.Default.Description,
                        imeAction     = ImeAction.Next,
                        isError       = state.descriptionError != null,
                        errorMessage  = state.descriptionError,
                        enabled       = !state.isLoading && !state.isAnalyzing,
                        singleLine    = false,
                        minLines      = 2,
                        maxLines      = 4
                    )
                }

                // ── Category ───────────────────────────────────────────────────
                FieldCard(label = stringResource(R.string.label_category)) {
                    val enabled   = !state.isLoading && !state.isAnalyzing
                    val iconTint  = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ExposedDropdownMenuBox(
                        expanded        = state.showCategoryDropdown,
                        onExpandedChange = { if (enabled) viewModel.onEvent(CreateListingEvent.ToggleCategoryDropdown) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
                                .menuAnchor()
                                .padding(horizontal = 12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Category, null, Modifier.size(18.dp), tint = iconTint)
                            Text(
                                stringResource(FoodCategory.valueOf(state.category).labelResId),
                                style    = MaterialTheme.typography.labelLarge,
                                color    = textColor,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (state.showCategoryDropdown) Icons.Default.ArrowDropUp
                                else Icons.Default.ArrowDropDown,
                                null, Modifier.size(18.dp), tint = iconTint
                            )
                        }
                        ExposedDropdownMenu(
                            expanded         = state.showCategoryDropdown,
                            onDismissRequest = { viewModel.onEvent(CreateListingEvent.ToggleCategoryDropdown) }
                        ) {
                            FoodCategory.entries.forEach { category ->
                                DropdownMenuItem(
                                    text    = { Text(stringResource(category.labelResId)) },
                                    onClick = { viewModel.onEvent(CreateListingEvent.CategoryChanged(category.name)) }
                                )
                            }
                        }
                    }
                }

                // ── Quantity ───────────────────────────────────────────────────
                FieldCard(label = stringResource(R.string.label_quantity_short)) {
                    val enabled   = !state.isLoading && !state.isAnalyzing
                    val iconTint  = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClearChainTextField(
                            value         = state.quantity,
                            onValueChange = { viewModel.onEvent(CreateListingEvent.QuantityChanged(it)) },
                            placeholder   = stringResource(R.string.hint_quantity_number),
                            leadingIcon   = Icons.Default.Numbers,
                            keyboardType  = KeyboardType.Number,
                            imeAction     = ImeAction.Next,
                            isError       = state.quantityError != null,
                            errorMessage  = state.quantityError,
                            enabled       = enabled,
                            modifier      = Modifier.weight(1f)
                        )
                        ExposedDropdownMenuBox(
                            expanded        = state.showUnitDropdown,
                            onExpandedChange = { if (enabled) viewModel.onEvent(CreateListingEvent.ToggleUnitDropdown) },
                            modifier        = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .border(
                                        1.dp,
                                        if (state.unitError != null) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        ShapeMedium
                                    )
                                    .menuAnchor()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    state.unit,
                                    style    = MaterialTheme.typography.labelLarge,
                                    color    = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (state.showUnitDropdown) Icons.Default.ArrowDropUp
                                    else Icons.Default.ArrowDropDown,
                                    null, Modifier.size(18.dp), tint = iconTint
                                )
                            }
                            ExposedDropdownMenu(
                                expanded         = state.showUnitDropdown,
                                onDismissRequest = { viewModel.onEvent(CreateListingEvent.ToggleUnitDropdown) }
                            ) {
                                listOf("kg", "g", "L", "mL", "pieces", "boxes", "bags").forEach { unit ->
                                    DropdownMenuItem(
                                        text    = { Text(unit) },
                                        onClick = { viewModel.onEvent(CreateListingEvent.UnitChanged(unit)) }
                                    )
                                }
                            }
                        }
                    }
                    if (state.unitError != null) {
                        Text(state.unitError!!, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp))
                    }
                }

                // ── Expiry Date ────────────────────────────────────────────────
                val futureDates = remember {
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            val date = java.time.Instant.ofEpochMilli(utcTimeMillis)
                                .atZone(java.time.ZoneId.of("UTC")).toLocalDate()
                            return !date.isBefore(java.time.LocalDate.now())
                        }
                    }
                }
                FieldCard(label = stringResource(R.string.label_expiry_date)) {
                    DatePickerField(
                        value           = state.expiryDate,
                        onDateSelected  = { viewModel.onEvent(CreateListingEvent.ExpiryDateChanged(it)) },
                        isError         = state.expiryDateError != null,
                        errorMessage    = state.expiryDateError,
                        enabled         = !state.isLoading && !state.isAnalyzing,
                        selectableDates = futureDates,
                        onClearDate     = { viewModel.onEvent(CreateListingEvent.ExpiryDateChanged("")) }
                    )
                }

                // ── Pickup Hours ───────────────────────────────────────────────
                FieldCard(label = stringResource(R.string.label_pickup_hours_from_profile)) {
                    Surface(
                        color    = MaterialTheme.colorScheme.secondaryContainer,
                        shape    = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier              = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text  = state.groceryHours
                                    ?: stringResource(R.string.label_pickup_hours_not_set),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
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

                // ── Submit ─────────────────────────────────────────────────────
                ClearChainButton(
                    text     = stringResource(R.string.btn_create_listing),
                    onClick  = { viewModel.onEvent(CreateListingEvent.CreateListing) },
                    loading  = state.isLoading,
                    enabled  = canCreate && !state.isLoading && !state.isAnalyzing,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (state.showImagePicker) {
        PhotoPickerDialog(
            onPhotoSelected = { uri -> viewModel.onEvent(CreateListingEvent.AddImage(uri)) },
            onDismiss       = { viewModel.onEvent(CreateListingEvent.ToggleImagePicker) }
        )
    }
}

// ── Individual field card ──────────────────────────────────────────────────────
@Composable
private fun FieldCard(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

// ── AI Analysis Card ───────────────────────────────────────────────────────────
@Composable
private fun AiAnalysisCard(state: CreateListingState, viewModel: CreateListingViewModel) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        stringResource(R.string.label_ai_food_analysis),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (state.selectedImageUri != null) {
                    IconButton(
                        onClick  = { viewModel.onEvent(CreateListingEvent.ClearImage) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, null, Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (state.selectedImageUri != null) {
                // Image preview
                AsyncImage(
                    model              = state.selectedImageUri,
                    contentDescription = null,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )

                when {
                    state.isAnalyzing -> {
                        Column(
                            modifier            = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    stringResource(R.string.label_analyzing_food),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    state.analysisResult != null -> {
                        val result = state.analysisResult!!

                        // Detected title
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Text(
                                result.title,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Stat chips: Confidence | Quality | Freshness
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AiStatChip(
                                value    = "${(result.confidence * 100).toInt()}%",
                                label    = stringResource(R.string.label_stat_confidence),
                                modifier = Modifier.weight(1f)
                            )
                            AiStatChip(
                                value    = result.qualityGrade,
                                label    = stringResource(R.string.label_stat_quality),
                                modifier = Modifier.weight(1f)
                            )
                            AiStatChip(
                                value    = "${result.freshnessScore}/100",
                                label    = stringResource(R.string.label_stat_freshness),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Detail rows
                        AiDetailRow(Icons.Default.Category,
                            stringResource(R.string.label_category),
                            result.category.lowercase().replaceFirstChar { it.titlecase() })
                        AiDetailRow(Icons.Default.CalendarToday,
                            stringResource(R.string.label_expiry_date),
                            result.expiryDate.take(10))
                        if (result.notes.isNotBlank()) {
                            AiDetailRow(Icons.Default.Notes,
                                stringResource(R.string.label_notes),
                                result.notes)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Two buttons
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick  = { viewModel.onEvent(CreateListingEvent.DismissAnalysis) },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(R.string.action_enter_manually))
                            }
                            Button(
                                onClick  = { viewModel.onEvent(CreateListingEvent.ApplyAISuggestions) },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.action_apply_ai))
                            }
                        }
                    }

                    state.analysisError != null -> {
                        AlertBanner(
                            message = state.analysisError!!,
                            type    = AlertType.ERROR,
                            icon    = Icons.Default.ErrorOutline
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick  = { viewModel.onEvent(CreateListingEvent.ToggleImagePicker) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_take_photo))
                }
                Text(
                    stringResource(R.string.label_ai_analyze_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AiStatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        color    = MaterialTheme.colorScheme.surfaceVariant,
        shape    = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AiDetailRow(
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp).padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f))
    }
}

