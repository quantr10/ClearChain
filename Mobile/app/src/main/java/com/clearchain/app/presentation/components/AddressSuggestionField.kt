package com.clearchain.app.presentation.components

import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.clearchain.app.R
import kotlinx.coroutines.withContext
import java.util.Locale

data class AddressSuggestion(
    val displayName: String,
    val fullAddress: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Address input with Geocoder-based suggestions.
 * Shows dropdown of suggestions after 300ms debounce.
 * When user picks a suggestion, calls onAddressSelected with full data.
 */
@Composable
fun AddressSuggestionField(
    value: String,
    onValueChange: (String) -> Unit,
    onAddressSelected: (AddressSuggestion) -> Unit,
    label: String = "",
    isOptional: Boolean = false,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val context = LocalContext.current
    val resolvedLabel = label.ifEmpty { stringResource(R.string.label_address) }
    val resolvedPlaceholder = placeholder.ifEmpty { stringResource(R.string.placeholder_start_typing_address) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    BoxWithConstraints(modifier = modifier.zIndex(if (showSuggestions) 1f else 0f)) {
        val dropdownWidth = maxWidth
        val dropdownOffset = with(density) {
            IntOffset(
                x = 0,
                y = (if (isError && !errorMessage.isNullOrBlank()) 78.dp else 56.dp).roundToPx()
            )
        }

        ClearChainTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)

                // Debounce search
                searchJob?.cancel()
                if (newValue.length >= 3) {
                    searchJob = scope.launch {
                        delay(300)
                        try {
                            val results = withContext(Dispatchers.IO) {
                                @Suppress("DEPRECATION")
                                geocoder.getFromLocationName(newValue, 5)
                            }
                            suggestions = results?.mapNotNull { addr ->
                                val line = addr.getAddressLine(0) ?: return@mapNotNull null
                                AddressSuggestion(
                                    displayName = line,
                                    fullAddress = line,
                                    city = addr.locality
                                        ?: addr.subAdminArea
                                        ?: addr.adminArea
                                        ?: "",
                                    state = addr.adminArea ?: "",
                                    zipCode = addr.postalCode ?: "",
                                    latitude = addr.latitude,
                                    longitude = addr.longitude
                                )
                            } ?: emptyList()
                            showSuggestions = suggestions.isNotEmpty()
                        } catch (e: Exception) {
                            suggestions = emptyList()
                            showSuggestions = false
                        }
                    }
                } else {
                    suggestions = emptyList()
                    showSuggestions = false
                }
            },
            label = resolvedLabel,
            isOptional = isOptional,
            placeholder = resolvedPlaceholder,
            leadingIcon = Icons.Default.Home,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = isError,
            errorMessage = errorMessage
        )

        // Suggestions dropdown
        if (showSuggestions) {
            Popup(
                alignment = Alignment.TopStart,
                offset = dropdownOffset
            ) {
                Card(
                    modifier = Modifier.width(dropdownWidth),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        suggestions.forEachIndexed { index, suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onValueChange(suggestion.fullAddress)
                                        onAddressSelected(suggestion)
                                        showSuggestions = false
                                        suggestions = emptyList()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Place, null,
                                    Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        suggestion.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 2
                                    )
                                    if (suggestion.city.isNotBlank()) {
                                        Text(
                                            suggestion.city,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (index < suggestions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 32.dp, end = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
