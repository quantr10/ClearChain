package com.clearchain.app.presentation.ngo.listingdetail

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.clearchain.app.domain.model.ListingStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.DateTimeUtils

// ═══ Screen ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    listingId: String,
    onNavigateBack: () -> Unit,
    onRequestPickup: (String) -> Unit,
    viewModel: ListingDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(listingId) { viewModel.loadListing(listingId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listing Details") },
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
                        title = "Failed to load listing",
                        subtitle = state.error,
                        actionLabel = "Retry",
                        onAction = { viewModel.loadListing(listingId) }
                    )
                }
                state.listing != null -> {
                    val listing = state.listing!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // ── Image ──────────────────────────────
                        listing.imageUrl?.let { url ->
                            if (url.isNotBlank()) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = listing.title,
                                    modifier = Modifier.fillMaxWidth().height(250.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ── Title + Status ─────────────────
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    listing.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                ListingStatusBadge(listing.status)
                            }

                            // ── Category + Quantity ─────────────
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CategoryBadge(listing.category)
                                Text(
                                    "${listing.quantity} ${listing.unit}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // ── Description ────────────────────
                            if (listing.description.isNotBlank()) {
                                Text(
                                    listing.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider()

                            // ── Details ────────────────────────
                            SectionHeader("Details")
                            InfoRow(Icons.Default.CalendarToday, "Expiry Date",
                                DateTimeUtils.formatDate(listing.expiryDate),
                                valueColor = MaterialTheme.colorScheme.error)
                            InfoRow(Icons.Default.Schedule, "Pickup Window",
                                "${listing.pickupTimeStart} – ${listing.pickupTimeEnd}")
                            listing.distanceKm?.let {
                                InfoRow(Icons.Default.NearMe, "Distance",
                                    "${it} km away",
                                    valueColor = MaterialTheme.colorScheme.primary)
                            }

                            HorizontalDivider()

                            // ── Grocery Info ───────────────────
                            SectionHeader("Grocery Store")
                            InfoRow(Icons.Default.Store, "Name", listing.groceryName)
                            InfoRow(Icons.Default.Place, "Location", listing.location)

                            // ── Group Summary ──────────────────
                            listing.groupSummary?.let { group ->
                                HorizontalDivider()
                                SectionHeader("Availability")
                                InfoRow(Icons.Default.CheckCircle, "Available",
                                    "${group.totalAvailable}")
                            }

                            // ── Request Pickup Button ──────────
                            if (listing.status == ListingStatus.AVAILABLE) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { onRequestPickup(listing.id) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ShoppingCart, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Request Pickup", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // ── Timestamp ──────────────────────
                            Text(
                                "Posted ${DateTimeUtils.getTimeAgo(listing.createdAt)}",
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
}