package com.clearchain.app.presentation.components

import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import com.clearchain.app.R
import com.clearchain.app.ui.theme.ControlHeight
import com.clearchain.app.ui.theme.ControlIconSize
import com.clearchain.app.ui.theme.ControlPadding
import com.clearchain.app.ui.theme.IconGap
import com.clearchain.app.ui.theme.ListGap
import com.clearchain.app.ui.theme.MinTouchTarget
import com.clearchain.app.ui.theme.ShapeMedium
import java.util.Locale
import kotlinx.coroutines.*

data class AddressSuggestion(
    val displayName: String,
    val fullAddress: String,
    val streetAddress: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val latitude: Double,
    val longitude: Double
)

/** What the area under the address field shows. */
private sealed interface SuggestionState {
    /** Nothing: no lookup is running and there's nothing to pick. */
    data object Idle : SuggestionState

    /** A lookup is running. */
    data object Searching : SuggestionState

    /** US addresses matching the latest query. */
    data class Results(val suggestions: List<AddressSuggestion>) : SuggestionState

    /** The latest query matched no US address. */
    data object NoMatches : SuggestionState
}

/** How many geocoder results to ask for before keeping the US ones. */
private const val LOOKUP_RESULTS = 10

/** How many suggestions to show. */
private const val SHOWN_SUGGESTIONS = 5

/** The quiet time after typing before a lookup starts. */
private const val DEBOUNCE_MS = 300L

/** The shortest query that starts a lookup. */
private const val MIN_QUERY_LENGTH = 3

/**
 * Address input with Geocoder suggestions, shown in the layout under the field so they push the
 * next field down instead of covering it. Lookups run only while the field has focus, and only
 * the latest query's results are shown. Picking a suggestion keeps only the street address in the
 * input and hands the structured address to [onAddressSelected].
 */
@Composable
fun AddressSuggestionField(
    value: String,
    onValueChange: (String) -> Unit,
    onAddressSelected: (AddressSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    showLabel: Boolean = true,
    isOptional: Boolean = false,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    fieldHeight: Dp = ControlHeight,
    fieldShape: Shape = ShapeMedium
) {
    val context = LocalContext.current
    val resolvedLabel = label.ifEmpty { stringResource(R.string.label_address) }
    val resolvedPlaceholder = placeholder.ifEmpty { stringResource(R.string.placeholder_start_typing_address) }
    val useAddressLabel = stringResource(R.string.cd_use_address)
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val scope = rememberCoroutineScope()
    val bringIntoView = remember { BringIntoViewRequester() }

    var listState by remember { mutableStateOf<SuggestionState>(SuggestionState.Idle) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var hasFocus by remember { mutableStateOf(false) }

    fun stopLookup() {
        searchJob?.cancel()
        searchJob = null
        listState = SuggestionState.Idle
    }

    LaunchedEffect(listState) {
        if (listState is SuggestionState.Results) bringIntoView.bringIntoView()
    }

    Column(
        modifier = modifier.onFocusChanged { focus ->
            hasFocus = focus.hasFocus
            if (!focus.hasFocus) stopLookup()
        },
        verticalArrangement = Arrangement.spacedBy(ListGap)
    ) {
        ClearChainTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                searchJob?.cancel()
                if (newValue.length >= MIN_QUERY_LENGTH && hasFocus) {
                    searchJob = scope.launch {
                        delay(DEBOUNCE_MS)
                        listState = SuggestionState.Searching
                        val results = try {
                            withContext(Dispatchers.IO) {
                                @Suppress("DEPRECATION")
                                geocoder.getFromLocationName(newValue, LOOKUP_RESULTS)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            null
                        }
                        // A newer keystroke or a focus change cancels this job; drop its answer.
                        ensureActive()
                        if (!hasFocus) return@launch
                        val suggestions = results?.toUsSuggestions()
                        listState = when {
                            suggestions == null -> SuggestionState.Idle
                            suggestions.isEmpty() -> SuggestionState.NoMatches
                            else -> SuggestionState.Results(suggestions)
                        }
                    }
                } else {
                    stopLookup()
                }
            },
            label = if (showLabel) resolvedLabel else "",
            isOptional = isOptional,
            placeholder = resolvedPlaceholder,
            leadingIcon = Icons.Default.Home,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = isError,
            errorMessage = errorMessage,
            fieldHeight = fieldHeight,
            fieldShape = fieldShape
        )

        when (val state = listState) {
            SuggestionState.Idle -> Unit
            SuggestionState.Searching -> Row(
                horizontalArrangement = Arrangement.spacedBy(IconGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InlineSpinner()
                Text(
                    text = stringResource(R.string.address_searching),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SuggestionState.NoMatches -> Text(
                text = stringResource(R.string.address_no_matches),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            is SuggestionState.Results -> ClearChainCard(
                modifier = Modifier.bringIntoViewRequester(bringIntoView)
            ) {
                state.suggestions.forEachIndexed { index, suggestion ->
                    SuggestionRow(
                        suggestion = suggestion,
                        onClickLabel = useAddressLabel,
                        onClick = {
                            onValueChange(suggestion.streetAddress)
                            onAddressSelected(suggestion)
                            listState = SuggestionState.Idle
                        }
                    )
                    if (index < state.suggestions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                start = ControlPadding + ControlIconSize + IconGap,
                                end = ControlPadding
                            ),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

/** One suggestion: the street on the first line, then "City, State ZIP". */
@Composable
private fun SuggestionRow(suggestion: AddressSuggestion, onClickLabel: String, onClick: () -> Unit) {
    val secondLine = listOf(
        suggestion.city,
        listOf(suggestion.state, suggestion.zipCode).filter { it.isNotBlank() }.joinToString(" ")
    ).filter { it.isNotBlank() }.joinToString(", ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clickable(onClickLabel = onClickLabel, onClick = onClick)
            .padding(horizontal = ControlPadding, vertical = ListGap),
        horizontalArrangement = Arrangement.spacedBy(IconGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            modifier = Modifier.size(ControlIconSize),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = suggestion.streetAddress,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (secondLine.isNotBlank()) {
                Text(
                    text = secondLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Keeps the US results, as suggestions, up to [SHOWN_SUGGESTIONS]. */
private fun List<Address>.toUsSuggestions(): List<AddressSuggestion> = asSequence()
    .filter { it.countryCode == "US" }
    .mapNotNull { addr ->
        val line = addr.getAddressLine(0) ?: return@mapNotNull null
        AddressSuggestion(
            displayName = line,
            fullAddress = line,
            streetAddress = addr.toStreetAddress(line),
            city = addr.locality
                ?: addr.subAdminArea
                ?: addr.adminArea
                ?: "",
            state = addr.adminArea ?: "",
            zipCode = addr.postalCode ?: "",
            latitude = addr.latitude,
            longitude = addr.longitude
        )
    }
    .take(SHOWN_SUGGESTIONS)
    .toList()

private fun Address.toStreetAddress(fullAddress: String): String {
    val streetName = thoroughfare?.trim()?.takeIf { it.isNotBlank() }
    val houseNumber = subThoroughfare?.trim()?.takeIf { it.isNotBlank() }
        ?: featureName?.trim()?.takeIf { feature ->
            streetName != null && feature != streetName && feature.any { it.isDigit() }
        }

    return listOfNotNull(houseNumber, streetName)
        .joinToString(" ")
        .takeIf { it.isNotBlank() }
        ?: fullAddress.substringBefore(',').trim()
}
