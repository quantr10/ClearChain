package com.clearchain.app.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.clearchain.app.R
import com.clearchain.app.domain.model.*
import com.clearchain.app.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun ListingCard(
    listing: Listing,
    modifier: Modifier = Modifier,
    showGroceryInfo: Boolean = false,
    showPickupTime: Boolean = true,
    onClick: (() -> Unit)? = null,
    cartAction: (@Composable () -> Unit)? = null,
    secondaryActions: (@Composable RowScope.() -> Unit)? = null,
    topRightAction: (@Composable () -> Unit)? = null,
    onGroceryAvatarClick: (() -> Unit)? = null
) {
    val daysUntilExpiry: Long = remember(listing.expiryDate) { daysUntil(listing.expiryDate) }
    val expiryColor = expiryTextColor(daysUntilExpiry)
    val expiryText = expiryLabel(daysUntilExpiry, listing.expiryDate)

    ClearChainCard(modifier = modifier, onClick = onClick) {
        Column {
            // ── Image ────────────────────────────────────────────────────────
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
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = { FoodImagePlaceholder(Modifier.fillMaxSize()) },
                        error = { FoodImagePlaceholder(Modifier.fillMaxSize()) }
                    )
                } else {
                    FoodImagePlaceholder(Modifier.fillMaxSize())
                }

                ExpiryUrgencyBanner(daysUntilExpiry, Modifier.align(Alignment.BottomCenter))

                // Status badge overlay (top-right)
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    ListingStatusBadge(listing.status)
                }

                // Grocery avatar (bottom-left, Browse mode only)
                if (onGroceryAvatarClick != null) {
                    OverlayAvatar(
                        imageUrl = listing.groceryProfilePictureUrl,
                        name = listing.groceryName,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        onClick = onGroceryAvatarClick
                    )
                }

                // Cart action (bottom-right, Browse mode only)
                if (cartAction != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        cartAction()
                    }
                }
            }

            // ── Content ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Name + category inline, topRightAction on the right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = listing.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            CategoryBadge(listing.category)
                        }
                    }
                    topRightAction?.invoke()
                }

                if (listing.description.isNotBlank()) {
                    Text(
                        listing.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Quantity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        null,
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${listing.quantity} ${listing.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Expiry date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(14.dp), tint = expiryColor)
                    Text(
                        expiryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = expiryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showPickupTime) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            null,
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
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Location + distance (browse mode, merged into one row)
                if (showGroceryInfo && listing.location.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            listing.location,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        listing.distanceKm?.let { km ->
                            Text(
                                "·",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                Icons.Default.NearMe,
                                null,
                                Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${km}km",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (!showGroceryInfo) {
                    listing.distanceKm?.let { DistanceBadge(it) }
                }

                secondaryActions?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = it
                    )
                }
            }
        }
    }
}

// ── Expiry, shared with InventoryItemCard ────────────────────────────────────

/** Whole days from today to [date] (yyyy-MM-dd, time ignored); [Long.MAX_VALUE] when unparseable. */
internal fun daysUntil(date: String): Long =
    try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        TimeUnit.MILLISECONDS.toDays(parsed.time - today.time)
    } catch (_: Exception) {
        Long.MAX_VALUE
    }

@Composable
internal fun expiryTextColor(daysUntilExpiry: Long): Color = when {
    daysUntilExpiry <= 0L -> MaterialTheme.colorScheme.error
    daysUntilExpiry <= 3L -> Color(0xFFE65100)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
internal fun expiryLabel(daysUntilExpiry: Long, date: String): String = when {
    daysUntilExpiry < 0 -> stringResource(R.string.listing_expired_label)
    daysUntilExpiry == 0L -> stringResource(R.string.listing_expires_today)
    daysUntilExpiry == 1L -> stringResource(R.string.listing_expires_tomorrow)
    daysUntilExpiry in 2..3 -> stringResource(R.string.listing_expires_in_days, daysUntilExpiry.toInt())
    else -> stringResource(R.string.listing_expires_on, DateTimeUtils.formatDate(date))
}

/**
 * The full-width strip across the bottom of a card's photo once food is three days or less
 * from expiry; draws nothing before that.
 */
@Composable
internal fun ExpiryUrgencyBanner(daysUntilExpiry: Long, modifier: Modifier = Modifier) {
    val text = when {
        daysUntilExpiry < 0L -> stringResource(R.string.listing_expired_label)
        daysUntilExpiry == 0L -> stringResource(R.string.listing_expires_today)
        daysUntilExpiry == 1L -> stringResource(R.string.listing_expires_tomorrow)
        daysUntilExpiry <= 3L -> stringResource(R.string.listing_expiring_soon)
        else -> return
    }
    val color = when {
        daysUntilExpiry <= 0L -> Color(0xCCB71C1C)
        daysUntilExpiry == 1L -> Color(0xCCE65100)
        else -> Color(0xCCF57F17)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
internal fun FoodImagePlaceholder(modifier: Modifier = Modifier) {
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
            Text(
                "${km}km",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
