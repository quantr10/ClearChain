package com.clearchain.app.presentation.grocery.createlisting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

                // ── AI Photo Analysis ──────────────────────────────────────────
                AiAnalysisCard(state = state, viewModel = viewModel)

                // ── Food Details ───────────────────────────────────────────────
                FormCard(title = stringResource(R.string.section_food_details)) {
                    FieldBlock(label = stringResource(R.string.label_title)) {
                        ClearChainTextField(
                            value         = state.title,
                            onValueChange = { viewModel.onEvent(CreateListingEvent.TitleChanged(it)) },
                            placeholder   = stringResource(R.string.label_title_placeholder),
                            leadingIcon   = { Icon(Icons.Default.ShoppingCart, null) },
                            imeAction     = ImeAction.Next,
                            isError       = state.titleError != null,
                            errorMessage  = state.titleError,
                            enabled       = !state.isLoading && !state.isAnalyzing
                        )
                    }

                    FieldBlock(label = stringResource(R.string.label_description)) {
                        ClearChainTextField(
                            value         = state.description,
                            onValueChange = { viewModel.onEvent(CreateListingEvent.DescriptionChanged(it)) },
                            placeholder   = stringResource(R.string.label_description_placeholder),
                            leadingIcon   = { Icon(Icons.Default.Description, null) },
                            imeAction     = ImeAction.Next,
                            isError       = state.descriptionError != null,
                            errorMessage  = state.descriptionError,
                            enabled       = !state.isLoading && !state.isAnalyzing,
                            singleLine    = false,
                            minLines      = 2,
                            maxLines      = 4
                        )
                    }

                    FieldBlock(label = stringResource(R.string.label_category)) {
                        ExposedDropdownMenuBox(
                            expanded        = state.showCategoryDropdown,
                            onExpandedChange = {
                                if (!state.isLoading && !state.isAnalyzing)
                                    viewModel.onEvent(CreateListingEvent.ToggleCategoryDropdown)
                            }
                        ) {
                            OutlinedTextField(
                                value         = stringResource(FoodCategory.valueOf(state.category).labelResId),
                                onValueChange = {},
                                readOnly      = true,
                                leadingIcon   = { Icon(Icons.Default.Category, null) },
                                trailingIcon  = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.showCategoryDropdown)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                enabled  = !state.isLoading && !state.isAnalyzing,
                                shape    = MaterialTheme.shapes.medium,
                                colors   = OutlinedTextFieldDefaults.colors()
                            )
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
                }

                // ── Quantity ───────────────────────────────────────────────────
                FormCard(title = stringResource(R.string.section_quantity)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FieldBlock(
                            label    = stringResource(R.string.label_quantity_short),
                            modifier = Modifier.weight(1f)
                        ) {
                            ClearChainTextField(
                                value         = state.quantity,
                                onValueChange = { viewModel.onEvent(CreateListingEvent.QuantityChanged(it)) },
                                placeholder   = stringResource(R.string.hint_quantity_number),
                                leadingIcon   = { Icon(Icons.Default.Numbers, null) },
                                keyboardType  = KeyboardType.Number,
                                imeAction     = ImeAction.Next,
                                isError       = state.quantityError != null,
                                errorMessage  = state.quantityError,
                                enabled       = !state.isLoading && !state.isAnalyzing
                            )
                        }

                        FieldBlock(
                            label    = stringResource(R.string.label_unit),
                            modifier = Modifier.weight(1f)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded        = state.showUnitDropdown,
                                onExpandedChange = {
                                    if (!state.isLoading && !state.isAnalyzing)
                                        viewModel.onEvent(CreateListingEvent.ToggleUnitDropdown)
                                }
                            ) {
                                OutlinedTextField(
                                    value         = state.unit,
                                    onValueChange = {},
                                    readOnly      = true,
                                    trailingIcon  = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.showUnitDropdown)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    enabled  = !state.isLoading && !state.isAnalyzing,
                                    isError  = state.unitError != null,
                                    supportingText = if (state.unitError != null) {
                                        { Text(state.unitError!!) }
                                    } else null,
                                    shape    = MaterialTheme.shapes.medium,
                                    colors   = OutlinedTextFieldDefaults.colors()
                                )
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
                    }
                }

                // ── Schedule ───────────────────────────────────────────────────
                FormCard(title = stringResource(R.string.section_schedule)) {
                    FieldBlock(label = stringResource(R.string.label_expiry_date)) {
                        DatePickerField(
                            value          = state.expiryDate,
                            onDateSelected = { viewModel.onEvent(CreateListingEvent.ExpiryDateChanged(it)) },
                            isError        = state.expiryDateError != null,
                            errorMessage   = state.expiryDateError,
                            enabled        = !state.isLoading && !state.isAnalyzing
                        )
                    }

                    FieldBlock(label = stringResource(R.string.label_pickup_hours_from_profile)) {
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
                    enabled  = !state.isLoading && !state.isAnalyzing,
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

// ── Section card wrapper ───────────────────────────────────────────────────────
@Composable
private fun FormCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                title,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

// ── Field with label above ─────────────────────────────────────────────────────
@Composable
private fun FieldBlock(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier            = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}

// ── AI Analysis Card ───────────────────────────────────────────────────────────
@Composable
private fun AiAnalysisCard(state: CreateListingState, viewModel: CreateListingViewModel) {
    Surface(
        color    = MaterialTheme.colorScheme.primaryContainer,
        shape    = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint        = MaterialTheme.colorScheme.primary,
                        modifier    = Modifier.size(20.dp)
                    )
                    Text(
                        text       = stringResource(R.string.label_ai_food_analysis),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (state.selectedImageUri != null) {
                    IconButton(onClick = { viewModel.onEvent(CreateListingEvent.ClearImage) }) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.cd_remove_image),
                            tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (state.selectedImages.isNotEmpty()) {
                MultiImageStrip(
                    images     = state.selectedImages,
                    onRemove   = { index -> viewModel.onEvent(CreateListingEvent.RemoveImage(index)) },
                    onReorder  = { from, to -> viewModel.onEvent(CreateListingEvent.ReorderImages(from, to)) },
                    canAddMore = state.selectedImages.size < 5,
                    onAddMore  = { viewModel.onEvent(CreateListingEvent.ToggleImagePicker) }
                )
            }

            if (state.selectedImageUri != null) {
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
                                Icon(
                                    Icons.Default.AutoAwesome, null,
                                    modifier = Modifier.size(14.dp),
                                    tint     = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    stringResource(R.string.label_analyzing_food),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    state.analysisResult != null -> {
                        val result = state.analysisResult!!
                        Surface(
                            color    = MaterialTheme.colorScheme.tertiaryContainer,
                            shape    = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier            = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle, null,
                                        tint     = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text       = result.title,
                                        style      = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AnalysisChip(stringResource(R.string.label_quality_grade, result.qualityGrade))
                                    AnalysisChip(stringResource(R.string.label_freshness_score, result.freshnessScore))
                                    AnalysisChip(stringResource(R.string.label_confidence_pct, (result.confidence * 100).toInt()))
                                }
                                Button(
                                    onClick  = { viewModel.onEvent(CreateListingEvent.ApplyAISuggestions) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    ),
                                    shape    = MaterialTheme.shapes.small
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_apply_ai))
                                }
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
                    shape    = MaterialTheme.shapes.medium,
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_take_photo))
                }
                Text(
                    text  = stringResource(R.string.label_ai_analyze_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AnalysisChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ── Multi-image strip with drag-to-reorder ─────────────────────────────────────
@Composable
private fun MultiImageStrip(
    images: List<android.net.Uri>,
    onRemove: (Int) -> Unit,
    onReorder: (Int, Int) -> Unit,
    canAddMore: Boolean,
    onAddMore: () -> Unit
) {
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetX  by remember { mutableFloatStateOf(0f) }
    val itemWidthPx  = 96.dp

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding        = PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(images, key = { _, uri -> uri.toString() }) { index, uri ->
            val isDragging = draggedIndex == index
            Box(
                modifier = Modifier
                    .size(itemWidthPx, 80.dp)
                    .graphicsLayer {
                        if (isDragging) {
                            translationX  = dragOffsetX
                            scaleX        = 1.05f
                            scaleY        = 1.05f
                            shadowElevation = 8.dp.toPx()
                        }
                    }
                    .shadow(if (isDragging) 6.dp else 0.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart  = { draggedIndex = index; dragOffsetX = 0f },
                            onDragEnd    = { draggedIndex = -1;    dragOffsetX = 0f },
                            onDragCancel = { draggedIndex = -1;    dragOffsetX = 0f },
                            onDrag       = { _, dragAmount ->
                                dragOffsetX += dragAmount.x
                                val itemPx = itemWidthPx.toPx() + 8.dp.toPx()
                                val steps  = (dragOffsetX / itemPx).toInt()
                                val target = (index + steps).coerceIn(0, images.size - 1)
                                if (target != index) {
                                    onReorder(index, target)
                                    draggedIndex = target
                                    dragOffsetX -= steps * itemPx
                                }
                            }
                        )
                    }
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = if (index == 0)
                        stringResource(R.string.cd_primary_image)
                    else
                        stringResource(R.string.cd_additional_image, index + 1),
                    contentScale = ContentScale.Crop,
                    modifier     = Modifier.fillMaxSize()
                )
                if (index == 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            stringResource(R.string.label_primary_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                IconButton(
                    onClick  = { onRemove(index) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_remove_image),
                        modifier = Modifier.size(12.dp),
                        tint     = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (canAddMore) {
            item {
                OutlinedButton(
                    onClick         = onAddMore,
                    modifier        = Modifier.size(itemWidthPx, 80.dp),
                    shape           = RoundedCornerShape(8.dp),
                    contentPadding  = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.cd_add_photo),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(stringResource(R.string.action_add), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
