package com.clearchain.app.presentation.ngo.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToItemDetail: (String) -> Unit = {},
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val statusFilters = listOf(
        FilterChipData("ACTIVE", stringResource(R.string.status_active)),
        FilterChipData("DISTRIBUTED", stringResource(R.string.status_distributed)),
        FilterChipData("EXPIRED", stringResource(R.string.status_expired))
    )
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
    }

    // Manual add bottom sheet
    if (state.showManualAddSheet) {
        ManualAddSheet(
            state    = state,
            onEvent  = { viewModel.onEvent(it) },
            onDismiss = { viewModel.onEvent(InventoryEvent.HideManualAddSheet) }
        )
    }

    BackHandler(state.isSelectionMode) { viewModel.onEvent(InventoryEvent.ToggleSelectionMode) }

    if (state.showFilterSheet) {
        InventoryFilterSheet(
            state     = state,
            onEvent   = { viewModel.onEvent(it) },
            onDismiss = { viewModel.onEvent(InventoryEvent.HideFilterSheet) }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (!state.isSelectionMode) {
                SmallFloatingActionButton(onClick = { viewModel.onEvent(InventoryEvent.ShowManualAddSheet) }) {
                    Icon(Icons.Default.Add, stringResource(R.string.cd_add_item_manually))
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.isSelectionMode && state.selectedCount > 0,
                enter   = slideInVertically { it },
                exit    = slideOutVertically { it }
            ) {
                Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClearChainButton(
                            text = stringResource(R.string.action_distribute_count, state.activeSelectedCount),
                            onClick  = { viewModel.onEvent(InventoryEvent.BulkDistribute) },
                            modifier = Modifier.weight(1f),
                            enabled  = state.activeSelectedCount > 0,
                            loading = state.isBulkOperating
                        )
                    }
                }
            }
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                        ListScreenHeader {
                        ListHeaderSearchRow(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.onEvent(InventoryEvent.SearchQueryChanged(it)) },
                            placeholder = stringResource(R.string.search_inventory_placeholder)
                        ) {
                            ClearChainActionIconButton(
                                icon               = Icons.Default.FileDownload,
                                contentDescription = stringResource(R.string.export_csv),
                                onClick            = { viewModel.onEvent(InventoryEvent.ExportCsv) }
                            )
                            BadgedBox(
                                badge = {
                                    if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) }
                                }
                            ) {
                                ClearChainActionIconButton(
                                    icon               = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.advanced_filters),
                                    onClick            = { viewModel.onEvent(InventoryEvent.ShowFilterSheet) }
                                )
                            }
                        }

                        FilterChipsRow(
                            filters = statusFilters,
                            selectedFilter = state.selectedStatusTab?.name,
                            onFilterSelected = { value ->
                                val status = value?.let { InventoryStatus.valueOf(it) }
                                viewModel.onEvent(InventoryEvent.StatusTabChanged(status))
                            }
                        )

                        ResultsCountAndSort(
                            count = state.filteredItems.size,
                            itemName = "item",
                            selectedSort = state.selectedSort,
                            onSortSelected = { viewModel.onEvent(InventoryEvent.SortOptionChanged(it)) },
                            sortOptions = state.availableSortOptions,
                            countText = if (state.isSelectionMode) {
                                "${state.selectedCount} ${if (state.selectedCount == 1) "item" else "items"} selected"
                            } else null,
                            leadingContent = if (state.isSelectionMode) {
                                {
                                    SelectionCircleButton(
                                        checked = state.allSelected,
                                        onCheckedChange = {
                                            if (state.allSelected) viewModel.onEvent(InventoryEvent.DeselectAll)
                                            else viewModel.onEvent(InventoryEvent.SelectAll)
                                        }
                                    )
                                }
                            } else null
                        )
                        }

                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        when {
                            state.isLoading && state.allItems.isEmpty() -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }

                            state.error != null && state.allItems.isEmpty() -> {
                                EmptyState(
                                    icon = Icons.Default.ErrorOutline,
                                    title = stringResource(R.string.error_generic),
                                    subtitle = state.error,
                                    actionLabel = stringResource(R.string.retry),
                                    onAction = { viewModel.onEvent(InventoryEvent.LoadInventory) }
                                )
                            }

                            state.filteredItems.isEmpty() -> {
                                EmptyState(
                                    icon = if (state.allItems.isEmpty()) Icons.Default.Inventory2 else Icons.Default.FilterAlt,
                                    title = if (state.allItems.isEmpty()) stringResource(R.string.empty_no_inventory)
                                    else stringResource(R.string.empty_no_inventory_filter),
                                    subtitle = if (state.allItems.isEmpty())
                                        stringResource(R.string.empty_no_inventory_subtitle)
                                    else stringResource(R.string.empty_try_filters)
                                )
                            }

                            else -> {
                                HapticPullToRefreshBox(
                                    isRefreshing = state.isRefreshing,
                                    onRefresh = { viewModel.onEvent(InventoryEvent.RefreshInventory) }
                                ) {
                                    LazyColumn(
                                        contentPadding = ScreenPadding,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(state.filteredItems, key = { it.id }) { item ->
                                            val isSelected = item.id in state.selectedIds
                                            Box {
                                                InventoryItemCard(
                                                    item     = item,
                                                    onClick  = {
                                                        if (state.isSelectionMode) {
                                                            viewModel.onEvent(InventoryEvent.ToggleItemSelection(item.id))
                                                        } else {
                                                            onNavigateToItemDetail(item.id)
                                                        }
                                                    },
                                                    modifier = Modifier.combinedClickable(
                                                        onClick = {
                                                            if (state.isSelectionMode) {
                                                                viewModel.onEvent(InventoryEvent.ToggleItemSelection(item.id))
                                                            } else {
                                                                onNavigateToItemDetail(item.id)
                                                            }
                                                        },
                                                        onLongClick = {
                                                            viewModel.onEvent(InventoryEvent.ToggleSelectionMode)
                                                            viewModel.onEvent(InventoryEvent.ToggleItemSelection(item.id))
                                                        }
                                                    ),
                                                    onDistribute = if (!state.isSelectionMode && item.status == InventoryStatus.ACTIVE) {
                                                        { viewModel.onEvent(InventoryEvent.DistributeItem(it)) }
                                                    } else null
                                                )
                                                if (state.isSelectionMode) {
                                                    SelectionCircleButton(
                                                        checked = isSelected,
                                                        onCheckedChange = { viewModel.onEvent(InventoryEvent.ToggleItemSelection(item.id)) },
                                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                                    )
                                                }
                                            }
                                        }

                                        item { Spacer(Modifier.height(80.dp)) }
                                    }
                                }
                            }
                        }
                        }
            }
        }
    }
}

