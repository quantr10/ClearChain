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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class AddressSuggestion(
    val displayName: String,
    val fullAddress: String,
    val city: String,
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
    label: String = "Address",
    placeholder: String = "Start typing an address...",
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val scope = rememberCoroutineScope()

    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    Column(modifier = modifier) {
        OutlinedTextField(
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
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Home, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp)
        )

        // Suggestions dropdown
        if (showSuggestions) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Place, null,
                                Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    suggestion.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                                if (suggestion.city.isNotBlank()) {
                                    Text(
                                        suggestion.city,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (index < suggestions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}