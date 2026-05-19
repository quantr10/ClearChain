package com.clearchain.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.clearchain.app.domain.model.*
import com.clearchain.app.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun ListingCard(
    listing: Listing,
    showGroceryInfo: Boolean = false,
    onClick: (() -> Unit)? = null,
    primaryAction: (@Composable () -> Unit)? = null,
    secondaryActions: (@Composable RowScope.() -> Unit)? = null,
    topRightAction: (@Composable () -> Unit)? = null,
    onGroceryAvatarClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val daysUntilExpiry: Long = remember(listing.expiryDate) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(listing.expiryDate)!!
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time
            TimeUnit.MILLISECONDS.toDays(date.time - today.time)
        } catch (_: Exception) { Long.MAX_VALUE }
    }
    val expiryColor = when {
        daysUntilExpiry <= 0L -> MaterialTheme.colorScheme.error
        daysUntilExpiry <= 3L -> Color(0xFFE65100)
        else                  -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val expiryText = when {
        daysUntilExpiry < 0   -> stringResource(R.string.listing_expired_label)
        daysUntilExpiry == 0L -> stringResource(R.string.listing_expires_today)
        daysUntilExpiry == 1L -> stringResource(R.string.listing_expires_tomorrow)
        daysUntilExpiry in 2..3 -> stringResource(R.string.listing_expires_in_days, daysUntilExpiry.toInt())
        else -> stringResource(R.string.listing_expires_on, DateTimeUtils.formatDate(listing.expiryDate))
    }

    val urgencyBannerText: String? = when {
        daysUntilExpiry < 0L  -> stringResource(R.string.listing_expired_label)
        daysUntilExpiry == 0L -> stringResource(R.string.listing_expires_today)
        daysUntilExpiry == 1L -> stringResource(R.string.listing_expires_tomorrow)
        daysUntilExpiry <= 3L -> stringResource(R.string.listing_expiring_soon)
        else                  -> null
    }
    val urgencyBannerColor: Color = when {
        daysUntilExpiry <= 0L -> Color(0xCCB71C1C)
        daysUntilExpiry == 1L -> Color(0xCCE65100)
        else                  -> Color(0xCCF57F17)
    }

    ClearChainCard(modifier = modifier, onClick = onClick) {
        Column {
            // ── Image ─────────────────────────────────────────────────────
            val imageUrl = listing.imageUrl?.takeIf { it.isNotBlank() }
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
                        contentDescription = listing.title,
                        modifier     = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp) }
                        },
                        error = { FoodImagePlaceholder(Modifier.fillMaxSize()) }
                    )
                } else {
                    FoodImagePlaceholder(Modifier.fillMaxSize())
                }

                // Urgency banner (bottom-center)
                if (urgencyBannerText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(urgencyBannerColor)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            urgencyBannerText.uppercase(),
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }

                // Status badge overlay (top-right)
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    if (listing.isArchived) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xCC455A64)
                        ) {
                            Text(
                                text       = stringResource(R.string.tab_archived),
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White,
                                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        ListingStatusBadge(listing.status)
                    }
                }

                // Grocery avatar (bottom-left, Browse mode only)
                if (onGroceryAvatarClick != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .size(38.dp)
                            .clickable { onGroceryAvatarClick() },
                        shape           = CircleShape,
                        color           = MaterialTheme.colorScheme.primaryContainer,
                        border          = BorderStroke(2.dp, Color.White),
                        shadowElevation = 3.dp
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text       = listing.groceryName.take(1).uppercase(),
                                style      = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Name + category inline, topRightAction on the right
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text       = listing.title,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis,
                                modifier   = Modifier.weight(1f, fill = false)
                            )
                            CategoryBadge(listing.category)
                        }
                    }
                    topRightAction?.invoke()
                }

                // Description
                if (listing.description.isNotBlank()) {
                    Text(
                        listing.description,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quantity
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart, null,
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${listing.quantity} ${listing.unit}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Expiry date
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(14.dp), tint = expiryColor)
                    Text(
                        expiryText,
                        style      = MaterialTheme.typography.labelMedium,
                        color      = expiryColor,
                        fontWeight = if (daysUntilExpiry <= 3) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                // Pickup time
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule, null,
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        listing.groceryHours
                            ?: stringResource(
                                R.string.listing_pickup_from,
                                listing.pickupTimeStart,
                                listing.pickupTimeEnd
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Location + distance (browse mode, merged into one row)
                if (showGroceryInfo && listing.location.isNotBlank()) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Place, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            listing.location,
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        listing.distanceKm?.let { km ->
                            Text("·", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.Default.NearMe, null, Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "${km}km",
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (!showGroceryInfo) {
                    listing.distanceKm?.let { DistanceBadge(it) }
                }

                // Actions
                primaryAction?.invoke()
                secondaryActions?.let {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment     = Alignment.CenterVertically,
                        content               = it
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun QuantityBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(Icons.Default.Scale, null, Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(text, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun DistanceBadge(km: Double) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(Icons.Default.NearMe, null, Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("${km}km", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
