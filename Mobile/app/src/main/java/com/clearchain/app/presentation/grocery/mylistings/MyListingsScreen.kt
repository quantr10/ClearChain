package com.clearchain.app.presentation.grocery.mylistings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.ListingStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyListingsScreen(
    navController: NavController,
    viewModel: MyListingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    if (state.showFilterSheet) {
        MyListingsFilterSheet(
            state     = state,
            onEvent   = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(MyListingsEvent.HideFilterSheet) }
        )
    }

    if (showBulkDeleteConfirm) {
        ConfirmDialog(
            title       = stringResource(R.string.bulk_delete),
            message     = stringResource(R.string.delete_account_confirm),
            confirmLabel = stringResource(R.string.delete),
            isDestructive = true,
            onConfirm   = { showBulkDeleteConfirm = false; viewModel.onEvent(MyListingsEvent.BulkDelete) },
            onDismiss   = { showBulkDeleteConfirm = false }
        )
    }

    BackHandler(state.isSelectionMode) { viewModel.onEvent(MyListingsEvent.ToggleSelectionMode) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Bulk action bottom bar
        bottomBar = {
            AnimatedVisibility(
                visible = state.isSelectionMode &&
                    state.selectedCount > 0 &&
                    state.activeTab in setOf(MyListingsTab.AVAILABLE, MyListingsTab.ARCHIVED),
                enter   = slideInVertically { it },
                exit    = slideOutVertically { it }
            ) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (state.activeTab) {
                            MyListingsTab.AVAILABLE -> {
                                ClearChainOutlinedButton(
                                    text = stringResource(R.string.action_archive),
                                    onClick = { viewModel.onEvent(MyListingsEvent.BulkArchive) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.isBulkOperating,
                                    loading = state.bulkOperation == MyListingsBulkOperation.ARCHIVE,
                                    icon = Icons.Default.Archive
                                )
                                ClearChainButton(
                                    text = stringResource(R.string.delete),
                                    onClick = { showBulkDeleteConfirm = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.isBulkOperating,
                                    loading = state.bulkOperation == MyListingsBulkOperation.DELETE,
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    icon = Icons.Default.Delete
                                )
                            }
                            MyListingsTab.ARCHIVED -> {
                                ClearChainOutlinedButton(
                                    text = stringResource(R.string.action_restore),
                                    onClick = { viewModel.onEvent(MyListingsEvent.BulkRestore) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.isBulkOperating,
                                    loading = state.bulkOperation == MyListingsBulkOperation.RESTORE,
                                    icon = Icons.Default.Unarchive
                                )
                                ClearChainButton(
                                    text = stringResource(R.string.delete),
                                    onClick = { showBulkDeleteConfirm = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.isBulkOperating,
                                    loading = state.bulkOperation == MyListingsBulkOperation.DELETE,
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    icon = Icons.Default.Delete
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!state.isSelectionMode) {
                FloatingActionButton(
                    onClick        = { navController.navigate(Screen.CreateListing.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.cd_create_listing))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchBar(
                                query       = state.searchQuery,
                                onQueryChange = { viewModel.onEvent(MyListingsEvent.SearchQueryChanged(it)) },
                                placeholder = stringResource(R.string.search_listings_placeholder),
                                modifier    = Modifier.weight(1f)
                            )
                            BadgedBox(
                                badge = {
                                    if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) }
                                }
                            ) {
                                ClearChainActionIconButton(
                                    icon               = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.advanced_filters),
                                    onClick            = { viewModel.onEvent(MyListingsEvent.ShowFilterSheet) }
                                )
                            }
                        }

                        // Tab row: Available | Archived | Reserved | Expired
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tabs = listOf(
                                MyListingsTab.AVAILABLE to stringResource(R.string.tab_available),
                                MyListingsTab.ARCHIVED  to stringResource(R.string.tab_archived),
                                MyListingsTab.RESERVED  to stringResource(R.string.status_reserved),
                                MyListingsTab.EXPIRED   to stringResource(R.string.status_expired)
                            )
                            tabs.forEach { (tab, label) ->
                                FilterChip(
                                    selected = state.activeTab == tab,
                                    onClick  = { viewModel.onEvent(MyListingsEvent.TabChanged(tab)) },
                                    label    = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    shape    = RoundedCornerShape(50)
                                )
                            }
                        }

                        ResultsCountAndSort(
                            count          = state.filteredListings.size,
                            itemName       = "listing",
                            selectedSort   = state.selectedSort,
                            onSortSelected = { viewModel.onEvent(MyListingsEvent.SortOptionChanged(it)) },
                            sortOptions    = state.availableSortOptions,
                            countText      = if (state.isSelectionMode) {
                                "${state.selectedCount} ${if (state.selectedCount == 1) "listing" else "listings"} selected"
                            } else null,
                            leadingContent = if (state.isSelectionMode) {
                                {
                                    CircleCheckbox(
                                        checked = state.allSelected,
                                        onCheckedChange = {
                                            if (state.allSelected) viewModel.onEvent(MyListingsEvent.DeselectAll)
                                            else viewModel.onEvent(MyListingsEvent.SelectAll)
                                        }
                                    )
                                }
                            } else null
                        )

                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        when {
                            state.isLoading && state.allListings.isEmpty() -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }

                            state.error != null && state.allListings.isEmpty() -> {
                                EmptyState(
                                    icon       = Icons.Default.ErrorOutline,
                                    title      = stringResource(R.string.error_generic),
                                    subtitle   = state.error,
                                    actionLabel = stringResource(R.string.retry),
                                    onAction   = { viewModel.onEvent(MyListingsEvent.LoadListings) }
                                )
                            }

                            state.filteredListings.isEmpty() -> {
                                EmptyState(
                                    icon   = if (state.allListings.isEmpty()) Icons.Default.PostAdd else Icons.Default.FilterAlt,
                                    title  = if (state.allListings.isEmpty()) stringResource(R.string.empty_no_listings) else stringResource(R.string.empty_no_listings_filter),
                                    subtitle = if (state.allListings.isEmpty()) stringResource(R.string.empty_no_listings_subtitle) else stringResource(R.string.empty_try_filters)
                                )
                            }

                            else -> {
                                HapticPullToRefreshBox(
                                    isRefreshing = state.isRefreshing,
                                    onRefresh    = { viewModel.onEvent(MyListingsEvent.RefreshListings) }
                                ) {
                                    LazyColumn(
                                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(state.filteredListings, key = { it.id }) { listing ->
                                            var showEditQty by remember { mutableStateOf(false) }
                                            val isSelected  = listing.id in state.selectedIds

                                            Box {
                                            ListingCard(
                                                listing          = listing,
                                                modifier         = Modifier.combinedClickable(
                                                    onClick      = {
                                                        if (state.isSelectionMode) {
                                                            viewModel.onEvent(MyListingsEvent.ToggleItemSelection(listing.id))
                                                        } else {
                                                            navController.navigate(Screen.ListingDetail.createRoute(listing.id))
                                                        }
                                                    },
                                                    onLongClick  = {
                                                        if (state.activeTab in setOf(MyListingsTab.AVAILABLE, MyListingsTab.ARCHIVED)) {
                                                            if (!state.isSelectionMode) {
                                                                viewModel.onEvent(MyListingsEvent.ToggleSelectionMode)
                                                            }
                                                            viewModel.onEvent(MyListingsEvent.ToggleItemSelection(listing.id))
                                                        }
                                                    }
                                                ),
                                                showGroceryInfo = false,
                                                topRightAction  = if (!state.isSelectionMode && listing.status == ListingStatus.AVAILABLE) {
                                                    {
                                                        IconButton(
                                                            onClick  = { navController.navigate(Screen.EditListing.createRoute(listing.id)) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Edit,
                                                                contentDescription = stringResource(R.string.action_edit_qty),
                                                                modifier = Modifier.size(18.dp),
                                                                tint     = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                } else null,
                                                secondaryActions = if (!state.isSelectionMode && (listing.viewCount > 0 || listing.requestCount > 0)) {
                                                    {
                                                        Row(
                                                            modifier = Modifier.weight(1f),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            if (listing.viewCount > 0) {
                                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                                    Icon(Icons.Default.Visibility, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                    Text("${listing.viewCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                }
                                                            }
                                                            if (listing.requestCount > 0) {
                                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                                    Icon(Icons.Default.Inventory, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                                    Text("${listing.requestCount} req", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else null
                                            )

                                            // Selection circle overlay (top-left)
                                            if (state.isSelectionMode) {
                                                CircleCheckbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { viewModel.onEvent(MyListingsEvent.ToggleItemSelection(listing.id)) },
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(8.dp)
                                                )
                                            }
                                            } // end Box

                                            if (showEditQty) {
                                                EditQuantityDialog(
                                                    currentQuantity = listing.quantity,
                                                    unit = listing.unit,
                                                    onDismiss = { showEditQty = false },
                                                    onConfirm = { newQty ->
                                                        showEditQty = false
                                                        viewModel.onEvent(MyListingsEvent.UpdateListingQuantity(listing.id, newQty))
                                                    }
                                                )
                                            }
                                        }

                                        item { Spacer(Modifier.height(16.dp)) }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MyListingsFilterSheet(
    state: MyListingsState,
    onEvent: (MyListingsEvent) -> Unit,
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
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.action_clear_all),
                        onClick = { onEvent(MyListingsEvent.ClearAdvancedFilters) }
                    )
                }
            }

            // Category
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.filter_category), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick  = { onEvent(MyListingsEvent.CategoryFilterChanged(null)) },
                        label    = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelSmall) }
                    )
                    FoodCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = state.selectedCategory == cat.name,
                            onClick  = { onEvent(MyListingsEvent.CategoryFilterChanged(if (state.selectedCategory == cat.name) null else cat.name)) },
                            label    = { Text(stringResource(cat.labelResId), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Expiry within
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.filter_expiry_within), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Any", 1 to "1 day", 3 to "3 days", 7 to "7 days", 14 to "2 weeks")
                        .forEach { (days, label) ->
                            FilterChip(
                                selected = state.filterExpiryWithinDays == days,
                                onClick  = { onEvent(MyListingsEvent.FilterExpiryWithinDaysChanged(days)) },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                }
            }

            // Has pickup requests
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.filter_has_requests), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked  = state.filterHasRequests,
                    onCheckedChange = { onEvent(MyListingsEvent.FilterHasRequestsChanged(it)) }
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

@Composable
private fun CircleCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick         = onCheckedChange,
        modifier        = modifier.size(24.dp),
        shape           = CircleShape,
        color           = if (checked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.92f),
        border          = if (!checked) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        shadowElevation = 1.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (checked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun EditQuantityDialog(
    currentQuantity: Int,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantity by remember { mutableStateOf(currentQuantity.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val errorInvalidNumber = stringResource(R.string.error_invalid_number)
    val errorMustBePositive = stringResource(R.string.error_must_be_positive)
    val errorSameAsCurrent = stringResource(R.string.error_same_as_current)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_edit_quantity)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.label_current_qty, currentQuantity, unit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value         = quantity,
                    onValueChange = { quantity = it; error = null },
                    label         = { Text(stringResource(R.string.label_new_quantity)) },
                    suffix        = { Text(unit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError       = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            ClearChainOutlinedButton(
                text = stringResource(R.string.action_update),
                onClick = {
                val newQty = quantity.toIntOrNull()
                when {
                    newQty == null -> error = errorInvalidNumber
                    newQty <= 0   -> error = errorMustBePositive
                    newQty == currentQuantity -> error = errorSameAsCurrent
                    else -> onConfirm(newQty)
                }
            })
        },
        dismissButton = {
            ClearChainOutlinedButton(text = stringResource(R.string.cancel), onClick = onDismiss)
        }
    )
}
