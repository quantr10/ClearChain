package com.clearchain.app.presentation.ngo.inventorydetail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.clearchain.app.R
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.DateTimeUtils

// ═══ Screen ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    itemId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRequestDetail: (String) -> Unit = {},
    onNavigateToPublicProfile: (String) -> Unit = {},
    viewModel: InventoryDetailViewModel = hiltViewModel()
) {
    val state            by viewModel.state.collectAsState()
    val context          = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var fullscreenPhoto  by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadPhoto(itemId, it) }
    }

    LaunchedEffect(itemId) { viewModel.loadItem(itemId) }
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            if (event is com.clearchain.app.util.UiEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Fullscreen photo dialog
    fullscreenPhoto?.let { url ->
        AlertDialog(
            onDismissRequest = { fullscreenPhoto = null },
            confirmButton = {
                TextButton(onClick = { fullscreenPhoto = null }) { Text(stringResource(R.string.dialog_close)) }
            },
            text = {
                AsyncImage(
                    model          = url,
                    contentDescription = stringResource(R.string.doc_photo_description),
                    modifier       = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    contentScale   = ContentScale.Fit
                )
            }
        )
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

    BackHandler(state.isEditing) { viewModel.cancelEdit() }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.error != null -> EmptyState(
                    icon        = Icons.Default.ErrorOutline,
                    title       = stringResource(R.string.failed_to_load_item),
                    subtitle    = state.error,
                    actionLabel = stringResource(R.string.retry),
                    onAction    = { viewModel.loadItem(itemId) }
                )

                state.isEditing && state.item != null -> {
                    EditItemContent(
                        state    = state,
                        itemId   = itemId,
                        viewModel = viewModel
                    )
                }

                state.item != null -> {
                    val item = state.item!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── Header ─────────────────────────────
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.productName,
                                    style      = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    item.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            InventoryStatusBadge(status = item.status)
                        }

                        // ── QR + Edit actions ─────────────────
                        if (item.status == InventoryStatus.ACTIVE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick  = { viewModel.showQrSheet() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.QrCode, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.generate_qr_label))
                                }
                                Button(
                                    onClick  = { viewModel.startEdit() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.edit_item))
                                }
                            }
                        }

                        HorizontalDivider()

                        // ── Quantity Card ──────────────────────
                        SectionHeader(stringResource(R.string.section_quantity))
                        Card(
                            shape  = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Scale, null, Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "${item.quantity} ${item.unit}",
                                    style      = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // ── Timeline / Lifecycle ───────────────
                        SectionHeader(stringResource(R.string.section_lifecycle))
                        LifecycleTimeline(item = item)

                        // ── Source Traceability ────────────────
                        state.relatedRequest?.let { request ->
                            HorizontalDivider()
                            SectionHeader(stringResource(R.string.section_source))

                            // Origin chain card
                            Card(
                                shape  = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier            = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AccountTree, null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text(
                                            stringResource(R.string.origin_chain),
                                            style      = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                                    )
                                    TraceabilityRow(
                                        icon  = Icons.Default.Store,
                                        label = stringResource(R.string.donated_by),
                                        value = request.groceryName,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    TraceabilityRow(
                                        icon  = Icons.Default.Inventory2,
                                        label = stringResource(R.string.label_listing),
                                        value = request.listingTitle,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    TraceabilityRow(
                                        icon  = Icons.Default.LocalShipping,
                                        label = stringResource(R.string.label_pickup_date),
                                        value = request.pickupDate,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    TraceabilityRow(
                                        icon  = Icons.Default.ShoppingCart,
                                        label = stringResource(R.string.label_original_qty),
                                        value = "${request.requestedQuantity}",
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    request.notes?.let { notes ->
                                        if (notes.isNotBlank()) {
                                            TraceabilityRow(
                                                icon  = Icons.Default.StickyNote2,
                                                label = stringResource(R.string.label_notes),
                                                value = notes,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            // Tap to view full request
                            OutlinedButton(
                                onClick  = { onNavigateToRequestDetail(request.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.view_full_request))
                            }
                            OutlinedButton(
                                onClick  = { onNavigateToPublicProfile(request.groceryId) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Store, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_view_grocery_profile))
                            }
                        }

                        if (state.isLoadingRequest) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.loading_source_info),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // ── Photo Documentation ────────────────
                        HorizontalDivider()
                        SectionHeader(stringResource(R.string.section_documentation))
                        state.photoUploadError?.let { err ->
                            AlertBanner(
                                message = err,
                                type    = AlertType.ERROR,
                                icon    = Icons.Default.ErrorOutline
                            )
                        }
                        if (item.photoUrl != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { fullscreenPhoto = item.photoUrl }
                            ) {
                                AsyncImage(
                                    model              = item.photoUrl,
                                    contentDescription = stringResource(R.string.doc_photo_description),
                                    modifier           = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale       = ContentScale.Crop
                                )
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                                    color    = Color.Black.copy(alpha = 0.6f),
                                    shape    = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomIn, null, Modifier.size(14.dp), tint = Color.White)
                                        Text(stringResource(R.string.tap_to_expand), style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }
                                }
                            }
                            if (item.status == InventoryStatus.ACTIVE) {
                                OutlinedButton(
                                    onClick  = { photoPickerLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled  = !state.isUploadingPhoto
                                ) {
                                    Icon(Icons.Default.ChangeCircle, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.replace_photo))
                                }
                            }
                        } else if (item.status == InventoryStatus.ACTIVE) {
                            Card(
                                shape  = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier            = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.AddAPhoto, null,
                                        Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    Text(
                                        stringResource(R.string.no_photo_attached),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick  = { photoPickerLauncher.launch("image/*") },
                                        enabled  = !state.isUploadingPhoto,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (state.isUploadingPhoto) {
                                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.uploading))
                                        } else {
                                            Icon(Icons.Default.AddAPhoto, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.attach_photo))
                                        }
                                    }
                                }
                            }
                        }

                        // ── Status Card ──────────────────────
                        HorizontalDivider()
                        when (item.status) {
                            InventoryStatus.ACTIVE -> StatusInfoCard(
                                icon    = Icons.Default.Info,
                                message = stringResource(R.string.status_active_info),
                                color   = MaterialTheme.colorScheme.secondaryContainer,
                                tint    = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            InventoryStatus.DISTRIBUTED -> StatusInfoCard(
                                icon    = Icons.Default.VolunteerActivism,
                                message = stringResource(R.string.status_distributed_info),
                                color   = MaterialTheme.colorScheme.tertiaryContainer,
                                tint    = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            InventoryStatus.EXPIRED -> StatusInfoCard(
                                icon    = Icons.Default.Warning,
                                message = stringResource(R.string.status_expired_info),
                                color   = MaterialTheme.colorScheme.errorContainer,
                                tint    = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ── Edit Item Content ─────────────────────────────────────────────────────────

@Composable
private fun EditItemContent(
    state:     InventoryDetailState,
    itemId:    String,
    viewModel: InventoryDetailViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.saveError?.let { error ->
            AlertBanner(
                message = error,
                type    = AlertType.ERROR,
                icon    = Icons.Default.ErrorOutline
            )
        }

        SectionHeader(stringResource(R.string.section_item_details))

        OutlinedTextField(
            value         = state.editProductName,
            onValueChange = { viewModel.onEditProductNameChanged(it) },
            label         = { Text(stringResource(R.string.label_product_name)) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            leadingIcon   = { Icon(Icons.Default.Inventory2, null) }
        )

        OutlinedTextField(
            value         = state.editCategory,
            onValueChange = { viewModel.onEditCategoryChanged(it) },
            label         = { Text(stringResource(R.string.label_category)) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            leadingIcon   = { Icon(Icons.Default.Category, null) }
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value         = state.editQuantity,
                onValueChange = { viewModel.onEditQuantityChanged(it) },
                label         = { Text(stringResource(R.string.label_quantity_required)) },
                modifier      = Modifier.weight(2f),
                singleLine    = true,
                leadingIcon   = { Icon(Icons.Default.Scale, null) }
            )
            OutlinedTextField(
                value         = state.editUnit,
                onValueChange = { viewModel.onEditUnitChanged(it) },
                label         = { Text(stringResource(R.string.label_unit)) },
                modifier      = Modifier.weight(1f),
                singleLine    = true
            )
        }

        OutlinedTextField(
            value         = state.editExpiryDate,
            onValueChange = { viewModel.onEditExpiryDateChanged(it) },
            label         = { Text(stringResource(R.string.label_expiry_date_format)) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            leadingIcon   = { Icon(Icons.Default.DateRange, null) }
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick  = { viewModel.cancelEdit() },
                modifier = Modifier.weight(1f),
                enabled  = !state.isSaving
            ) { Text(stringResource(R.string.cancel)) }

            Button(
                onClick  = { viewModel.saveEdit(itemId) },
                modifier = Modifier.weight(1f),
                enabled  = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.save))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Lifecycle Timeline ────────────────────────────────────────────────────────

@Composable
private fun LifecycleTimeline(item: com.clearchain.app.domain.model.InventoryItem) {
    data class LifecycleStep(
        val icon:  androidx.compose.ui.graphics.vector.ImageVector,
        val label: String,
        val date:  String
    )
    val steps = buildList {
        add(LifecycleStep(Icons.Default.Inventory, stringResource(R.string.inventory_step_received), DateTimeUtils.formatDate(item.receivedAt)))
        if (item.status == InventoryStatus.DISTRIBUTED && item.distributedAt != null)
            add(LifecycleStep(Icons.Default.VolunteerActivism, stringResource(R.string.inventory_step_distributed), DateTimeUtils.formatDate(item.distributedAt)))
        if (item.status == InventoryStatus.EXPIRED)
            add(LifecycleStep(Icons.Default.Warning, stringResource(R.string.inventory_step_expired), DateTimeUtils.formatDate(item.expiryDate)))
        else
            add(LifecycleStep(Icons.Default.EventBusy, stringResource(R.string.inventory_step_expires), DateTimeUtils.formatDate(item.expiryDate)))
    }

    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            steps.forEachIndexed { idx, step ->
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(step.icon, null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(step.label, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(step.date, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
                if (idx < steps.lastIndex) {
                    HorizontalDivider(
                        Modifier.padding(start = 32.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ── Traceability Row ──────────────────────────────────────────────────────────

@Composable
private fun TraceabilityRow(
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp).padding(top = 2.dp), tint = color.copy(alpha = 0.7f))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

// ── Status Info Card ──────────────────────────────────────────────────────────

@Composable
private fun StatusInfoCard(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    color:   Color,
    tint:    Color
) {
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = tint)
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = tint)
        }
    }
}

// ── QR Label Bottom Sheet ─────────────────────────────────────────────────────

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
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
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
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.dialog_close))
            }
            Button(onClick = onShare, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.btn_share_label))
            }
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
