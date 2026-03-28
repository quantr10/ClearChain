package com.clearchain.app.presentation.shared.requestdetail

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.clearchain.app.data.remote.api.PickupRequestApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══ State ═══
data class RequestDetailState(
    val request: PickupRequest? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// ═══ ViewModel ═══
@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    private val pickupRequestApi: PickupRequestApi
) : ViewModel() {

    private val _state = MutableStateFlow(RequestDetailState())
    val state: StateFlow<RequestDetailState> = _state.asStateFlow()

    fun loadRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = pickupRequestApi.getPickupRequestById(requestId)
                _state.update { it.copy(request = response.data.toDomain(), isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load request", isLoading = false) }
            }
        }
    }
}

// ═══ Screen ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    requestId: String,
    onNavigateBack: () -> Unit,
    viewModel: RequestDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showFullPhotoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(requestId) { viewModel.loadRequest(requestId) }

    // Full photo dialog
    showFullPhotoUrl?.let { url ->
        FullPhotoDialog(photoUrl = url, onDismiss = { showFullPhotoUrl = null })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Details") },
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
                        title = "Failed to load request",
                        subtitle = state.error,
                        actionLabel = "Retry",
                        onAction = { viewModel.loadRequest(requestId) }
                    )
                }
                state.request != null -> {
                    val req = state.request!!
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
                            Text(
                                req.listingTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            PickupStatusBadge(req.status)
                        }

                        // ── Status Timeline ────────────────────
                        StatusTimeline(currentStatus = req.status)

                        HorizontalDivider()

                        // ── Request Info ───────────────────────
                        SectionHeader("Request Information")
                        InfoRow(Icons.Default.Inventory2, "Item", req.listingCategory)
                        InfoRow(Icons.Default.ShoppingCart, "Quantity", "${req.requestedQuantity}")
                        InfoRow(Icons.Default.DateRange, "Pickup Date", req.pickupDate)
                        InfoRow(Icons.Default.Schedule, "Pickup Time", req.pickupTime)
                        req.notes?.let {
                            if (it.isNotBlank()) {
                                InfoRow(Icons.Default.StickyNote2, "Notes", it)
                            }
                        }

                        HorizontalDivider()

                        // ── Parties ────────────────────────────
                        SectionHeader("Grocery")
                        InfoRow(Icons.Default.Store, "Name", req.groceryName)

                        SectionHeader("NGO")
                        InfoRow(Icons.Default.VolunteerActivism, "Name", req.ngoName)

                        // ── Proof Photo ────────────────────────
                        if (req.status == PickupRequestStatus.COMPLETED && req.proofPhotoUrl != null) {
                            HorizontalDivider()
                            SectionHeader("Proof Photo")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clickable { showFullPhotoUrl = req.proofPhotoUrl },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                AsyncImage(
                                    model = req.proofPhotoUrl,
                                    contentDescription = "Proof of pickup",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Text(
                                "Tap to view full size",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // ── Timestamp ──────────────────────────
                        Text(
                            "Created ${DateTimeUtils.getTimeAgo(req.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ═══ Status Timeline Stepper ═══
@Composable
private fun StatusTimeline(currentStatus: PickupRequestStatus) {
    val steps = listOf(
        PickupRequestStatus.PENDING to "Pending",
        PickupRequestStatus.APPROVED to "Approved",
        PickupRequestStatus.READY to "Ready",
        PickupRequestStatus.COMPLETED to "Completed"
    )

    val currentIndex = steps.indexOfFirst { it.first == currentStatus }.coerceAtLeast(0)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, (_, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = when {
                            index < currentIndex -> MaterialTheme.colorScheme.primary
                            index == currentIndex -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (index < currentIndex) {
                                Icon(
                                    Icons.Default.Check, null,
                                    Modifier.size(18.dp),
                                    tint = Color.White
                                )
                            } else {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (index <= currentIndex) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index <= currentIndex)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}