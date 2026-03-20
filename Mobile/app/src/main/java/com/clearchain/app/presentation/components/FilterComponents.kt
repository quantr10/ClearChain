// FilterComponents.kt
// Common reusable filter components for search, sort, and filter
// NEW LAYOUT: Search Bar → Filter Chips → Results Count + Sort

package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ════════════════════════════════════════════════════════════════════════════════
// SEARCH BAR
// ════════════════════════════════════════════════════════════════════════════════

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors()
    )
}

// ════════════════════════════════════════════════════════════════════════════════
// SORT DROPDOWN
// ════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortDropdown(
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    sortOptions: List<SortOption>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedSort.label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = { Icon(Icons.Default.Sort, null, modifier = Modifier.size(20.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sortOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (selectedSort == option) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// FILTER CHIPS ROW
// ════════════════════════════════════════════════════════════════════════════════

@Composable
fun FilterChipsRow(
    filters: List<FilterChipData>,
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selectedFilter == filter.value,
                onClick = {
                    onFilterSelected(
                        if (selectedFilter == filter.value) null else filter.value
                    )
                },
                label = { Text(filter.label) },
                leadingIcon = if (selectedFilter == filter.value) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// RESULTS COUNT + SORT ROW (Bottom Section)
// ════════════════════════════════════════════════════════════════════════════════

@Composable
fun ResultsCountAndSort(
    count: Int,
    itemName: String = "item",
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    sortOptions: List<SortOption>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Results count on the left
        Text(
            text = "$count $itemName${if (count != 1) "s" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )

        // Sort dropdown on the right
        SortDropdown(
            selectedSort = selectedSort,
            onSortSelected = onSortSelected,
            sortOptions = sortOptions,
            modifier = Modifier.width(160.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// DATA CLASSES
// ════════════════════════════════════════════════════════════════════════════════

data class SortOption(
    val value: String,
    val label: String
)

data class FilterChipData(
    val value: String?,
    val label: String
)

// ════════════════════════════════════════════════════════════════════════════════
// COMMON SORT OPTIONS
// ════════════════════════════════════════════════════════════════════════════════

object CommonSortOptions {
    val CREATED_DATE_DESC = SortOption("date_desc", "Newest")
    val CREATED_DATE_ASC = SortOption("date_asc", "Oldest")
    val DISTRIBUTED_DATE_ASC = SortOption("distributed_date", "Newest")
    val DISTRIBUTED_DATE_DESC = SortOption("distributed_date", "Oldest")
    val NAME_ASC = SortOption("name_asc", "A-Z")
    val NAME_DESC = SortOption("name_desc", "Z-A")
    val EXPIRY_ASC = SortOption("expiry_asc", "Expiring Soon")
    val EXPIRY_DESC = SortOption("expiry_desc", "Expiring Later")
    val PICKUP_DATE_ASC = SortOption("pickup_date_asc", "Oldest")
    val PICKUP_DATE_DESC = SortOption("pickup_date_desc", "Newest")
}