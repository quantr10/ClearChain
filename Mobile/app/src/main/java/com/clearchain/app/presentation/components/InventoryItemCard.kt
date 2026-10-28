package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.clearchain.app.R
import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.util.DateTimeUtils

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    modifier: Modifier = Modifier,
    onDistribute: ((String) -> Unit)? = null,
    /** This item's distribute call is in flight: the confirm dialog spins until it settles. */
    isDistributing: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var showDistributeDialog by remember { mutableStateOf(false) }
    DismissWhenFinished(isDistributing) { showDistributeDialog = false }

    val daysUntilExpiry = remember(item.expiryDate) { daysUntil(item.expiryDate) }
    // A distributed item has left the shelf, so its expiry no longer needs a warning.
    val tracksExpiry = item.status != InventoryStatus.DISTRIBUTED

    ClearChainCard(modifier = modifier, onClick = onClick) {
        Column {
            // ── Photo, same layout as ListingCard ────────────────────────────
            val imageUrl = item.photoUrl?.takeIf { it.isNotBlank() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                if (imageUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl).crossfade(true).build(),
                        contentDescription = item.productName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = { FoodImagePlaceholder(Modifier.fillMaxSize()) },
                        error = { FoodImagePlaceholder(Modifier.fillMaxSize()) }
                    )
                } else {
                    FoodImagePlaceholder(Modifier.fillMaxSize())
                }

                if (tracksExpiry) {
                    ExpiryUrgencyBanner(daysUntilExpiry, Modifier.align(Alignment.BottomCenter))
                }

                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    InventoryStatusBadge(item.status)
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ── Name + category ──────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    InventoryCategoryBadge(category = item.category)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Details grid ─────────────────────────────────────────────
                CompactInventoryRow(
                    icon = Icons.Default.Scale,
                    text = "${item.quantity} ${item.unit}",
                    textColor = MaterialTheme.colorScheme.onSurface
                )
                CompactInventoryRow(
                    icon = Icons.Default.Event,
                    text = if (tracksExpiry) {
                        expiryLabel(daysUntilExpiry, item.expiryDate)
                    } else {
                        stringResource(R.string.listing_expires_on, DateTimeUtils.formatDate(item.expiryDate))
                    },
                    textColor = if (tracksExpiry) expiryTextColor(daysUntilExpiry) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                CompactInventoryRow(
                    icon = Icons.Default.Inventory,
                    text = "${stringResource(R.string.inventory_step_received)}: ${DateTimeUtils.formatDate(item.receivedAt)}"
                )
                item.distributedAt?.let {
                    CompactInventoryRow(
                        icon = Icons.Default.CheckCircle,
                        text = "${stringResource(R.string.inventory_step_distributed)}: ${DateTimeUtils.formatDate(it)}"
                    )
                }

                // ── Action / Status notice ───────────────────────────────────
                when (item.status) {
                    InventoryStatus.ACTIVE -> {
                        if (onDistribute != null) {
                            ClearChainButton(
                                text = stringResource(R.string.action_mark_distributed),
                                onClick = { showDistributeDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isDistributing,
                                icon = Icons.Default.VolunteerActivism
                            )
                        }
                    }
                    // The red "Expired" strip on the photo already says it.
                    InventoryStatus.EXPIRED, InventoryStatus.DISTRIBUTED -> {}
                }
            }
        }
    }

    if (showDistributeDialog) {
        ConfirmDialog(
            icon = Icons.Default.Redeem,
            title = stringResource(R.string.action_mark_distributed),
            message = stringResource(R.string.mark_distributed_confirm, item.productName),
            confirmLabel = stringResource(R.string.ok),
            confirmLoading = isDistributing,
            onConfirm = { onDistribute?.invoke(item.id) },
            onDismiss = { showDistributeDialog = false }
        )
    }
}

@Composable
private fun CompactInventoryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = textColor)
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
