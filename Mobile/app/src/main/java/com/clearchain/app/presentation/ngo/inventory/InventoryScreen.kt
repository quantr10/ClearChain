package com.clearchain.app.presentation.ngo.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BrandGreen
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

    // Beneficiary count dialog
    state.showBeneficiaryDialogForId?.let {
        BeneficiaryCountDialog(
            count     = state.beneficiaryCount,
            onChange  = { viewModel.onEvent(InventoryEvent.BeneficiaryCountChanged(it)) },
            onConfirm = { viewModel.onEvent(InventoryEvent.ConfirmDistribute) },
            onDismiss = { viewModel.onEvent(InventoryEvent.DismissBeneficiaryDialog) }
        )
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
                FloatingActionButton(onClick = { viewModel.onEvent(InventoryEvent.ShowManualAddSheet) }) {
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
                        Button(
                            onClick  = { viewModel.onEvent(InventoryEvent.BulkDistribute) },
                            modifier = Modifier.weight(1f),
                            enabled  = !state.isBulkOperating && state.activeSelectedCount > 0
                        ) {
                            if (state.isBulkOperating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Text(stringResource(R.string.action_distribute_count, state.activeSelectedCount))
                        }
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
            when {
                state.isLoading && state.allItems.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null && state.allItems.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = stringResource(R.string.msg_failed_load_inventory),
                        subtitle = state.error,
                        actionLabel = stringResource(R.string.retry),
                        onAction = { viewModel.onEvent(InventoryEvent.LoadInventory) }
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchBar(
                                query = state.searchQuery,
                                onQueryChange = { viewModel.onEvent(InventoryEvent.SearchQueryChanged(it)) },
                                placeholder = stringResource(R.string.search_inventory_placeholder),
                                modifier = Modifier.weight(1f),
                            )
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
                            },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        if (state.isSelectionMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    if (state.allSelected) viewModel.onEvent(InventoryEvent.DeselectAll)
                                    else viewModel.onEvent(InventoryEvent.SelectAll)
                                }) {
                                    Text(if (state.allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all))
                                }
                            }
                        }

                        ResultsCountAndSort(
                            count = state.filteredItems.size,
                            itemName = "item",
                            selectedSort = state.selectedSort,
                            onSortSelected = { viewModel.onEvent(InventoryEvent.SortOptionChanged(it)) },
                            sortOptions = state.availableSortOptions,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        state.error?.let {
                            ErrorBanner(
                                message = it,
                                onDismiss = { viewModel.onEvent(InventoryEvent.ClearError) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        when {
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
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Expiry alerts banner
                                        if (state.expiringItems.isNotEmpty() && state.selectedStatusTab == InventoryStatus.ACTIVE) {
                                            item {
                                                ExpiryAlertBanner(
                                                    totalCount    = state.expiringItems.size,
                                                    criticalCount = state.criticalExpiryItems.size,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }
                                        }

                                        // Category breakdown (when showing all active)
                                        if (state.selectedStatusTab == InventoryStatus.ACTIVE && state.categoryBreakdown.isNotEmpty() && state.allItems.size >= 3) {
                                            item {
                                                CategoryBreakdownCard(
                                                    breakdown = state.categoryBreakdown,
                                                    total     = state.allItems.count { it.status == InventoryStatus.ACTIVE }
                                                )
                                            }
                                        }

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
                                                        { viewModel.onEvent(InventoryEvent.ShowBeneficiaryDialog(it)) }
                                                    } else null
                                                )
                                                if (state.isSelectionMode) {
                                                    Checkbox(
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
}

// ─────────────────────────────────────────────────────────────────────────────
// Advanced filter sheet
// ─────────────────────────────────────────────────────────────────────────────

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.advanced_filters),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (state.activeFilterCount > 0) {
                    TextButton(onClick = { onEvent(InventoryEvent.ClearAdvancedFilters) }) {
                        Text(stringResource(R.string.action_clear_all))
                    }
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
                        label    = { Text(stringResource(R.string.filter_all)) }
                    )
                    FoodCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = state.selectedCategory == cat.name,
                            onClick  = { onEvent(InventoryEvent.CategoryFilterChanged(if (state.selectedCategory == cat.name) null else cat.name)) },
                            label    = { Text(stringResource(cat.labelResId)) }
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
                                label    = { Text(label) }
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

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_apply_filters))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Expiry alert banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpiryAlertBanner(
    totalCount: Int,
    criticalCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Critical tier: expiring within 48 hours
        if (criticalCount > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Error, null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Expiring within 48 hours!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "$criticalCount item${if (criticalCount != 1) "s" else ""} need immediate attention",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        // Warning tier: expiring within 3 days (excludes critical)
        val warningCount = totalCount - criticalCount
        if (warningCount > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, null,
                        tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Text(
                        "$warningCount item${if (warningCount != 1) "s" else ""} expiring in 3 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category breakdown horizontal bar chart
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryBreakdownCard(breakdown: List<Pair<String, Int>>, total: Int) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Active Items by Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            val colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                BrandGreen,
                MaterialTheme.colorScheme.error
            )
            breakdown.forEachIndexed { idx, (category, count) ->
                val fraction = if (total > 0) count.toFloat() / total else 0f
                val color = colors.getOrElse(idx) { MaterialTheme.colorScheme.primary }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(category.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium)
                        Text("$count",
                            style = MaterialTheme.typography.labelMedium,
                            color = color, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color    = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Beneficiary count dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BeneficiaryCountDialog(
    count:     String,
    onChange:  (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.People, null) },
        title = { Text(stringResource(R.string.label_beneficiaries_count)) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.label_beneficiaries_optional))
                OutlinedTextField(
                    value         = count,
                    onValueChange = onChange,
                    label         = { Text(stringResource(R.string.label_number_people_optional)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.action_distribute_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Manual add bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            label    = { Text(stringResource(cat.labelResId)) }
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

            Button(
                onClick  = { onEvent(InventoryEvent.SubmitManualAdd) },
                modifier = Modifier.fillMaxWidth(),
                enabled  = state.manualProductName.isNotBlank() && !state.isSubmittingManual
            ) {
                if (state.isSubmittingManual) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_add_to_inventory))
                }
            }
        }
    }
}
