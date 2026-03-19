// ═══════════════════════════════════════════════════════════════════════════════
// RequestCard.kt — Unified pickup request card
// Used on: ManageRequests (Grocery), MyRequests (NGO), Transactions (Admin)
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus

/**
 * Unified request card component.
 *
 * @param request The pickup request data
 * @param viewMode Who is viewing: GROCERY (manage), NGO (my requests), ADMIN (readonly)
 * @param onApprove Grocery action
 * @param onReject Grocery action
 * @param onMarkReady Grocery action
 * @param onCancel NGO action
 * @param onConfirmPickup NGO action
 * @param onViewPhoto View proof photo fullscreen
 */
enum class RequestViewMode { GROCERY, NGO, ADMIN }

@Composable
fun RequestCard(
    request: PickupRequest,
    viewMode: RequestViewMode,
    onApprove: ((String) -> Unit)? = null,
    onReject: ((String) -> Unit)? = null,
    onMarkReady: ((String) -> Unit)? = null,
    onCancel: ((String) -> Unit)? = null,
    onConfirmPickup: ((String) -> Unit)? = null,
    onViewPhoto: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header: Title + Badge ───────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.listingTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (viewMode) {
                            RequestViewMode.GROCERY -> "From: ${request.ngoName}"
                            RequestViewMode.NGO     -> "At: ${request.groceryName}"
                            RequestViewMode.ADMIN   -> "${request.ngoName} → ${request.groceryName}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                PickupStatusBadge(status = request.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Details ─────────────────────────────────────────────────
            InfoRow(Icons.Default.Inventory2, "Item", request.listingCategory)
            InfoRow(Icons.Default.ShoppingCart, "Qty", "${request.requestedQuantity}")
            InfoRow(Icons.Default.DateRange, "Date", request.pickupDate)
            InfoRow(Icons.Default.Schedule, "Time", request.pickupTime)

            request.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    InfoRow(Icons.Default.StickyNote2, "Notes", notes)
                }
            }

            // ── Proof Photo (Completed requests) ────────────────────────
            if (request.status == PickupRequestStatus.COMPLETED && request.proofPhotoUrl != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Proof Photo",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { onViewPhoto?.invoke(request.proofPhotoUrl!!) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = request.proofPhotoUrl,
                            contentDescription = "Proof Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "View",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // ── Action Buttons ──────────────────────────────────────────
            when (viewMode) {
                RequestViewMode.GROCERY -> GroceryActions(
                    request = request,
                    onApprove = { showConfirmDialog = "approve" },
                    onReject = { showConfirmDialog = "reject" },
                    onMarkReady = { showConfirmDialog = "ready" }
                )
                RequestViewMode.NGO -> NgoActions(
                    request = request,
                    onCancel = { showConfirmDialog = "cancel" },
                    onConfirmPickup = { onConfirmPickup?.invoke(request.id) }
                )
                RequestViewMode.ADMIN -> { /* Read-only, no actions */ }
            }
        }
    }

    // ── Confirmation Dialogs ────────────────────────────────────────────
    showConfirmDialog?.let { action ->
        val (title, message, label, destructive) = when (action) {
            "approve" -> listOf(
                "Approve Request?",
                "Approve pickup from ${request.ngoName} for ${request.requestedQuantity} items?",
                "Approve",
                "false"
            )
            "reject" -> listOf(
                "Reject Request?",
                "Reject request from ${request.ngoName}? Quantity will be restored.",
                "Reject",
                "true"
            )
            "ready" -> listOf(
                "Mark Ready?",
                "Mark as ready for pickup? ${request.ngoName} will be notified.",
                "Mark Ready",
                "false"
            )
            "cancel" -> listOf(
                "Cancel Request?",
                "Are you sure you want to cancel this pickup request?",
                "Cancel Request",
                "true"
            )
            else -> return@let
        }

        ConfirmDialog(
            title = title,
            message = message,
            confirmLabel = label,
            isDestructive = destructive == "true",
            onConfirm = {
                when (action) {
                    "approve" -> onApprove?.invoke(request.id)
                    "reject" -> onReject?.invoke(request.id)
                    "ready" -> onMarkReady?.invoke(request.id)
                    "cancel" -> onCancel?.invoke(request.id)
                }
                showConfirmDialog = null
            },
            onDismiss = { showConfirmDialog = null }
        )
    }
}

@Composable
private fun GroceryActions(
    request: PickupRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onMarkReady: () -> Unit
) {
    when (request.status) {
        PickupRequestStatus.PENDING -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Approve")
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
        PickupRequestStatus.APPROVED -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick = onMarkReady,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(Icons.Default.Done, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mark Ready for Pickup")
            }
        }
        PickupRequestStatus.READY -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HourglassTop,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Waiting for ${request.ngoName} to confirm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        else -> { }
    }
}

@Composable
private fun NgoActions(
    request: PickupRequest,
    onCancel: () -> Unit,
    onConfirmPickup: () -> Unit
) {
    when (request.status) {
        PickupRequestStatus.PENDING -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Cancel, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cancel Request")
            }
        }
        PickupRequestStatus.READY -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick = onConfirmPickup,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Confirm Pickup")
            }
        }
        else -> { }
    }
}