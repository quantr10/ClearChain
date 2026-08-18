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
    val titleText = when (viewMode) {
        RequestViewMode.GROCERY -> request.ngoName
        RequestViewMode.NGO -> request.groceryName
        RequestViewMode.ADMIN -> request.listingTitle
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: title + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(6.dp))
                PickupStatusBadge(request.status)
            }

            // Details

            if (request.items.isNotEmpty()) {
                RequestItemsPreview(request)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

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
                    textColor = expiryColor
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
                    text      = handlingParts.joinToString(" \u00B7 "),
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions
            when (viewMode) {
                RequestViewMode.GROCERY -> GroceryRequestActions(
                    request     = request,
                    onApprove   = { onApprove?.invoke(request.id) },
                    onReject    = { showConfirmDialog = "reject" },
                    onMarkReady = { onMarkReady?.invoke(request.id) }
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

    // Confirmation dialogs
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

// Compact detail row

@Composable
private fun RequestItemsPreview(request: PickupRequest) {
    val visibleItems = request.items.take(5)
    val hiddenCount = request.items.size - visibleItems.size

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleItems.forEach { item ->
            ProductThumbnail(
                imageUrl = item.listingPhotoUrl,
                contentDescription = item.listingTitle,
                size = 44.dp
            )
        }
        if (hiddenCount > 0) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+$hiddenCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestDetailRow(
    icon:      ImageVector,
    text:      String,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = textColor)
        Text(
            text       = text,
            style      = MaterialTheme.typography.labelSmall,
            color      = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Grocery-side action buttons

@Composable
private fun GroceryRequestActions(
    request:    PickupRequest,
    onApprove:  () -> Unit,
    onReject:   () -> Unit,
    onMarkReady: () -> Unit
) {
    when (request.status) {
        PickupRequestStatus.PENDING -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClearChainButton(
                    text = stringResource(R.string.approve),
                    onClick  = onApprove,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Check
                )
                ClearChainOutlinedButton(
                    text = stringResource(R.string.reject),
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Close,
                    contentColor = MaterialTheme.colorScheme.error
                )
            }
        }
        PickupRequestStatus.APPROVED -> {
            ClearChainButton(
                text = stringResource(R.string.action_mark_ready),
                onClick  = onMarkReady,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Check
            )
        }
        PickupRequestStatus.READY -> {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ClearChainButtonDefaults.Height)
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HourglassTop, null,
                        tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(ClearChainButtonDefaults.IconSize)
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

// NGO-side action buttons

@Composable
private fun NgoRequestActions(
    request:         PickupRequest,
    onCancel:        () -> Unit,
    onConfirmPickup: () -> Unit
) {
    when (request.status) {
        PickupRequestStatus.PENDING -> {
            ClearChainOutlinedButton(
                text = stringResource(R.string.cancel_request),
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Cancel,
                contentColor = MaterialTheme.colorScheme.error,
                fillMaxWidth = true
            )
        }
        PickupRequestStatus.APPROVED -> {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ClearChainButtonDefaults.Height)
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HourglassTop, null,
                        tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(ClearChainButtonDefaults.IconSize)
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
            ClearChainButton(
                text = stringResource(R.string.confirm_pickup_photo),
                onClick  = onConfirmPickup,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.CameraAlt
            )
        }
        else -> {}
    }
}
