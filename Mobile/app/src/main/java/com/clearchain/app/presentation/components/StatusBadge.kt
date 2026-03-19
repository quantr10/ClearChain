// ═══════════════════════════════════════════════════════════════════════════════
// StatusBadge.kt — Unified status badges for all domain types
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.domain.model.*
import com.clearchain.app.ui.theme.CategoryColors
import com.clearchain.app.ui.theme.StatusColors

data class BadgeStyle(
    val backgroundColor: Color,
    val contentColor: Color,
    val label: String,
    val icon: ImageVector? = null
)

@Composable
fun StatusBadge(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    it,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = contentColor
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
fun PickupStatusBadge(status: PickupRequestStatus) {
    val style = when (status) {
        PickupRequestStatus.PENDING   -> BadgeStyle(StatusColors.PendingBg, StatusColors.PendingOnBg, "Pending", Icons.Default.Schedule)
        PickupRequestStatus.APPROVED  -> BadgeStyle(StatusColors.ApprovedBg, StatusColors.ApprovedOnBg, "Approved", Icons.Default.ThumbUp)
        PickupRequestStatus.READY     -> BadgeStyle(StatusColors.ReadyBg, StatusColors.ReadyOnBg, "Ready", Icons.Default.CheckCircle)
        PickupRequestStatus.REJECTED  -> BadgeStyle(StatusColors.RejectedBg, StatusColors.RejectedOnBg, "Rejected", Icons.Default.Cancel)
        PickupRequestStatus.COMPLETED -> BadgeStyle(StatusColors.CompletedBg, StatusColors.CompletedOnBg, "Completed", Icons.Default.Done)
    }
    StatusBadge(style.label, style.backgroundColor, style.contentColor, style.icon)
}

@Composable
fun ListingStatusBadge(status: ListingStatus) {
    val style = when (status) {
        ListingStatus.AVAILABLE -> BadgeStyle(StatusColors.AvailableBg, StatusColors.AvailableOnBg, "Available", Icons.Default.CheckCircle)
        ListingStatus.RESERVED  -> BadgeStyle(StatusColors.ReservedBg, StatusColors.ReservedOnBg, "Reserved", Icons.Default.Lock)
        ListingStatus.COMPLETED -> BadgeStyle(StatusColors.CompletedBg, StatusColors.CompletedOnBg, "Completed", Icons.Default.Done)
        ListingStatus.EXPIRED   -> BadgeStyle(StatusColors.ExpiredBg, StatusColors.ExpiredOnBg, "Expired", Icons.Default.Warning)
    }
    StatusBadge(style.label, style.backgroundColor, style.contentColor, style.icon)
}

@Composable
fun InventoryStatusBadge(status: InventoryStatus) {
    val style = when (status) {
        InventoryStatus.ACTIVE      -> BadgeStyle(StatusColors.AvailableBg, StatusColors.AvailableOnBg, "Active", Icons.Default.Inventory)
        InventoryStatus.DISTRIBUTED -> BadgeStyle(StatusColors.DistributedBg, StatusColors.DistributedOnBg, "Distributed", Icons.Default.Done)
        InventoryStatus.EXPIRED     -> BadgeStyle(StatusColors.ExpiredBg, StatusColors.ExpiredOnBg, "Expired", Icons.Default.Warning)
    }
    StatusBadge(style.label, style.backgroundColor, style.contentColor, style.icon)
}

@Composable
fun CategoryBadge(category: FoodCategory) {
    val color = when (category) {
        FoodCategory.FRUITS     -> CategoryColors.Fruits
        FoodCategory.VEGETABLES -> CategoryColors.Vegetables
        FoodCategory.DAIRY      -> CategoryColors.Dairy
        FoodCategory.BAKERY     -> CategoryColors.Bakery
        FoodCategory.MEAT       -> CategoryColors.Meat
        FoodCategory.SEAFOOD    -> CategoryColors.Seafood
        FoodCategory.PACKAGED   -> CategoryColors.Packaged
        FoodCategory.BEVERAGES  -> CategoryColors.Beverages
        FoodCategory.OTHER      -> CategoryColors.Other
    }
    StatusBadge(
        label = category.displayName(),
        backgroundColor = color.copy(alpha = 0.12f),
        contentColor = color
    )
}