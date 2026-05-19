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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                InventoryStatusBadge(item.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Details grid ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoRow(Icons.Default.Scale, stringResource(R.string.listing_quantity), "${item.quantity} ${item.unit}")
                    InfoRow(Icons.Default.DateRange, stringResource(R.string.inventory_step_received), DateTimeUtils.formatDate(item.receivedAt))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val expiryColor = if (item.status == InventoryStatus.ACTIVE)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                    InfoRow(
                        icon = Icons.Default.Event,
                        label = stringResource(R.string.inventory_step_expires),
                        value = DateTimeUtils.formatDate(item.expiryDate),
                        valueColor = expiryColor
                    )
                    item.distributedAt?.let {
                        InfoRow(Icons.Default.CheckCircle, stringResource(R.string.inventory_step_distributed), DateTimeUtils.formatDate(it))
                    }
                }
            }

            // ── Action / Status notice ────────────────────────────────────
            when (item.status) {
                InventoryStatus.ACTIVE -> {
                    if (onDistribute != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Button(
                            onClick = { showDistributeDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.VolunteerActivism, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_mark_distributed))
                        }
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
            title = stringResource(R.string.action_mark_distributed),
            message = stringResource(R.string.mark_distributed_confirm, item.productName),
            confirmLabel = stringResource(R.string.ok),
            onConfirm = { onDistribute?.invoke(item.id); showDistributeDialog = false },
            onDismiss = { showDistributeDialog = false }
        )
    }
}
