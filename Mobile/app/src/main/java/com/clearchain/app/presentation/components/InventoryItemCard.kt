// ═══════════════════════════════════════════════════════════════════════════════
// InventoryItemCard.kt — Unified inventory card
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.util.DateTimeUtils

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onDistribute: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDistributeDialog by remember { mutableStateOf(false) }

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
            // ── Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                InventoryStatusBadge(status = item.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Details ─────────────────────────────────────────────────
            InfoRow(Icons.Default.ShoppingCart, "Qty", "${item.quantity} ${item.unit}")
            InfoRow(Icons.Default.DateRange, "Received", DateTimeUtils.formatDate(item.receivedAt))
            InfoRow(
                icon = Icons.Default.CalendarToday,
                label = "Expires",
                value = DateTimeUtils.formatDate(item.expiryDate),
                valueColor = if (item.status == InventoryStatus.ACTIVE)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
                iconTint = if (item.status == InventoryStatus.ACTIVE)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            item.distributedAt?.let {
                InfoRow(Icons.Default.CheckCircle, "Distributed", DateTimeUtils.formatDate(it))
            }

            // ── Action Button ───────────────────────────────────────────
            if (item.status == InventoryStatus.ACTIVE && onDistribute != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Button(
                    onClick = { showDistributeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.VolunteerActivism, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as Distributed")
                }
            }

            // ── Expired Warning ─────────────────────────────────────────
            if (item.status == InventoryStatus.EXPIRED) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "This item has expired — dispose of properly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    if (showDistributeDialog) {
        ConfirmDialog(
            title = "Mark as Distributed?",
            message = "Confirm that ${item.productName} has been distributed to beneficiaries?",
            confirmLabel = "Confirm",
            onConfirm = {
                onDistribute?.invoke(item.id)
                showDistributeDialog = false
            },
            onDismiss = { showDistributeDialog = false }
        )
    }
}