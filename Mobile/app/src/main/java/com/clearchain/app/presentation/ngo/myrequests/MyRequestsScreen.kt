package com.clearchain.app.presentation.ngo.myrequests

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.presentation.components.FilterSection
import com.clearchain.app.presentation.components.PhotoPickerDialog
import com.clearchain.app.presentation.components.FullPhotoDialog
import com.clearchain.app.presentation.components.UploadErrorDialog
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MyRequestsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                else -> {}
            }
        }
    }

    // ✅ Loading Overlay with Attempt Counter
    if (state.isUploading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Uploading photo...")
                    if (state.uploadAttempts > 1) {
                        Text(
                            text = "Attempt ${state.uploadAttempts} of 3",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // ✅ Upload Error Dialog with Retry
    state.uploadError?.let { errorMessage ->
        UploadErrorDialog(
            errorMessage = errorMessage,
            canRetry = state.uploadAttempts < 3,
            onRetry = {
                viewModel.onEvent(MyRequestsEvent.RetryFailedUpload)
            },
            onDismiss = {
                viewModel.onEvent(MyRequestsEvent.DismissUploadError)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Requests") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(MyRequestsEvent.RefreshRequests) }
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                FilterSection(
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = {
                        viewModel.onEvent(MyRequestsEvent.SearchQueryChanged(it))
                    },
                    searchPlaceholder = "Search by item, grocery...",
                    selectedSort = state.selectedSort,
                    onSortSelected = {
                        viewModel.onEvent(MyRequestsEvent.SortOptionChanged(it))
                    },
                    sortOptions = state.availableSortOptions,
                    filterChips = state.availableStatusFilters,
                    selectedFilter = state.selectedStatus,
                    onFilterSelected = {
                        viewModel.onEvent(MyRequestsEvent.StatusFilterChanged(it))
                    },
                    resultsCount = state.filteredRequests.size,
                    itemName = "request"
                )

                state.error?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.onEvent(MyRequestsEvent.ClearError) }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                when {
                    state.isLoading && state.allRequests.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    state.allRequests.isEmpty() && !state.isLoading -> {
                        EmptyState(message = "No pickup requests yet")
                    }

                    state.filteredRequests.isEmpty() -> {
                        EmptyState(message = "No requests match your filters")
                    }

                    else -> {
                        RequestsList(
                            requests = state.filteredRequests,
                            onCancelRequest = { requestId ->
                                viewModel.onEvent(MyRequestsEvent.CancelRequest(requestId))
                            },
                            onConfirmPickup = { requestId, photoUri ->
                                viewModel.onEvent(
                                    MyRequestsEvent.ConfirmPickupWithPhoto(requestId, photoUri)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestsList(
    requests: List<PickupRequest>,
    onCancelRequest: (String) -> Unit,
    onConfirmPickup: (String, Uri) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { request ->
            RequestCard(
                request = request,
                onCancelRequest = onCancelRequest,
                onConfirmPickup = onConfirmPickup
            )
        }
    }
}

@Composable
private fun RequestCard(
    request: PickupRequest,
    onCancelRequest: (String) -> Unit,
    onConfirmPickup: (String, Uri) -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var showPhotoPickerDialog by remember { mutableStateOf(false) }
    var showFullPhoto by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.listingTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = request.groceryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = request.status)
            }

            HorizontalDivider()

            DetailRow(
                icon = Icons.Default.ShoppingCart,
                label = "Quantity",
                value = "${request.requestedQuantity}"
            )

            DetailRow(
                icon = Icons.Default.DateRange,
                label = "Pickup Date",
                value = request.pickupDate
            )

            DetailRow(
                icon = Icons.Default.Schedule,
                label = "Pickup Time",
                value = request.pickupTime
            )

            request.notes?.let { notes ->
                DetailRow(
                    icon = Icons.Default.Info,
                    label = "Notes",
                    value = notes
                )
            }

            // ✅ Display proof photo for completed requests
            if (request.status == PickupRequestStatus.COMPLETED && request.proofPhotoUrl != null) {
                HorizontalDivider()
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Proof Photo:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Photo thumbnail
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clickable { showFullPhoto = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box {
                            AsyncImage(
                                model = request.proofPhotoUrl,
                                contentDescription = "Proof Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Overlay with zoom icon
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ZoomIn,
                                        contentDescription = "View full",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Tap to view",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action buttons
            when (request.status) {
                PickupRequestStatus.PENDING -> {
                    HorizontalDivider()
                    Button(
                        onClick = { showCancelDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Request")
                    }
                }

                PickupRequestStatus.READY -> {
                    HorizontalDivider()
                    Button(
                        onClick = { showPhotoPickerDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Pickup")
                    }
                }

                else -> {}
            }
        }
    }

    // Dialogs
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Request?") },
            text = { Text("Are you sure you want to cancel this pickup request?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancelRequest(request.id)
                        showCancelDialog = false
                    }
                ) {
                    Text("Cancel Request", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep")
                }
            }
        )
    }

    if (showPhotoPickerDialog) {
        PhotoPickerDialog(
            onPhotoSelected = { uri ->
                onConfirmPickup(request.id, uri)
                showPhotoPickerDialog = false
            },
            onDismiss = {
                showPhotoPickerDialog = false
            }
        )
    }

    // Full photo viewer
    if (showFullPhoto && request.proofPhotoUrl != null) {
        FullPhotoDialog(
            photoUrl = request.proofPhotoUrl,
            onDismiss = { showFullPhoto = false }
        )
    }
}

@Composable
private fun StatusBadge(status: PickupRequestStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        PickupRequestStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Pending"
        )
        PickupRequestStatus.APPROVED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Approved"
        )
        PickupRequestStatus.READY -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Ready for Pickup"
        )
        PickupRequestStatus.REJECTED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Rejected"
        )
        PickupRequestStatus.COMPLETED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Completed"
        )
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📋",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}