package com.clearchain.app.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.Dp
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
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .border(1.dp, borderColor, ShapeMedium)
            .padding(horizontal = 8.dp),
        textStyle = MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Search, null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
                if (trailingIcon != null) {
                    trailingIcon()
                } else if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            stringResource(R.string.cd_clear_search),
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

/**
 * Row 1 of every list-screen header: a full-width [SearchBar] plus optional trailing
 * action buttons (filter / cart / location / export …). Owns the header's horizontal
 * gutter and vertical rhythm so screens can't drift apart.
 */
@Composable
fun ListHeaderSearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    // Disable the 48.dp min-touch-target inflation so the 32.dp search field + 24.dp action
    // buttons don't add 8.dp of dead space above/below the row — keeps it flush on the
    // header's 8.dp rhythm.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = placeholder,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
    }
}

/**
 * Stacks a list screen's header rows ([ListHeaderSearchRow], [FilterChipsRow],
 * [ResultsCountAndSort], …) on the SAME 8.dp vertical rhythm the content [LazyColumn]
 * uses for its cards (`verticalArrangement = Arrangement.spacedBy(8.dp)`), so the gaps
 * between header rows match the gaps between cards. No bottom padding — the list's own
 * `contentPadding` supplies the 8.dp gap to the first card.
 */
@Composable
fun ListScreenHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
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
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text  = stringResource(R.string.sort_by) + ": ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .clickable { HapticUtils.tick(context); showSheet = true }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text       = stringResource(selectedSort.labelResId),
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
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
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text       = stringResource(R.string.sort_by),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
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
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(filters) { filter ->
            val context = LocalContext.current
            HeaderChip(
                selected = selectedFilter == filter.value,
                label = filter.labelResId?.let { stringResource(it) } ?: filter.label,
                onClick = {
                    HapticUtils.tick(context)
                    onFilterSelected(if (selectedFilter == filter.value) null else filter.value)
                }
            )
        }
    }
}

/**
 * Typed variant of [FilterChipsRow] for a fixed tab set whose value is an enum / boolean /
 * nullable string rather than a [FilterChipData] list. Single-select, no toggle-off.
 * Same gutter, spacing, chip shape and label style as the [FilterChipData] overload.
 */
@Composable
fun <T> FilterChipsRow(
    tabs: List<Pair<T, String>>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(tabs) { (tab, label) ->
            HeaderChip(
                selected = selectedTab == tab,
                label = label,
                onClick = {
                    HapticUtils.tick(context)
                    onTabSelected(tab)
                }
            )
        }
    }
}

/**
 * A [FilterChip] rendered at its 32.dp visual pill height, with the 48.dp minimum-touch-target
 * inflation disabled so header chip rows sit flush on the same 8.dp vertical rhythm as the search
 * row and the content cards. (The chips are wide, so horizontal touch area stays comfortable.)
 */
@Composable
private fun HeaderChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            shape = RoundedCornerShape(50)
        )
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
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
    modifier: Modifier = Modifier,
    countText: String? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leadingContent?.invoke(this)
            Text(
                text = countText ?: "$count ${itemName}${if (count != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
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
