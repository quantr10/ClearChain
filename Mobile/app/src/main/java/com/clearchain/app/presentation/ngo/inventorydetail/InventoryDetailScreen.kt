package com.clearchain.app.presentation.ngo.inventorydetail

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.InventoryApi
import com.clearchain.app.data.remote.api.PickupRequestApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══ State ═══
data class InventoryDetailState(
    val item: InventoryItem? = null,
    val relatedRequest: PickupRequest? = null,
    val isLoading: Boolean = false,
    val isLoadingRequest: Boolean = false,
    val error: String? = null
)

// ═══ ViewModel ═══
@HiltViewModel
class InventoryDetailViewModel @Inject constructor(
    private val inventoryApi: InventoryApi,
    private val pickupRequestApi: PickupRequestApi
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryDetailState())
    val state: StateFlow<InventoryDetailState> = _state.asStateFlow()

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = inventoryApi.getInventoryItemById(itemId)
                val item = response.data.toDomain()
                _state.update { it.copy(item = item, isLoading = false) }

                // Load related pickup request if available
                item.pickupRequestId?.let { requestId ->
                    loadRelatedRequest(requestId)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load item", isLoading = false) }
            }
        }
    }

    private fun loadRelatedRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRequest = true) }
            try {
                val response = pickupRequestApi.getPickupRequestById(requestId)
                _state.update { it.copy(relatedRequest = response.data.toDomain(), isLoadingRequest = false) }
            } catch (e: Exception) {
                // Non-fatal — just don't show request info
                _state.update { it.copy(isLoadingRequest = false) }
            }
        }
    }
}

// ═══ Screen ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    itemId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRequestDetail: (String) -> Unit = {},
    viewModel: InventoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(itemId) { viewModel.loadItem(itemId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = "Failed to load item",
                        subtitle = state.error,
                        actionLabel = "Retry",
                        onAction = { viewModel.loadItem(itemId) }
                    )
                }
                state.item != null -> {
                    val item = state.item!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── Header ─────────────────────────────
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.productName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    item.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            InventoryStatusBadge(status = item.status)
                        }

                        HorizontalDivider()

                        // ── Quantity Card ──────────────────────
                        SectionHeader("Quantity")
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Scale, null, Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "${item.quantity} ${item.unit}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // ── Timeline ──────────────────────────
                        SectionHeader("Timeline")
                        InfoRow(Icons.Default.DateRange, "Received",
                            DateTimeUtils.formatDate(item.receivedAt))
                        InfoRow(Icons.Default.CalendarToday, "Expires",
                            DateTimeUtils.formatDate(item.expiryDate),
                            valueColor = if (item.status == InventoryStatus.ACTIVE)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface)
                        item.distributedAt?.let {
                            InfoRow(Icons.Default.CheckCircle, "Distributed",
                                DateTimeUtils.formatDate(it),
                                valueColor = MaterialTheme.colorScheme.primary)
                        }

                        // ── Related Pickup Request ────────────
                        state.relatedRequest?.let { request ->
                            HorizontalDivider()
                            SectionHeader("Pickup Request")

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToRequestDetail(request.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Request header
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            request.listingTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        PickupStatusBadge(request.status)
                                    }

                                    // Grocery info
                                    InfoRow(Icons.Default.Store, "From", request.groceryName)

                                    // Pickup date/time
                                    InfoRow(Icons.Default.DateRange, "Pickup Date", request.pickupDate)
                                    InfoRow(Icons.Default.Schedule, "Pickup Time", request.pickupTime)

                                    // Requested quantity
                                    InfoRow(Icons.Default.ShoppingCart, "Requested Qty",
                                        "${request.requestedQuantity}")

                                    // Notes
                                    request.notes?.let { notes ->
                                        if (notes.isNotBlank()) {
                                            InfoRow(Icons.Default.StickyNote2, "Notes", notes)
                                        }
                                    }

                                    // Tap hint
                                    Text(
                                        "Tap to view full request details →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Loading indicator for request
                        if (state.isLoadingRequest) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Loading request details...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // ── Status Info Card ──────────────────
                        when (item.status) {
                            InventoryStatus.ACTIVE -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Info, null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text("This item is active and ready to be distributed to beneficiaries.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }
                            InventoryStatus.DISTRIBUTED -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.VolunteerActivism, null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Text("This item has been distributed successfully.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            }
                            InventoryStatus.EXPIRED -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, null,
                                            tint = MaterialTheme.colorScheme.error)
                                        Text("This item has expired — dispose of properly.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}