package com.clearchain.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.launch

private val WheelItemHeight: Dp = 40.dp
private const val WHEEL_VISIBLE_ROWS = 3

/**
 * A single scrollable "slide up/down" column of values (e.g. hours or minutes) that snaps
 * the nearest item to the center once the user stops dragging, similar to a native wheel
 * picker. [values] must already be filtered to whatever range is currently selectable -
 * this composable has no concept of min/max bounds on its own.
 */
@Composable
fun NumberWheel(
    values: List<Int>,
    selected: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    format: (Int) -> String = { it.toString().padStart(2, '0') }
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = values.indexOf(selected).coerceAtLeast(0)
    )
    val coroutineScope = rememberCoroutineScope()

    // The set of selectable values can shift under us (e.g. picking an hour narrows the
    // valid minutes) - if the current selection fell out of range, snap to the closest one.
    LaunchedEffect(values) {
        if (values.isEmpty()) return@LaunchedEffect
        if (values.indexOf(selected) < 0) {
            onSelectedChange(values.first())
        }
        val target = values.indexOf(selected).coerceAtLeast(0)
        listState.scrollToItem(target)
    }

    // Once a drag/fling settles, snap whichever item is nearest the center into place.
    val isScrolling = listState.isScrollInProgress
    LaunchedEffect(isScrolling) {
        if (isScrolling || values.isEmpty()) return@LaunchedEffect
        val info = listState.layoutInfo
        val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
        val centerItem = info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
            ?: return@LaunchedEffect
        val delta = (centerItem.offset + centerItem.size / 2) - viewportCenter
        if (delta != 0) {
            coroutineScope.launch { listState.animateScrollBy(delta.toFloat()) }
        }
        val value = values.getOrNull(centerItem.index)
        if (value != null && value != selected) onSelectedChange(value)
    }

    Box(
        modifier = modifier.height(WheelItemHeight * WHEEL_VISIBLE_ROWS),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = WheelItemHeight),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(values, key = { _, value -> value }) { _, value ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier.fillMaxWidth().height(WheelItemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(value),
                        style = if (isSelected) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }
            }
        }
    }
}

/** Two [NumberWheel]s side by side for picking an hour and a minute. */
@Composable
fun WheelTimePicker(
    hours: List<Int>,
    minutesForHour: (Int) -> List<Int>,
    selectedHour: Int,
    selectedMinute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumberWheel(
            values = hours,
            selected = selectedHour,
            onSelectedChange = onHourChange,
            modifier = Modifier.width(72.dp)
        )
        Text(
            ":",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        NumberWheel(
            values = minutesForHour(selectedHour),
            selected = selectedMinute,
            onSelectedChange = onMinuteChange,
            modifier = Modifier.width(72.dp)
        )
    }
}
