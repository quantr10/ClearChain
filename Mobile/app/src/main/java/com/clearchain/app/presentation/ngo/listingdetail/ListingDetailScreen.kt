package com.clearchain.app.presentation.ngo.listingdetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.ListingStatus
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.DateTimeUtils
import com.clearchain.app.util.UiEvent
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ListingDetailScreen(
    listingId: String,
    onNavigateBack: () -> Unit,
    onRequestPickup: (String) -> Unit,
    onNavigateToListingDetail: (String) -> Unit = {},
    onNavigateToStoreProfile: (String) -> Unit = {},
    viewModel: ListingDetailViewModel = hiltViewModel()
) {
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showEditQty by remember { mutableStateOf(false) }

    LaunchedEffect(listingId) { viewModel.onEvent(ListingDetailEvent.LoadListing(listingId)) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.NavigateUp   -> onNavigateBack()
                else -> Unit
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (state.showReportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ListingDetailEvent.DismissReportDialog) },
            title = { Text(stringResource(R.string.report_listing_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.report_listing_subtitle), style = MaterialTheme.typography.bodyMedium)
                    listOf(
                        stringResource(R.string.report_reason_inaccurate),
                        stringResource(R.string.report_reason_already_gone),
                        stringResource(R.string.report_reason_spam),
                        stringResource(R.string.report_reason_inappropriate),
                        stringResource(R.string.report_reason_other)
                    ).forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.onEvent(ListingDetailEvent.ReportReasonChanged(reason)) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = state.reportReason == reason,
                                onClick  = { viewModel.onEvent(ListingDetailEvent.ReportReasonChanged(reason)) }
                            )
                            Text(reason, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick  = { viewModel.onEvent(ListingDetailEvent.SubmitReport) },
                    enabled  = state.reportReason.isNotBlank() && !state.isSubmittingReport
                ) {
                    if (state.isSubmittingReport) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text(stringResource(R.string.btn_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ListingDetailEvent.DismissReportDialog) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (state.showDeleteConfirm) {
        ConfirmDialog(
            title         = stringResource(R.string.action_delete_count, 1),
            message       = stringResource(R.string.msg_delete_listing_confirm, state.listing?.title ?: ""),
            confirmLabel  = stringResource(R.string.delete),
            isDestructive = true,
            onConfirm     = { viewModel.onEvent(ListingDetailEvent.DeleteListing) },
            onDismiss     = { viewModel.onEvent(ListingDetailEvent.DismissDeleteConfirm) }
        )
    }

    if (showEditQty && state.listing != null) {
        val listing = state.listing!!
        var qty by remember { mutableStateOf(listing.quantity.toString()) }
        var qtyError by remember { mutableStateOf<String?>(null) }
        val errInvalid  = stringResource(R.string.error_invalid_number)
        val errPositive = stringResource(R.string.error_must_be_positive)
        val errSame     = stringResource(R.string.error_same_as_current)
        AlertDialog(
            onDismissRequest = { showEditQty = false },
            title = { Text(stringResource(R.string.label_edit_quantity)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.label_current_qty, listing.quantity, listing.unit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value           = qty,
                        onValueChange   = { qty = it; qtyError = null },
                        label           = { Text(stringResource(R.string.label_new_quantity)) },
                        suffix          = { Text(listing.unit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError         = qtyError != null,
                        supportingText  = qtyError?.let { { Text(it) } }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = qty.toIntOrNull()
                    when {
                        n == null             -> qtyError = errInvalid
                        n <= 0                -> qtyError = errPositive
                        n == listing.quantity -> qtyError = errSame
                        else -> { showEditQty = false; viewModel.onEvent(ListingDetailEvent.UpdateQuantity(n)) }
                    }
                }) { Text(stringResource(R.string.action_update)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditQty = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    fullscreenImageUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { fullscreenImageUrl = null },
            confirmButton    = {},
            dismissButton    = { TextButton(onClick = { fullscreenImageUrl = null }) { Text(stringResource(R.string.close)) } },
            text = {
                ZoomableImage(
                    imageUrl           = url,
                    contentDescription = stringResource(R.string.cd_full_size_image),
                    modifier           = Modifier.fillMaxWidth().height(400.dp)
                )
            }
        )
    }

    // No topBar — back via system gesture
    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.error != null -> EmptyState(
                    icon        = Icons.Default.ErrorOutline,
                    title       = stringResource(R.string.error_failed_load_listing),
                    subtitle    = state.error,
                    actionLabel = stringResource(R.string.retry),
                    onAction    = { viewModel.onEvent(ListingDetailEvent.LoadListing(listingId)) }
                )

                state.listing != null -> {
                    val listing   = state.listing!!
                    val isGrocery = state.currentUserType == OrganizationType.GROCERY
                    val images    = listOfNotNull(listing.imageUrl?.takeIf { it.isNotBlank() })

                    // ── Expiry logic ───────────────────────────────────────────
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
                        daysUntilExpiry < 0      -> stringResource(R.string.listing_expired_label)
                        daysUntilExpiry == 0L    -> stringResource(R.string.listing_expires_today)
                        daysUntilExpiry == 1L    -> stringResource(R.string.listing_expires_tomorrow)
                        daysUntilExpiry in 2..3  -> stringResource(R.string.listing_expires_in_days, daysUntilExpiry.toInt())
                        else -> stringResource(R.string.listing_expires_on, DateTimeUtils.formatDate(listing.expiryDate))
                    }
                    val urgencyText: String? = when {
                        daysUntilExpiry < 0L  -> stringResource(R.string.listing_expired_label)
                        daysUntilExpiry == 0L -> stringResource(R.string.listing_expires_today)
                        daysUntilExpiry == 1L -> stringResource(R.string.listing_expires_tomorrow)
                        daysUntilExpiry <= 3L -> stringResource(R.string.listing_expiring_soon)
                        else                  -> null
                    }
                    val urgencyColor: Color = when {
                        daysUntilExpiry <= 0L -> Color(0xCCB71C1C)
                        daysUntilExpiry == 1L -> Color(0xCCE65100)
                        else                  -> Color(0xCCF57F17)
                    }

                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    ) {

                        // ══════════════════════════════════════════════════════
                        // IMAGE AREA  (200dp)
                        // ══════════════════════════════════════════════════════
                        Card(
                            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
                            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) { Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            // Base image / placeholder
                            if (images.isNotEmpty()) {
                                DetailImageCarousel(
                                    images = images,
                                    title  = listing.title,
                                    onTap  = { url -> fullscreenImageUrl = url }
                                )
                            } else {
                                Box(
                                    Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PhotoCamera, null,
                                        Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                }
                            }

                            // ── Status badge — top-left ────────────────────────
                            Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                                if (listing.isArchived) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xCC455A64)
                                    ) {
                                        Text(
                                            stringResource(R.string.tab_archived),
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

                            // ── 3 action buttons — top-right, horizontal row ───
                            Row(
                                modifier              = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isGrocery) {
                                    ImageActionButton(
                                        icon  = Icons.Default.Edit,
                                        label = stringResource(R.string.action_edit_qty)
                                    ) { showEditQty = true }
                                    if (listing.isArchived) {
                                        ImageActionButton(
                                            icon    = Icons.Default.Unarchive,
                                            label   = stringResource(R.string.action_restore),
                                            loading = state.isArchiving
                                        ) { viewModel.onEvent(ListingDetailEvent.UnarchiveListing) }
                                    } else {
                                        ImageActionButton(
                                            icon    = Icons.Default.Archive,
                                            label   = stringResource(R.string.action_archive),
                                            loading = state.isArchiving
                                        ) { viewModel.onEvent(ListingDetailEvent.ArchiveListing) }
                                    }
                                    ImageActionButton(
                                        icon    = Icons.Default.Delete,
                                        tint    = Color(0xFFFF6B6B),
                                        label   = stringResource(R.string.delete),
                                        loading = state.isDeleting
                                    ) { viewModel.onEvent(ListingDetailEvent.ShowDeleteConfirm) }
                                } else {
                                    ImageActionButton(
                                        icon  = if (state.isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        tint  = if (state.isSaved) Color(0xFFFF6B6B) else Color.White,
                                        label = if (state.isSaved) stringResource(R.string.cd_remove_from_saved)
                                                else stringResource(R.string.cd_save_listing),
                                        loading = state.isTogglingFave
                                    ) { viewModel.onEvent(ListingDetailEvent.ToggleSave) }
                                    ImageActionButton(
                                        icon  = Icons.Default.Share,
                                        label = stringResource(R.string.cd_share)
                                    ) {
                                        val shareText = buildString {
                                            append("${listing.title}\n${listing.quantity} ${listing.unit} available\n")
                                            append("Expires: ${listing.expiryDate.take(10)}\n")
                                            if (!listing.groceryHours.isNullOrBlank()) append("Hours: ${listing.groceryHours}\n")
                                            append("At: ${listing.groceryName}, ${listing.location}")
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                    putExtra(Intent.EXTRA_SUBJECT, listing.title)
                                                }, "Share listing"
                                            )
                                        )
                                    }
                                    ImageActionButton(
                                        icon  = Icons.Default.Flag,
                                        label = stringResource(R.string.cd_report_listing_icon)
                                    ) { viewModel.onEvent(ListingDetailEvent.ShowReportDialog) }
                                }
                            }

                            // ── Urgency banner — bottom-center (same as ListingCard) ──
                            if (urgencyText != null) {
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
                                        style      = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White
                                    )
                                }
                            }

                            // ── Grocery avatar — bottom-left, inside image (NGO only) ──
                            if (!isGrocery) Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .size(38.dp)
                                    .clickable { onNavigateToStoreProfile(listing.groceryId) },
                                shape           = CircleShape,
                                color           = MaterialTheme.colorScheme.primaryContainer,
                                border          = BorderStroke(2.dp, Color.White),
                                shadowElevation = 3.dp
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        listing.groceryName.take(1).uppercase(),
                                        style      = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } }


                        // ══════════════════════════════════════════════════════
                        // CONTENT BELOW IMAGE
                        // ══════════════════════════════════════════════════════
                        Column(
                            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ── Title ──────────────────────────────────────────
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    listing.title,
                                    style      = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier   = Modifier.weight(1f),
                                    maxLines   = 2,
                                    overflow   = TextOverflow.Ellipsis
                                )
                                CategoryBadge(listing.category)
                            }

                            // ── Description card ───────────────────────────────
                            if (listing.description.isNotBlank()) {
                                SectionCard(stringResource(R.string.label_description)) {
                                    Text(
                                        listing.description,
                                        style    = MaterialTheme.typography.bodySmall,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // ── Details card ───────────────────────────────────
                            val displayQty = state.availabilityOverride ?: listing.quantity
                            SectionCard(stringResource(R.string.section_details)) {
                                CompactDetailRow(Icons.Default.ShoppingCart, "${displayQty} ${listing.unit}")
                                CompactDetailRow(
                                    icon      = Icons.Default.CalendarToday,
                                    text      = expiryText,
                                    textColor = expiryColor,
                                    bold      = daysUntilExpiry <= 3
                                )
                                if (!listing.groceryHours.isNullOrBlank()) {
                                    CompactDetailRow(
                                        icon = Icons.Default.Schedule,
                                        text = listing.groceryHours!!
                                    )
                                } else if (listing.pickupTimeStart.isNotBlank()) {
                                    CompactDetailRow(
                                        icon = Icons.Default.Schedule,
                                        text = stringResource(R.string.listing_pickup_from, listing.pickupTimeStart, listing.pickupTimeEnd)
                                    )
                                }
                                listing.distanceKm?.let { km ->
                                    CompactDetailRow(
                                        icon      = Icons.Default.NearMe,
                                        text      = stringResource(R.string.distance_km_away, km),
                                        textColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // ── About Us card (NGO only) ───────────────────────
                            if (!isGrocery) {
                                val address = state.groceryProfile?.address
                                    ?.takeIf { it.isNotBlank() } ?: listing.location

                                SectionCard(stringResource(R.string.section_about_us)) {
                                    if (address.isNotBlank()) {
                                        Row(
                                            modifier              = Modifier.fillMaxWidth(),
                                            verticalAlignment     = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Place, null,
                                                Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                address,
                                                style    = MaterialTheme.typography.labelMedium,
                                                color    = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(
                                                onClick = {
                                                    val encoded = Uri.encode(address)
                                                    val uri = Uri.parse("geo:0,0?q=$encoded")
                                                    runCatching {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_VIEW, uri).apply {
                                                                setPackage("com.google.android.apps.maps")
                                                            }
                                                        )
                                                    }.onFailure {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_VIEW,
                                                                Uri.parse("https://maps.google.com/?q=$encoded"))
                                                        )
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) {
                                                Text(
                                                    stringResource(R.string.action_get_directions),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }

                                    state.groceryProfile?.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                                        Row(
                                            modifier              = Modifier.clickable {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                            },
                                            verticalAlignment     = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Phone, null, Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                phone,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // ── Similar listings card (NGO only) ───────────────
                            if (!isGrocery && state.similarListings.isNotEmpty()) {
                                SectionCard(stringResource(R.string.section_similar_listings)) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding        = PaddingValues(vertical = 2.dp)
                                    ) {
                                        items(state.similarListings, key = { it.id }) { similar ->
                                            SimilarListingCard(
                                                listing = similar,
                                                onClick = { onNavigateToListingDetail(similar.id) }
                                            )
                                        }
                                    }
                                }
                            }

                            // ── Reserve button (NGO only) ──────────────────────
                            if (!isGrocery && listing.status == ListingStatus.AVAILABLE) {
                                Button(
                                    onClick  = { onRequestPickup(listing.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape    = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.ShoppingCart, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.action_request_pickup), fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Image carousel
// ─────────────────────────────────────────────────────────────────────────────

@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
private fun DetailImageCarousel(images: List<String>, title: String, onTap: (String) -> Unit = {}) {
    val pagerState = rememberPagerState(pageCount = { images.size })
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model              = images[page],
                contentDescription = title,
                modifier           = Modifier.fillMaxSize().clickable { onTap(images[page]) },
                contentScale       = ContentScale.Crop
            )
        }
        if (images.size > 1) {
            Row(
                modifier              = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(images.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Circular action button overlaid on image — white icon on dark circle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImageActionButton(
    icon:    ImageVector,
    label:   String,
    tint:    Color = Color.White,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick         = onClick,
        modifier        = Modifier.size(36.dp),
        shape           = CircleShape,
        color           = Color(0x99000000),
        shadowElevation = 2.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(icon, label, Modifier.size(18.dp), tint = tint)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compact section label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompactSectionLabel(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.onSurface
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Compact detail row — same visual as ListingCard rows
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompactDetailRow(
    icon:      ImageVector,
    text:      String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    bold:      Boolean = false
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = textColor)
        Text(
            text,
            style      = MaterialTheme.typography.labelMedium,
            color      = textColor,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Similar listing card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SimilarListingCard(listing: Listing, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(160.dp).clickable { onClick() },
        shape    = RoundedCornerShape(10.dp)
    ) {
        Column {
            if (!listing.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model              = listing.imageUrl,
                    contentDescription = listing.title,
                    modifier           = Modifier.fillMaxWidth().height(80.dp),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.RestaurantMenu, null, Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(listing.title, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${listing.quantity} ${listing.unit}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.label_exp_short, listing.expiryDate.take(10)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
