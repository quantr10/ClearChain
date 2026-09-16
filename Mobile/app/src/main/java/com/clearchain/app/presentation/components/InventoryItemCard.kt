package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.util.DateTimeUtils

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onDistribute: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDistributeDialog by remember { mutableStateOf(false) }

    ClearChainCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                ProductThumbnail(
                    imageUrl = item.photoUrl,
                    contentDescription = item.productName,
                    size = 48.dp
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    InventoryCategoryBadge(category = item.category)
                }
                Spacer(Modifier.width(8.dp))
                InventoryStatusBadge(item.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Details grid ─────────────────────────────────────────────
            val expiryColor = if (item.status == InventoryStatus.ACTIVE) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            CompactInventoryRow(
                icon = Icons.Default.Scale,
                text = "${item.quantity} ${item.unit}",
                textColor = MaterialTheme.colorScheme.onSurface
            )
            CompactInventoryRow(
                icon = Icons.Default.Event,
                text = stringResource(R.string.listing_expires_on, DateTimeUtils.formatDate(item.expiryDate)),
                textColor = expiryColor
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

            // ── Action / Status notice ────────────────────────────────────
            when (item.status) {
                InventoryStatus.ACTIVE -> {
                    if (onDistribute != null) {
                        ClearChainButton(
                            text = stringResource(R.string.action_mark_distributed),
                            onClick = { showDistributeDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.VolunteerActivism
                        )
                    }
                }
                InventoryStatus.EXPIRED -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Text(
                                stringResource(R.string.status_expired_info),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                InventoryStatus.DISTRIBUTED -> {}
            }
        }
    }

    if (showDistributeDialog) {
        ConfirmDialog(
            icon = Icons.Default.Redeem,
            title = stringResource(R.string.action_mark_distributed),
            message = stringResource(R.string.mark_distributed_confirm, item.productName),
            confirmLabel = stringResource(R.string.ok),
            onConfirm = { onDistribute?.invoke(item.id); showDistributeDialog = false },
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
