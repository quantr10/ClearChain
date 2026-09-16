package com.clearchain.app.presentation.ngo.inventorydetail

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clearchain.app.ui.theme.ScreenPadding
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.clearchain.app.R
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.DateTimeUtils
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// --- Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    itemId: String,
    onNavigateBack: () -> Unit = {},
    onNavigateToRequestDetail: (String) -> Unit = {},
    onNavigateToPublicProfile: (String) -> Unit = {},
    onNavigateToListingDetail: (String) -> Unit = {},
    viewModel: InventoryDetailViewModel = hiltViewModel()
) {
    val state            by viewModel.state.collectAsState()
    val context          = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showFullPhoto by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) { viewModel.loadItem(itemId) }
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            if (event is com.clearchain.app.util.UiEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // QR label bottom sheet
    if (state.showQrSheet) {
        state.item?.let { item ->
            ModalBottomSheet(onDismissRequest = { viewModel.dismissQrSheet() }) {
                QrLabelSheet(
                    itemId      = item.id,
                    productName = item.productName,
                    category    = item.category,
                    quantity    = "${item.quantity} ${item.unit}",
                    expiryDate  = DateTimeUtils.formatDate(item.expiryDate),
                    onShare     = {
                        val text = "ClearChain Item\nID: ${item.id}\n" +
                            "Product: ${item.productName}\n" +
                            "Category: ${item.category}\n" +
                            "Qty: ${item.quantity} ${item.unit}\n" +
                            "Expires: ${DateTimeUtils.formatDate(item.expiryDate)}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_item_label)))
                    },
                    onDismiss = { viewModel.dismissQrSheet() }
                )
            }
        }
    }
    if (showFullPhoto) {
        state.item?.photoUrl?.let { photoUrl ->
            FullPhotoDialog(photoUrl = photoUrl, onDismiss = { showFullPhoto = false })
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenTitleRow(
                title = stringResource(R.string.inventory_detail_title),
                onBack = onNavigateBack,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.error != null -> EmptyState(
                    icon        = Icons.Default.ErrorOutline,
                    title       = stringResource(R.string.error_generic),
                    subtitle    = state.error,
                    actionLabel = stringResource(R.string.retry),
                    onAction    = { viewModel.loadItem(itemId) }
                )

                state.item != null -> {
                    val item = state.item!!
                    val daysUntilExpiry = remember(item.expiryDate) { daysUntilExpiry(item.expiryDate) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(ScreenPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InventoryHeroCard(
                            item = item,
                            daysUntilExpiry = daysUntilExpiry,
                            groceryName = state.relatedRequest?.groceryName,
                            groceryProfilePictureUrl = state.relatedRequest?.groceryProfilePictureUrl,
                            onViewGroceryProfile = state.relatedRequest?.let { request ->
                                { onNavigateToPublicProfile(request.groceryId) }
                            },
                            onExpandPhoto = { showFullPhoto = true },
                            onShowQr = { viewModel.showQrSheet() }
                        )

                        InventorySectionCard(stringResource(R.string.section_item_details)) {
                            InventoryDetailRow(
                                icon = Icons.Default.Scale,
                                label = stringResource(R.string.section_quantity),
                                value = "${formatInventoryQuantity(item.quantity)} ${item.unit}"
                            )
                            InventoryDetailRow(
                                icon = Icons.Default.Event,
                                label = stringResource(R.string.inventory_step_expires),
                                value = DateTimeUtils.formatDate(item.expiryDate),
                                valueColor = if (item.status == InventoryStatus.ACTIVE && (daysUntilExpiry ?: 99L) <= 3L) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            InventoryDetailRow(
                                icon = Icons.Default.Inventory,
                                label = stringResource(R.string.inventory_step_received),
                                value = DateTimeUtils.formatDate(item.receivedAt)
                            )
                            item.distributedAt?.let {
                                InventoryDetailRow(
                                    icon = Icons.Default.VolunteerActivism,
                                    label = stringResource(R.string.inventory_step_distributed),
                                    value = DateTimeUtils.formatDate(it)
                                )
                            }
                        }

                        InventorySectionCard(stringResource(R.string.section_lifecycle)) {
                            LifecycleStep(
                                icon = Icons.Default.Inventory,
                                title = stringResource(R.string.inventory_step_received),
                                value = DateTimeUtils.formatDate(item.receivedAt),
                                active = true
                            )
                            LifecycleStep(
                                icon = if (item.status == InventoryStatus.EXPIRED) Icons.Default.Warning else Icons.Default.Event,
                                title = if (item.status == InventoryStatus.EXPIRED) {
                                    stringResource(R.string.inventory_step_expired)
                                } else {
                                    stringResource(R.string.inventory_step_expires)
                                },
                                value = DateTimeUtils.formatDate(item.expiryDate),
                                active = item.status != InventoryStatus.DISTRIBUTED
                            )
                            LifecycleStep(
                                icon = Icons.Default.VolunteerActivism,
                                title = stringResource(R.string.inventory_step_distributed),
                                value = item.distributedAt?.let { DateTimeUtils.formatDate(it) }
                                    ?: stringResource(R.string.not_yet_distributed),
                                active = item.status == InventoryStatus.DISTRIBUTED
                            )
                        }

                        // -- Source Traceability ----------------
                        state.relatedRequest?.let { request ->
                            InventorySectionCard(stringResource(R.string.section_source)) {
                                InventoryDetailRow(
                                    icon = Icons.Default.Store,
                                    label = stringResource(R.string.donated_by),
                                    value = request.groceryName
                                )
                                InventoryDetailRow(
                                    icon = Icons.Default.Inventory2,
                                    label = stringResource(R.string.label_listing),
                                    value = request.listingTitle
                                )
                                InventoryDetailRow(
                                    icon = Icons.Default.LocalShipping,
                                    label = stringResource(R.string.label_pickup_date),
                                    value = DateTimeUtils.formatDate(request.pickupDate)
                                )
                                InventoryDetailRow(
                                    icon = Icons.Default.ShoppingCart,
                                    label = stringResource(R.string.label_original_qty),
                                    value = "${request.requestedQuantity}"
                                )
                                request.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                                    InventoryDetailRow(
                                        icon = Icons.Default.StickyNote2,
                                        label = stringResource(R.string.label_notes),
                                        value = notes
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ClearChainOutlinedButton(
                                        text = stringResource(R.string.view_full_request),
                                        onClick  = { onNavigateToRequestDetail(request.id) },
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.OpenInNew
                                    )
                                    ClearChainOutlinedButton(
                                        text = stringResource(R.string.action_view_grocery_profile),
                                        onClick  = { onNavigateToPublicProfile(request.groceryId) },
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Store
                                    )
                                }
                            }
                        }

                        if (state.isLoadingRequest) {
                            InventorySectionCard(stringResource(R.string.section_source)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.loading_source_info),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (state.moreFromStore.isNotEmpty()) {
                            InventorySectionCard(stringResource(R.string.label_more_from_store)) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(state.moreFromStore, key = { it.id }) { listing ->
                                        ListingCard(
                                            listing = listing,
                                            onClick = { onNavigateToListingDetail(listing.id) },
                                            modifier = Modifier.width(220.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            }
        }
    }
}

// -- Edit Item Content ---------------------------------------------------------

@Composable
private fun InventoryHeroCard(
    item: com.clearchain.app.domain.model.InventoryItem,
    daysUntilExpiry: Long?,
    groceryName: String?,
    groceryProfilePictureUrl: String? = null,
    onViewGroceryProfile: (() -> Unit)?,
    onExpandPhoto: () -> Unit,
    onShowQr: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(200.dp)) {
                if (!item.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.photoUrl,
                        contentDescription = item.productName,
                        modifier = Modifier.fillMaxSize().clickable(onClick = onExpandPhoto),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
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

                Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    InventoryStatusBadge(status = item.status)
                }

                if (item.status == InventoryStatus.ACTIVE) {
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        InventoryImageActionButton(
                            icon = Icons.Default.QrCode,
                            label = stringResource(R.string.generate_qr_label),
                            onClick = onShowQr
                        )
                    }
                }

                val urgencyText = when {
                    item.status == InventoryStatus.EXPIRED -> stringResource(R.string.listing_expired_label)
                    daysUntilExpiry == 0L -> stringResource(R.string.listing_expires_today)
                    daysUntilExpiry == 1L -> stringResource(R.string.listing_expires_tomorrow)
                    daysUntilExpiry != null && daysUntilExpiry <= 3L -> stringResource(R.string.listing_expiring_soon)
                    else -> null
                }
                if (urgencyText != null) {
                    val urgencyColor = when {
                        item.status == InventoryStatus.EXPIRED || daysUntilExpiry == 0L -> Color(0xCCB71C1C)
                        daysUntilExpiry == 1L -> Color(0xCCE65100)
                        else -> Color(0xCCF57F17)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(urgencyColor)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            urgencyText.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (!groceryName.isNullOrBlank() && onViewGroceryProfile != null) {
                    OverlayAvatar(
                        imageUrl = groceryProfilePictureUrl,
                        name     = groceryName,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        onClick  = onViewGroceryProfile
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                item.productName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            InventoryCategoryBadge(category = item.category)
        }

        Text(
            text = expirySummaryText(item.status, daysUntilExpiry, item.expiryDate),
            style = MaterialTheme.typography.labelSmall,
            color = expirySummaryColor(item.status, daysUntilExpiry),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InventoryImageActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(24.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InventorySectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ClearChainCard {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun LifecycleStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    active: Boolean
) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = MaterialTheme.shapes.small,
            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InventoryDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (label == null) {
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatInventoryQuantity(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        quantity.toInt().toString()
    } else {
        quantity.toString()
    }
}

private fun daysUntilExpiry(expiryDate: String): Long? {
    return try {
        ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(expiryDate.take(10)))
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun expirySummaryText(status: InventoryStatus, daysUntilExpiry: Long?, expiryDate: String): String {
    if (status == InventoryStatus.EXPIRED) return stringResource(R.string.listing_expired_label)
    return when (daysUntilExpiry) {
        null -> DateTimeUtils.formatDate(expiryDate)
        0L -> stringResource(R.string.listing_expires_today)
        1L -> stringResource(R.string.listing_expires_tomorrow)
        else -> if (daysUntilExpiry > 1L) {
            stringResource(R.string.listing_expires_in_days, daysUntilExpiry.toInt())
        } else {
            stringResource(R.string.listing_expired_label)
        }
    }
}

@Composable
private fun expirySummaryColor(status: InventoryStatus, daysUntilExpiry: Long?): Color {
    return when {
        status == InventoryStatus.EXPIRED -> MaterialTheme.colorScheme.error
        daysUntilExpiry != null && daysUntilExpiry <= 3L -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

// -- QR Label Bottom Sheet -----------------------------------------------------

@Composable
private fun QrLabelSheet(
    itemId:      String,
    productName: String,
    category:    String,
    quantity:    String,
    expiryDate:  String,
    onShare:     () -> Unit,
    onDismiss:   () -> Unit
) {
    // Derive a simple QR-like matrix from the item ID bytes (visual only, not scannable)
    val qrMatrix = remember(itemId) { generateQrMatrix(itemId, 21) }
    val qrColor  = MaterialTheme.colorScheme.onSurface

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.qr_item_label_title),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // QR code canvas (visual pattern derived from item ID)
        Canvas(modifier = Modifier.size(200.dp)) {
            val cellSize = size.width / qrMatrix.size
            qrMatrix.forEachIndexed { row, cols ->
                cols.forEachIndexed { col, filled ->
                    if (filled) {
                        drawRect(
                            color   = qrColor,
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size    = androidx.compose.ui.geometry.Size(cellSize - 1f, cellSize - 1f)
                        )
                    }
                }
            }
        }

        // Label details card
        ClearChainSurfaceCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LabelRow(stringResource(R.string.label_product), productName)
                LabelRow(stringResource(R.string.label_category_short), category)
                LabelRow(stringResource(R.string.label_quantity_short), quantity)
                LabelRow(stringResource(R.string.label_expires_short), expiryDate)
                LabelRow(stringResource(R.string.label_id), itemId.take(8).uppercase())
            }
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ClearChainOutlinedButton(
                text = stringResource(R.string.dialog_close),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            ClearChainButton(
                text = stringResource(R.string.btn_share_label),
                onClick = onShare,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Share
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LabelRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold)
    }
}

// Generate a deterministic grid pattern from the item ID for QR visual
private fun generateQrMatrix(seed: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }
    val bytes  = seed.toByteArray()
    // Finder patterns (corners)
    for (r in 0..6) for (c in 0..6) {
        val inOuter = r == 0 || r == 6 || c == 0 || c == 6
        val inInner = r in 2..4 && c in 2..4
        matrix[r][c] = inOuter || inInner
        matrix[r][size - 1 - c] = inOuter || inInner
        matrix[size - 1 - r][c] = inOuter || inInner
    }
    // Data modules (deterministic from seed)
    for (r in 8 until size) {
        for (c in 8 until size) {
            val idx = (r * size + c) % bytes.size
            matrix[r][c] = bytes[idx].toInt() and ((r + c) % 8) != 0
        }
    }
    return matrix
}
