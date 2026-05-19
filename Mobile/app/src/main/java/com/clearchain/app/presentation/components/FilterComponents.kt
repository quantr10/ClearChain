package com.clearchain.app.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.ui.theme.ShapeMedium
import com.clearchain.app.util.HapticUtils

// ════════════════════════════════════════════════════════════════════════════════
// SEARCH BAR
// ════════════════════════════════════════════════════════════════════════════════

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (trailingIcon != null) {
                trailingIcon()
            } else if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, stringResource(R.string.cd_clear_search), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        singleLine = true,
        shape = ShapeMedium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

// ════════════════════════════════════════════════════════════════════════════════
// SORT DROPDOWN  (bottom-sheet style)
// ════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortDropdown(
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    sortOptions: List<SortOption>,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text  = stringResource(R.string.sort_by) + ": ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .clickable { HapticUtils.tick(context); showSheet = true }
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text       = stringResource(selectedSort.labelResId),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint     = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text       = stringResource(R.string.sort_by),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 4.dp, bottom = 8.dp),
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                )
                sortOptions.forEach { option ->
                    val isSelected = selectedSort == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtils.tick(context)
                                onSortSelected(option)
                                showSheet = false
                            }
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = stringResource(option.labelResId),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick  = {
                                HapticUtils.tick(context)
                                onSortSelected(option)
                                showSheet = false
                            }
                        )
                    }
                }
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        items(filters) { filter ->
            val context = LocalContext.current
            FilterChip(
                selected = selectedFilter == filter.value,
                onClick = {
                    HapticUtils.tick(context)
                    onFilterSelected(if (selectedFilter == filter.value) null else filter.value)
                },
                label = { Text(filter.labelResId?.let { stringResource(it) } ?: filter.label, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (selectedFilter == filter.value) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(50)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// STATUS TAB ROW
// ════════════════════════════════════════════════════════════════════════════════

@Composable
fun <T> StatusTabRow(
    tabs: List<Pair<T?, String>>,
    selectedTab: T?,
    onTabSelected: (T?) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp,
        divider = {}
    ) {
        val context = LocalContext.current
        tabs.forEach { (status, label) ->
            Tab(
                selected = selectedTab == status,
                onClick = { HapticUtils.tick(context); onTabSelected(status) },
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == status) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// FILTER SECTION WRAPPER
// ════════════════════════════════════════════════════════════════════════════════

@Composable
fun FilterSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        content()
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// RESULTS COUNT + SORT ROW
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
        Text(
            text = "$count ${itemName}${if (count != 1) "s" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        SortDropdown(
            selectedSort   = selectedSort,
            onSortSelected = onSortSelected,
            sortOptions    = sortOptions
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// DATA CLASSES
// ════════════════════════════════════════════════════════════════════════════════

data class SortOption(val value: String, @StringRes val labelResId: Int)
data class FilterChipData(val value: String?, val label: String = "", @StringRes val labelResId: Int? = null)

// ════════════════════════════════════════════════════════════════════════════════
// COMMON SORT OPTIONS
// ════════════════════════════════════════════════════════════════════════════════

object CommonSortOptions {
    val CREATED_DATE_DESC      = SortOption("date_desc",        R.string.sort_newest)
    val CREATED_DATE_ASC       = SortOption("date_asc",         R.string.sort_oldest)
    val DISTRIBUTED_DATE_ASC   = SortOption("distributed_date", R.string.sort_newest)
    val DISTRIBUTED_DATE_DESC  = SortOption("distributed_date", R.string.sort_oldest)
    val NAME_ASC               = SortOption("name_asc",         R.string.sort_a_to_z)
    val NAME_DESC              = SortOption("name_desc",        R.string.sort_z_to_a)
    val EXPIRY_ASC             = SortOption("expiry_asc",       R.string.sort_expiring_soon)
    val EXPIRY_DESC            = SortOption("expiry_desc",      R.string.sort_expiring_later)
    val PICKUP_DATE_ASC        = SortOption("pickup_date_asc",  R.string.sort_oldest)
    val PICKUP_DATE_DESC       = SortOption("pickup_date_desc", R.string.sort_newest)
}