@Composable
private fun SelectionCircleButton(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onCheckedChange,
        modifier = modifier.size(24.dp),
        shape = RoundedCornerShape(50),
        color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = if (checked) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (checked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ── Advanced filter sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun InventoryFilterSheet(
    state: InventoryState,
    onEvent: (InventoryEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.advanced_filters),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (state.activeFilterCount > 0) {
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.action_clear_all),
                        onClick = { onEvent(InventoryEvent.ClearAdvancedFilters) }
                    )
                }
            }

            // Category
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.filter_category), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick  = { onEvent(InventoryEvent.CategoryFilterChanged(null)) },
                        label    = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelSmall) }
                    )
                    FoodCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = state.selectedCategory == cat.name,
                            onClick  = { onEvent(InventoryEvent.CategoryFilterChanged(if (state.selectedCategory == cat.name) null else cat.name)) },
                            label    = { Text(stringResource(cat.labelResId), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Expiry urgency
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.filter_expiry_urgency), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Any", 1 to "Today", 2 to "48h", 3 to "3 days", 7 to "7 days")
                        .forEach { (days, label) ->
                            FilterChip(
                                selected = state.filterExpiryWithinDays == days,
                                onClick  = { onEvent(InventoryEvent.FilterExpiryWithinDaysChanged(days)) },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                }
            }

            // Quantity range
            val qtyLabel = when {
                state.filterMinQty > 0.0 && state.filterMaxQty != null ->
                    "${state.filterMinQty.toInt()}–${state.filterMaxQty.toInt()} units"
                state.filterMinQty > 0.0 -> "Min ${state.filterMinQty.toInt()} units"
                state.filterMaxQty != null -> "Up to ${state.filterMaxQty.toInt()} units"
                else -> "Any quantity"
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${stringResource(R.string.filter_quantity_range)}: $qtyLabel", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                RangeSlider(
                    value         = state.filterMinQty.toFloat()..(state.filterMaxQty?.toFloat() ?: 500f),
                    onValueChange = { range ->
                        onEvent(InventoryEvent.FilterMinQtyChanged(range.start.toDouble()))
                        onEvent(InventoryEvent.FilterMaxQtyChanged(if (range.endInclusive >= 500f) null else range.endInclusive.toDouble()))
                    },
                    valueRange = 0f..500f, steps = 49
                )
            }

            ClearChainButton(
                text = stringResource(R.string.action_apply_filters),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Category breakdown horizontal bar chart ──────────────────────────────────

// ── Manual add bottom sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ManualAddSheet(
    state:     InventoryState,
    onEvent:   (InventoryEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.add_item_manually),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value         = state.manualProductName,
                onValueChange = { onEvent(InventoryEvent.ManualProductNameChanged(it)) },
                label         = { Text(stringResource(R.string.label_product_name)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            // Category selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.label_category), style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FoodCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = state.manualCategory == cat.name,
                            onClick  = { onEvent(InventoryEvent.ManualCategoryChanged(cat.name)) },
                            label    = { Text(stringResource(cat.labelResId), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = state.manualQuantity,
                    onValueChange = { onEvent(InventoryEvent.ManualQuantityChanged(it)) },
                    label         = { Text(stringResource(R.string.label_quantity_required)) },
                    modifier      = Modifier.weight(1f),
                    singleLine    = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value         = state.manualUnit,
                    onValueChange = { onEvent(InventoryEvent.ManualUnitChanged(it)) },
                    label         = { Text(stringResource(R.string.label_unit)) },
                    modifier      = Modifier.width(80.dp),
                    singleLine    = true
                )
            }

            DatePickerField(
                value          = state.manualExpiryDate,
                onDateSelected = { onEvent(InventoryEvent.ManualExpiryDateChanged(it)) },
                label          = stringResource(R.string.label_expiry_date_field)
            )

            ClearChainButton(
                text = stringResource(R.string.action_add_to_inventory),
                onClick  = { onEvent(InventoryEvent.SubmitManualAdd) },
                modifier = Modifier.fillMaxWidth(),
                enabled  = state.manualProductName.isNotBlank(),
                loading = state.isSubmittingManual
            )
        }
    }
}
