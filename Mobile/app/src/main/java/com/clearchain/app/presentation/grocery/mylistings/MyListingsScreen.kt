// ═══════════════════════════════════════════════════════════════════════════════
// MyListingsScreen.kt — Same structure as ManageRequestsScreen
// with status chips + category chips + fullscreen error on first load fail
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.grocery.mylistings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.domain.model.ListingStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent

private val statusFilters = listOf(
    FilterChipData(null, "All"),
    FilterChipData("AVAILABLE", "Available"),
    FilterChipData("RESERVED", "Reserved"),
    FilterChipData("COMPLETED", "Completed"),
    FilterChipData("EXPIRED", "Expired")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    navController: NavController,
    viewModel: MyListingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Listings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                // ── 1. First load → fullscreen spinner ──────────────
                state.isLoading && state.allListings.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // ── 2. First load error → fullscreen error + Retry ──
                state.error != null && state.allListings.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = "Failed to load listings",
                        subtitle = state.error,
                        actionLabel = "Retry",
                        onAction = { viewModel.onEvent(MyListingsEvent.LoadListings) }
                    )
                }

                // ── 3. Normal → filters always visible + content ────
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Search bar
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.onEvent(MyListingsEvent.SearchQueryChanged(it)) },
                            placeholder = "Search by title, location...",
                            modifier = Modifier.padding(16.dp)
                        )

                        // Status chips (row 1)
                        FilterChipsRow(
                            filters = statusFilters,
                            selectedFilter = state.selectedStatusTab?.name,
                            onFilterSelected = { value ->
                                val status = value?.let { ListingStatus.valueOf(it) }
                                viewModel.onEvent(MyListingsEvent.StatusTabChanged(status))
                            },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Category chips (row 2)
                        FilterChipsRow(
                            filters = state.availableCategoryFilters,
                            selectedFilter = state.selectedCategory,
                            onFilterSelected = { viewModel.onEvent(MyListingsEvent.CategoryFilterChanged(it)) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Results count + Sort
                        ResultsCountAndSort(
                            count = state.filteredListings.size,
                            itemName = "listing",
                            selectedSort = state.selectedSort,
                            onSortSelected = { viewModel.onEvent(MyListingsEvent.SortOptionChanged(it)) },
                            sortOptions = state.availableSortOptions,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Inline error (after data loaded but action failed)
                        state.error?.let {
                            ErrorBanner(
                                message = it,
                                onDismiss = { viewModel.onEvent(MyListingsEvent.ClearError) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        // Content area
                        when {
                            state.filteredListings.isEmpty() -> {
                                EmptyState(
                                    icon = if (state.allListings.isEmpty()) Icons.Default.PostAdd else Icons.Default.FilterAlt,
                                    title = if (state.allListings.isEmpty()) "No listings yet"
                                    else "No listings match your filters",
                                    subtitle = if (state.allListings.isEmpty())
                                        "Create your first listing to get started"
                                    else "Try adjusting your search or filters"
                                )
                            }

                            else -> {
                                PullToRefreshBox(
                                    isRefreshing = state.isRefreshing,
                                    onRefresh = { viewModel.onEvent(MyListingsEvent.RefreshListings) }
                                ) {
                                    
                                        LazyColumn(
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(state.filteredListings, key = { it.id }) { listing ->
                                                var showEditQty by remember { mutableStateOf(false) }
                                                var showDelete by remember { mutableStateOf(false) }

                                                ListingCard(
                                                    listing = listing,
                                                    modifier = Modifier.clickable {
                                                        navController.navigate("listing_detail/${listing.id}")
                                                    },
                                                    showGroceryInfo = false,
                                                    secondaryActions = if (listing.status == ListingStatus.AVAILABLE) {
                                                        {
                                                            TextButton(onClick = { showEditQty = true }) {
                                                                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                                                                Spacer(Modifier.width(4.dp))
                                                                Text("Edit Qty", style = MaterialTheme.typography.labelMedium)
                                                            }
                                                            TextButton(
                                                                onClick = { showDelete = true },
                                                                colors = ButtonDefaults.textButtonColors(
                                                                    contentColor = MaterialTheme.colorScheme.error
                                                                )
                                                            ) {
                                                                Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                                                                Spacer(Modifier.width(4.dp))
                                                                Text("Delete", style = MaterialTheme.typography.labelMedium)
                                                            }
                                                        }
                                                    } else null
                                                )

                                                if (showEditQty) {
                                                    EditQuantityDialog(
                                                        currentQuantity = listing.quantity,
                                                        unit = listing.unit,
                                                        onDismiss = { showEditQty = false },
                                                        onConfirm = { newQty ->
                                                            showEditQty = false
                                                            viewModel.onEvent(
                                                                MyListingsEvent.UpdateListingQuantity(listing.id, newQty)
                                                            )
                                                        }
                                                    )
                                                }

                                                if (showDelete) {
                                                    ConfirmDialog(
                                                        title = "Delete Listing",
                                                        message = "Delete \"${listing.title}\"? This cannot be undone.",
                                                        confirmLabel = "Delete",
                                                        isDestructive = true,
                                                        onConfirm = {
                                                            showDelete = false
                                                            viewModel.onEvent(MyListingsEvent.DeleteListing(listing.id))
                                                        },
                                                        onDismiss = { showDelete = false }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Quantity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Current: $currentQuantity $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it; error = null },
                    label = { Text("New Quantity") },
                    suffix = { Text(unit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newQty = quantity.toIntOrNull()
                when {
                    newQty == null -> error = "Invalid number"
                    newQty <= 0 -> error = "Must be greater than 0"
                    newQty == currentQuantity -> error = "Same as current"
                    else -> onConfirm(newQty)
                }
            }) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}