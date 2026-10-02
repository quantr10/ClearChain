package com.clearchain.app.presentation.ngo.cart

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.CartGroupData
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.ui.theme.ShapeMedium
import com.clearchain.app.util.UiEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun CartPickupScreen(
    groceryId: String,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: CartViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarMessageEffect(snackbarHostState, state.error)

    LaunchedEffect(groceryId) {
        viewModel.onEvent(CartEvent.ShowCheckout(groceryId))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                is UiEvent.Navigate -> onNavigate(event.route)
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenTitleRow(
                title = stringResource(R.string.title_request_pickup),
                onBack = onNavigateBack,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val group = state.groups.firstOrNull { it.groceryId == groceryId }
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    group == null -> EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = stringResource(R.string.cart_empty_title),
                        subtitle = stringResource(R.string.cart_empty_subtitle)
                    )
                    else -> CartPickupContent(
                        group = group,
                        state = state,
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartPickupContent(
    group: CartGroupData,
    state: CartState,
    onEvent: (CartEvent) -> Unit
) {
    val expiryMaxDate = remember(group.earliestExpiryDate) {
        group.earliestExpiryDate?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
    }
    val pickupSelectableDates = remember(expiryMaxDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                val today = LocalDate.now()
                return !date.isBefore(today) && (expiryMaxDate == null || !date.isAfter(expiryMaxDate))
            }
        }
    }
    val storeStart = remember(group.pickupTimeStart) {
        group.pickupTimeStart?.let { runCatching { LocalTime.parse(it.take(5)) }.getOrNull() } ?: LocalTime.MIN
    }
    val storeEnd = remember(group.pickupTimeEnd) {
        group.pickupTimeEnd?.let { runCatching { LocalTime.parse(it.take(5)) }.getOrNull() } ?: LocalTime.of(23, 59)
    }
    val isPickupToday = remember(state.pickupDate) {
        runCatching { LocalDate.parse(state.pickupDate.take(10)) == LocalDate.now() }.getOrDefault(false)
    }
    // If picking up today, nothing sooner than 2 hours from now is selectable. LocalTime
    // arithmetic wraps at midnight, so route through LocalDateTime to detect that "now + 2h"
    // actually rolled into tomorrow (e.g. it's 11pm) - in that case no time today qualifies.
    val minTime = remember(storeStart, isPickupToday) {
        if (!isPickupToday) {
            storeStart
        } else {
            val nowPlus2h = LocalDateTime.now().plusHours(2)
            val floorToday = if (nowPlus2h.toLocalDate() == LocalDate.now()) nowPlus2h.toLocalTime() else LocalTime.MAX
            maxOf(floorToday, storeStart)
        }
    }
    val maxTime = storeEnd
    val hasValidPickupTimes = !minTime.isAfter(maxTime)
    val validHours = remember(minTime, maxTime, hasValidPickupTimes) {
        if (!hasValidPickupTimes) emptyList() else (minTime.hour..maxTime.hour).toList()
    }
    val validMinutesForHour: (Int) -> List<Int> = remember(minTime, maxTime) {
        { hour ->
            val lo = if (hour == minTime.hour) minTime.minute else 0
            val hi = if (hour == maxTime.hour) maxTime.minute else 59
            if (lo > hi) emptyList() else (lo..hi).toList()
        }
    }
    val isLoading = state.isSubmitting
    val canSubmit = group.canCheckout &&
        state.pickupDate.isNotBlank() &&
        state.pickupTime.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AvatarImage(
                            imageUrl = group.groceryProfilePictureUrl,
                            name = group.groceryName,
                            size = 32
                        )
                        Text(group.groceryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    group.items.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProductThumbnail(
                                imageUrl = item.imageUrl,
                                contentDescription = item.title,
                                size = 44.dp
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        stringResource(R.string.label_expires_date, item.expiryDate ?: "N/A"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${item.requestedQuantity} ${item.unit}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            FieldCard(label = stringResource(R.string.label_pickup_date_section)) {
                DatePickerField(
                    value = state.pickupDate,
                    onDateSelected = { onEvent(CartEvent.PickupDateChanged(it)) },
                    enabled = !isLoading,
                    selectableDates = pickupSelectableDates,
                    onClearDate = { onEvent(CartEvent.PickupDateChanged("")) }
                )
            }
        }

        item {
            FieldCard(label = stringResource(R.string.label_pickup_time_field)) {
                var showTimeSheet by remember { mutableStateOf(false) }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(
                            R.string.label_pickup_time_window,
                            group.pickupTimeStart.orEmpty(),
                            group.pickupTimeEnd.orEmpty()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
                            .clickable(enabled = !isLoading && hasValidPickupTimes) { showTimeSheet = true }
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                null,
                                Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.pickupTime.ifBlank { stringResource(R.string.label_select_pickup_time) },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.pickupTime.isNotBlank()) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        }
                    }
                    if (!hasValidPickupTimes) {
                        Text(
                            stringResource(R.string.no_pickup_times_today),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (showTimeSheet) {
                    PickupTimeSheet(
                        hours = validHours,
                        minutesForHour = validMinutesForHour,
                        initialTime = state.pickupTime,
                        onConfirm = { hour, minute ->
                            val formatted = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                            onEvent(CartEvent.PickupTimeChanged(formatted))
                            showTimeSheet = false
                        },
                        onDismiss = { showTimeSheet = false }
                    )
                }
            }
        }

        item {
            FieldCard(label = stringResource(R.string.label_special_handling), isOptional = true) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SpecialHandlingRow(
                        icon = Icons.Default.AcUnit,
                        label = stringResource(R.string.label_needs_refrigeration),
                        checked = state.requiresRefrigeration,
                        enabled = !isLoading,
                        onToggle = { onEvent(CartEvent.ToggleRefrigeration) }
                    )
                    SpecialHandlingRow(
                        icon = Icons.Default.Warning,
                        label = stringResource(R.string.fragile_items),
                        checked = state.isFragile,
                        enabled = !isLoading,
                        onToggle = { onEvent(CartEvent.ToggleFragile) }
                    )
                    SpecialHandlingRow(
                        icon = Icons.Default.FitnessCenter,
                        label = stringResource(R.string.label_heavy_load),
                        checked = state.isHeavy,
                        enabled = !isLoading,
                        onToggle = { onEvent(CartEvent.ToggleHeavy) }
                    )
                }
            }
        }

        item {
            FieldCard(label = stringResource(R.string.label_notes_optional), isOptional = true) {
                ClearChainTextField(
                    value = state.notes,
                    onValueChange = { onEvent(CartEvent.NotesChanged(it)) },
                    placeholder = stringResource(R.string.hint_additional_instructions),
                    leadingIcon = Icons.Default.Edit,
                    imeAction = ImeAction.Done,
                    enabled = !isLoading,
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5
                )
            }
        }

        item {
            ClearChainButton(
                text = stringResource(R.string.action_submit_request),
                onClick = { onEvent(CartEvent.SubmitCheckout) },
                loading = state.isSubmitting,
                enabled = canSubmit && !state.isSubmitting,
                icon = Icons.Default.Send,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickupTimeSheet(
    hours: List<Int>,
    minutesForHour: (Int) -> List<Int>,
    initialTime: String,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHour = initialTime.takeIf { it.length >= 5 }?.substring(0, 2)?.toIntOrNull()
        ?.takeIf { it in hours }
        ?: hours.firstOrNull()
        ?: 0
    val initialMinutes = minutesForHour(initialHour)
    val initialMinute = initialTime.takeIf { it.length >= 5 }?.substring(3, 5)?.toIntOrNull()
        ?.takeIf { it in initialMinutes }
        ?: initialMinutes.firstOrNull()
        ?: 0

    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.label_select_pickup_time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            WheelTimePicker(
                hours = hours,
                minutesForHour = minutesForHour,
                selectedHour = hour,
                selectedMinute = minute,
                onHourChange = { hour = it },
                onMinuteChange = { minute = it }
            )
            ClearChainButton(
                text = stringResource(R.string.ok),
                onClick = { onConfirm(hour, minute) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FieldCard(
    label: String,
    modifier: Modifier = Modifier,
    isOptional: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OptionalFieldLabel(
                text = label,
                isOptional = isOptional,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun SpecialHandlingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled, modifier = Modifier.size(24.dp))
    }
}
