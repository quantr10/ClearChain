// ═══════════════════════════════════════════════════════════════════════════════
// MyListingsScreen.kt - UPDATED WITH EDIT QUANTITY FEATURE
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.grocery.mylistings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.ListingStatus
import com.clearchain.app.domain.model.displayName
import com.clearchain.app.presentation.components.FilterSection
import com.clearchain.app.util.DateTimeUtils
import com.clearchain.app.util.UiEvent

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
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
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
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(MyListingsEvent.RefreshListings) },
                        enabled = !state.isRefreshing
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.onEvent(MyListingsEvent.LoadListings) }) {
                            Text("Retry")
                        }
                    }
                }

                state.allListings.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📦",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No listings yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create your first listing to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        StatusTabs(
                            selectedStatus = state.selectedStatusTab,
                            statusCounts = state.getStatusCounts(),
                            onStatusSelected = { status ->
                                viewModel.onEvent(MyListingsEvent.StatusTabChanged(status))
                            }
                        )

                        FilterSection(
                            searchQuery = state.searchQuery,
                            onSearchQueryChange = {
                                viewModel.onEvent(MyListingsEvent.SearchQueryChanged(it))
                            },
                            searchPlaceholder = "Search by title, location...",
                            selectedSort = state.selectedSort,
                            onSortSelected = {
                                viewModel.onEvent(MyListingsEvent.SortOptionChanged(it))
                            },
                            sortOptions = state.availableSortOptions,
                            filterChips = state.availableCategoryFilters,
                            selectedFilter = state.selectedCategory,
                            onFilterSelected = {
                                viewModel.onEvent(MyListingsEvent.CategoryFilterChanged(it))
                            },
                            resultsCount = state.filteredListings.size,
                            itemName = "listing"
                        )

                        if (state.filteredListings.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "🔍",
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Text(
                                        text = "No listings match your filters",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Try adjusting your search or filters",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.filteredListings, key = { it.id }) { listing ->
                                    ListingCard(
                                        listing = listing,
                                        onDelete = {
                                            viewModel.onEvent(MyListingsEvent.DeleteListing(listing.id))
                                        },
                                        onUpdateQuantity = { newQuantity ->
                                            viewModel.onEvent(
                                                MyListingsEvent.UpdateListingQuantity(
                                                    listing.id,
                                                    newQuantity
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STATUS TABS COMPONENT
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StatusTabs(
    selectedStatus: ListingStatus?,
    statusCounts: Map<ListingStatus?, Int>,
    onStatusSelected: (ListingStatus?) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = when (selectedStatus) {
            null -> 0
            ListingStatus.AVAILABLE -> 1
            ListingStatus.RESERVED -> 2
            ListingStatus.COMPLETED -> 3
            ListingStatus.EXPIRED -> 4
        },
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        divider = {}
    ) {
        Tab(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedStatus == null) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${statusCounts[null] ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedStatus == null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        )

        Tab(
            selected = selectedStatus == ListingStatus.AVAILABLE,
            onClick = { onStatusSelected(ListingStatus.AVAILABLE) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Available",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedStatus == ListingStatus.AVAILABLE) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${statusCounts[ListingStatus.AVAILABLE] ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedStatus == ListingStatus.AVAILABLE) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        )

        Tab(
            selected = selectedStatus == ListingStatus.RESERVED,
            onClick = { onStatusSelected(ListingStatus.RESERVED) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Reserved",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedStatus == ListingStatus.RESERVED) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${statusCounts[ListingStatus.RESERVED] ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedStatus == ListingStatus.RESERVED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        )

        Tab(
            selected = selectedStatus == ListingStatus.COMPLETED,
            onClick = { onStatusSelected(ListingStatus.COMPLETED) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedStatus == ListingStatus.COMPLETED) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${statusCounts[ListingStatus.COMPLETED] ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedStatus == ListingStatus.COMPLETED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        )

        Tab(
            selected = selectedStatus == ListingStatus.EXPIRED,
            onClick = { onStatusSelected(ListingStatus.EXPIRED) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Expired",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedStatus == ListingStatus.EXPIRED) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${statusCounts[ListingStatus.EXPIRED] ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedStatus == ListingStatus.EXPIRED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LISTING CARD COMPONENT
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ListingCard(
    listing: Listing,
    onDelete: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditQuantityDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = listing.status.displayName())
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = listing.category.displayName(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Scale,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${listing.quantity} ${listing.unit}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = listing.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Expires: ${DateTimeUtils.formatDate(listing.expiryDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Pickup: ${listing.pickupTimeStart} - ${listing.pickupTimeEnd}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions - Show Edit and Delete for AVAILABLE listings
            if (listing.status == ListingStatus.AVAILABLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showEditQuantityDialog = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Qty")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }

            Text(
                text = "Created ${DateTimeUtils.getTimeAgo(listing.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Listing") },
            text = { Text("Are you sure you want to delete \"${listing.title}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditQuantityDialog) {
        EditQuantityDialog(
            currentQuantity = listing.quantity,
            unit = listing.unit,
            onDismiss = { showEditQuantityDialog = false },
            onConfirm = { newQuantity ->
                showEditQuantityDialog = false
                onUpdateQuantity(newQuantity)
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EDIT QUANTITY DIALOG
// ═══════════════════════════════════════════════════════════════════════════════

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
            Column {
                Text(
                    text = "Current quantity: $currentQuantity $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                        error = null
                    },
                    label = { Text("New Quantity") },
                    suffix = { Text(unit) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newQty = quantity.toIntOrNull()
                    when {
                        newQty == null -> error = "Invalid number"
                        newQty <= 0 -> error = "Must be greater than 0"
                        newQty == currentQuantity -> error = "Same as current"
                        else -> onConfirm(newQty)
                    }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StatusBadge(status: String) {
    val color = when (status.lowercase()) {
        "available" -> MaterialTheme.colorScheme.primary
        "reserved" -> MaterialTheme.colorScheme.tertiary
        "completed" -> MaterialTheme.colorScheme.secondary
        "expired" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}