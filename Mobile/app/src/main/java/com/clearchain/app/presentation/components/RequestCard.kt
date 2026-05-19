package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

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
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf<String?>(null) }

    // Parse category string → FoodCategory enum for proper CategoryBadge styling
    val foodCategory: FoodCategory = remember(request.listingCategory) {
        FoodCategory.entries.find {
            it.name.equals(request.listingCategory, ignoreCase = true)
        } ?: FoodCategory.OTHER
    }

    // Expiry computation
    val daysUntilExpiry: Long? = remember(request.listingExpiryDate) {
        val raw = request.listingExpiryDate ?: return@remember null
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(raw)!!
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time
            TimeUnit.MILLISECONDS.toDays(date.time - today.time)
        } catch (_: Exception) { null }
    }

    ClearChainCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Header: title + category badge left, status badge right ──────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Product name + category badge on same row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = request.listingTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        CategoryBadge(foodCategory)
                    }
                    // To / From line
                    Text(
                        text = when (viewMode) {
                            RequestViewMode.GROCERY -> stringResource(R.string.request_to, request.ngoName)
                            RequestViewMode.NGO     -> stringResource(R.string.request_from, request.groceryName)
                            RequestViewMode.ADMIN   -> stringResource(R.string.request_admin_route, request.ngoName, request.groceryName)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                PickupStatusBadge(request.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Details ──────────────────────────────────────────────────────

            // Quantity + unit
            val qtyText = if (request.listingUnit.isNotBlank())
                "${request.requestedQuantity} ${request.listingUnit}"
            else
                "${request.requestedQuantity}"
            RequestDetailRow(
                icon = Icons.Default.ShoppingCart,
                text = qtyText,
                textColor = MaterialTheme.colorScheme.onSurface
            )

            // Expiry (only when available)
            if (daysUntilExpiry != null) {
                val expiryColor = when {
                    daysUntilExpiry <= 0L -> MaterialTheme.colorScheme.error
                    daysUntilExpiry <= 3L -> Color(0xFFE65100)
                    else                  -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val expiryText = when {
                    daysUntilExpiry < 0     -> stringResource(R.string.listing_expired_label)
                    daysUntilExpiry == 0L   -> stringResource(R.string.listing_expires_today)
                    daysUntilExpiry == 1L   -> stringResource(R.string.listing_expires_tomorrow)
                    daysUntilExpiry in 2..3 -> stringResource(R.string.listing_expires_in_days, daysUntilExpiry.toInt())
                    else -> stringResource(R.string.listing_expires_on, DateTimeUtils.formatDate(request.listingExpiryDate!!))
                }
                RequestDetailRow(
                    icon      = Icons.Default.CalendarToday,
                    text      = expiryText,
                    textColor = expiryColor,
                    bold      = daysUntilExpiry <= 3
                )
            }

            // Pickup date + time
            val timestampText = stringResource(
                R.string.label_pickup_on_at,
                DateTimeUtils.formatDate(request.pickupDate),
                request.pickupTime
            )
            RequestDetailRow(
                icon = Icons.Default.AccessTime,
                text = timestampText,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Vehicle row
            if (!request.vehicleType.isNullOrBlank()) {
                val vehicleLabel = when (request.vehicleType.lowercase()) {
                    "walk"       -> stringResource(R.string.vehicle_walk)
                    "bicycle"    -> stringResource(R.string.vehicle_bicycle)
                    "motorcycle" -> stringResource(R.string.vehicle_motorcycle)
                    "car"        -> stringResource(R.string.vehicle_car)
                    "van"        -> stringResource(R.string.vehicle_van)
                    else         -> request.vehicleType.replaceFirstChar { it.titlecase() }
                }
                RequestDetailRow(
                    icon      = Icons.Default.LocalShipping,
                    text      = vehicleLabel,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Handling flags + optional user note
            val handlingParts = buildList {
                if (request.requiresRefrigeration) add(stringResource(R.string.note_needs_refrigeration))
                if (request.isFragile)             add(stringResource(R.string.note_fragile_items))
                if (request.isHeavy)               add(stringResource(R.string.note_heavy_load))
                request.notes?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            if (handlingParts.isNotEmpty()) {
                RequestDetailRow(
                    icon      = Icons.Default.StickyNote2,
                    text      = handlingParts.joinToString(" · "),
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Actions ───────────────────────────────────────────────────────
            when (viewMode) {
                RequestViewMode.GROCERY -> GroceryRequestActions(
                    request     = request,
                    onApprove   = { showConfirmDialog = "approve" },
                    onReject    = { showConfirmDialog = "reject" },
                    onMarkReady = { showConfirmDialog = "ready" }
                )
                RequestViewMode.NGO -> NgoRequestActions(
                    request         = request,
                    onCancel        = { showConfirmDialog = "cancel" },
                    onConfirmPickup = { onConfirmPickup?.invoke(request.id) }
                )
                RequestViewMode.ADMIN -> {}
            }
        }
    }

    // ── Confirmation dialogs ──────────────────────────────────────────────────
    showConfirmDialog?.let { action ->
        val approveTitle = stringResource(R.string.dialog_approve_title)
        val approveMsg   = stringResource(R.string.dialog_approve_message, request.ngoName, request.requestedQuantity)
        val rejectTitle  = stringResource(R.string.dialog_reject_title)
        val rejectMsg    = stringResource(R.string.dialog_reject_message, request.ngoName)
        val readyTitle   = stringResource(R.string.dialog_ready_title)
        val readyMsg     = stringResource(R.string.dialog_ready_message, request.ngoName)
        val cancelTitle  = stringResource(R.string.dialog_cancel_title)
        val cancelMsg    = stringResource(R.string.dialog_cancel_message)
        val approveLabel = stringResource(R.string.approve)
        val rejectLabel  = stringResource(R.string.reject)
        val readyLabel   = stringResource(R.string.status_ready)
        val cancelLabel  = stringResource(R.string.cancel_request)
        val (title, message, label, destructive) = when (action) {
            "approve" -> listOf(approveTitle, approveMsg, approveLabel, "false")
            "reject"  -> listOf(rejectTitle,  rejectMsg,  rejectLabel,  "true")
            "ready"   -> listOf(readyTitle,   readyMsg,   readyLabel,   "false")
            "cancel"  -> listOf(cancelTitle,  cancelMsg,  cancelLabel,  "true")
            else -> return@let
        }
        ConfirmDialog(
            title         = title,
            message       = message,
            confirmLabel  = label,
            isDestructive = destructive == "true",
            onConfirm = {
                when (action) {
                    "approve" -> onApprove?.invoke(request.id)
                    "reject"  -> onReject?.invoke(request.id)
                    "ready"   -> onMarkReady?.invoke(request.id)
                    "cancel"  -> onCancel?.invoke(request.id)
                }
                showConfirmDialog = null
            },
            onDismiss = { showConfirmDialog = null }
        )
    }
}

// ── Compact detail row ────────────────────────────────────────────────────────

@Composable
private fun RequestDetailRow(
    icon:      ImageVector,
    text:      String,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    bold:      Boolean = false
) {
    Row(
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(15.dp), tint = textColor)
        Text(
            text       = text,
            style      = MaterialTheme.typography.bodySmall,
            color      = textColor,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ── Grocery-side action buttons ───────────────────────────────────────────────

@Composable
private fun GroceryRequestActions(
    request:    PickupRequest,
    onApprove:  () -> Unit,
    onReject:   () -> Unit,
    onMarkReady: () -> Unit
) {
    when (request.status) {
        PickupRequestStatus.PENDING -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick  = onApprove,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.approve))
                }
                OutlinedButton(
                    onClick  = onReject,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.reject))
                }
            }
        }
        PickupRequestStatus.APPROVED -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick  = onMarkReady,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.Done, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.mark_ready_for_pickup))
            }
        }
        PickupRequestStatus.READY -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HourglassTop, null,
                        tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        stringResource(R.string.waiting_for_confirm, request.ngoName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        else -> {}
    }
}

// ── NGO-side action buttons ───────────────────────────────────────────────────

@Composable
private fun NgoRequestActions(
    request:         PickupRequest,
    onCancel:        () -> Unit,
    onConfirmPickup: () -> Unit
) {
    when (request.status) {
        PickupRequestStatus.PENDING -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OutlinedButton(
                onClick  = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.cancel_request))
            }
        }
        PickupRequestStatus.APPROVED -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HourglassTop, null,
                        tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        stringResource(R.string.note_waiting_grocery_prepare, request.groceryName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        PickupRequestStatus.READY -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick  = onConfirmPickup,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.confirm_pickup_photo))
            }
        }
        else -> {}
    }
}
