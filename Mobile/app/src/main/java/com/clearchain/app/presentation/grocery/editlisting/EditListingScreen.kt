package com.clearchain.app.presentation.grocery.editlisting

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.clearchain.app.R
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.ShapeMedium
import com.clearchain.app.util.UiEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingScreen(
    listingId: String,
    navController: NavController,
    viewModel: EditListingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(listingId) { viewModel.loadListing(listingId) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.NavigateUp   -> navController.navigateUp()
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val busy = state.isSaving
                val titleInvalid    = state.title.isBlank() || state.title.length < 3
                val descInvalid     = state.description.isBlank()
                val qtyInvalid      = (state.quantity.toIntOrNull() ?: 0) <= 0
                val expiryInvalid   = state.expiryDate.isBlank()
                val canSave = !titleInvalid && !descInvalid && !qtyInvalid && !expiryInvalid

                // ── Photo (read-only) ─────────────────────────────────────────
                if (state.imageUrl.isNotBlank()) {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        AsyncImage(
                            model              = state.imageUrl,
                            contentDescription = null,
                            modifier           = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // ── Title ─────────────────────────────────────────────────────
                FieldCard(label = stringResource(R.string.label_title)) {
                    ClearChainTextField(
                        value         = state.title,
                        onValueChange = { viewModel.onEvent(EditListingEvent.TitleChanged(it)) },
                        placeholder   = stringResource(R.string.label_title_placeholder),
                        leadingIcon   = Icons.Default.ShoppingCart,
                        imeAction     = ImeAction.Next,
                        isError       = state.titleError != null,
                        errorMessage  = state.titleError,
                        enabled       = !busy
                    )
                }

                // ── Description ───────────────────────────────────────────────
                FieldCard(label = stringResource(R.string.label_description)) {
                    ClearChainTextField(
                        value         = state.description,
                        onValueChange = { viewModel.onEvent(EditListingEvent.DescriptionChanged(it)) },
                        placeholder   = stringResource(R.string.label_description_placeholder),
                        leadingIcon   = Icons.Default.Description,
                        imeAction     = ImeAction.Next,
                        isError       = state.descriptionError != null,
                        errorMessage  = state.descriptionError,
                        enabled       = !busy,
                        singleLine    = false,
                        minLines      = 2,
                        maxLines      = 4
                    )
                }

                // ── Category ──────────────────────────────────────────────────
                FieldCard(label = stringResource(R.string.label_category)) {
                    val iconTint  = if (!busy) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    val textColor = if (!busy) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ExposedDropdownMenuBox(
                        expanded         = state.showCategoryDropdown,
                        onExpandedChange = { if (!busy) viewModel.onEvent(EditListingEvent.ToggleCategoryDropdown) }
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
                            onDismissRequest = { viewModel.onEvent(EditListingEvent.ToggleCategoryDropdown) }
                        ) {
                            FoodCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text    = { Text(stringResource(cat.labelResId)) },
                                    onClick = { viewModel.onEvent(EditListingEvent.CategoryChanged(cat.name)) }
                                )
                            }
                        }
                    }
                }

                // ── Quantity + Unit ───────────────────────────────────────────
                FieldCard(label = stringResource(R.string.label_quantity_short)) {
                    val iconTint  = if (!busy) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    val textColor = if (!busy) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClearChainTextField(
                            value         = state.quantity,
                            onValueChange = { viewModel.onEvent(EditListingEvent.QuantityChanged(it)) },
                            placeholder   = stringResource(R.string.hint_quantity_number),
                            leadingIcon   = Icons.Default.Numbers,
                            keyboardType  = KeyboardType.Number,
                            imeAction     = ImeAction.Next,
                            isError       = state.quantityError != null,
                            errorMessage  = state.quantityError,
                            enabled       = !busy,
                            modifier      = Modifier.weight(1f)
                        )
                        ExposedDropdownMenuBox(
                            expanded         = state.showUnitDropdown,
                            onExpandedChange = { if (!busy) viewModel.onEvent(EditListingEvent.ToggleUnitDropdown) },
                            modifier         = Modifier.weight(1f)
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
                                Text(state.unit, style = MaterialTheme.typography.labelLarge,
                                    color = textColor, modifier = Modifier.weight(1f))
                                Icon(
                                    if (state.showUnitDropdown) Icons.Default.ArrowDropUp
                                    else Icons.Default.ArrowDropDown,
                                    null, Modifier.size(18.dp), tint = iconTint
                                )
                            }
                            ExposedDropdownMenu(
                                expanded         = state.showUnitDropdown,
                                onDismissRequest = { viewModel.onEvent(EditListingEvent.ToggleUnitDropdown) }
                            ) {
                                listOf("kg", "g", "L", "mL", "pieces", "boxes", "bags").forEach { unit ->
                                    DropdownMenuItem(
                                        text    = { Text(unit) },
                                        onClick = { viewModel.onEvent(EditListingEvent.UnitChanged(unit)) }
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

                // ── Expiry Date ───────────────────────────────────────────────
                val futureDates = remember {
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            val date = Instant.ofEpochMilli(utcTimeMillis)
                                .atZone(ZoneId.of("UTC")).toLocalDate()
                            return !date.isBefore(LocalDate.now())
                        }
                    }
                }
                FieldCard(label = stringResource(R.string.label_expiry_date)) {
                    DatePickerField(
                        value           = state.expiryDate,
                        onDateSelected  = { viewModel.onEvent(EditListingEvent.ExpiryDateChanged(it)) },
                        isError         = state.expiryDateError != null,
                        errorMessage    = state.expiryDateError,
                        enabled         = !busy,
                        selectableDates = futureDates,
                        onClearDate     = { viewModel.onEvent(EditListingEvent.ExpiryDateChanged("")) }
                    )
                }

                // ── Pickup Hours (read-only from profile) ─────────────────────
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
                            Icon(Icons.Default.Schedule, null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp))
                            Text(
                                state.groceryHours ?: stringResource(R.string.label_pickup_hours_not_set),
                                style      = MaterialTheme.typography.bodySmall,
                                color      = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ── Error banner ──────────────────────────────────────────────
                AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut()) {
                    AlertBanner(
                        message = state.error ?: "",
                        type    = AlertType.ERROR,
                        icon    = Icons.Default.ErrorOutline
                    )
                }

                // ── Cancel + Save buttons ─────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = { navController.navigateUp() },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        enabled  = !busy
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick  = { viewModel.onEvent(EditListingEvent.SaveListing) },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        enabled  = canSave && !busy
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.save))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FieldCard(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
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
            Text(label, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}
