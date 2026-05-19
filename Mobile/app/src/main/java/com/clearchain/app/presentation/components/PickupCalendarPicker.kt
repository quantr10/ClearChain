package com.clearchain.app.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val DAY_HEADERS = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

/**
 * Inline calendar month grid for picking a pickup date.
 *
 * @param selectedDate  Currently selected date ("YYYY-MM-DD") or empty.
 * @param onDateSelected Called with "YYYY-MM-DD" when user taps a valid day.
 * @param maxDate       Optional upper bound (e.g. listing expiry). Days after this are disabled.
 * @param enabled       When false the calendar is non-interactive.
 */
@Composable
fun PickupCalendarPicker(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxDate: LocalDate? = null,
    enabled: Boolean = true
) {
    val today = LocalDate.now()
    val parsed = remember(selectedDate) {
        runCatching { LocalDate.parse(selectedDate) }.getOrNull()
    }

    var displayMonth by remember(parsed) {
        mutableStateOf(YearMonth.from(parsed ?: today))
    }
    var slideDir by remember { mutableIntStateOf(1) }

    Surface(
        modifier      = modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(16.dp),
        color         = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Month navigation header ───────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick  = {
                        slideDir = -1
                        displayMonth = displayMonth.minusMonths(1)
                    },
                    enabled  = enabled && displayMonth.isAfter(YearMonth.now().minusMonths(1))
                ) {
                    Icon(Icons.Default.ChevronLeft, stringResource(R.string.cd_prev_month))
                }

                AnimatedContent(
                    targetState   = displayMonth,
                    transitionSpec = {
                        (slideInHorizontally { if (slideDir > 0) it else -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (slideDir > 0) -it else it } + fadeOut())
                    },
                    label = "month"
                ) { month ->
                    Text(
                        text       = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.width(180.dp)
                    )
                }

                IconButton(
                    onClick  = {
                        slideDir = 1
                        displayMonth = displayMonth.plusMonths(1)
                    },
                    enabled  = enabled && (maxDate == null || displayMonth.isBefore(YearMonth.from(maxDate).plusMonths(1)))
                ) {
                    Icon(Icons.Default.ChevronRight, stringResource(R.string.cd_next_month))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Day-of-week header row ────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                DAY_HEADERS.forEach { label ->
                    Text(
                        text      = label,
                        modifier  = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Day grid ──────────────────────────────────────────────
            AnimatedContent(
                targetState   = displayMonth,
                transitionSpec = {
                    (slideInHorizontally { if (slideDir > 0) it else -it } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (slideDir > 0) -it else it } + fadeOut())
                },
                label = "days"
            ) { month ->
                val firstDay  = month.atDay(1)
                // Offset so Sunday = 0
                val startOffset = (firstDay.dayOfWeek.value % 7)
                val daysInMonth = month.lengthOfMonth()
                val totalCells  = startOffset + daysInMonth
                val rows        = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(rows) { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            repeat(7) { col ->
                                val cellIndex = row * 7 + col
                                val dayNum    = cellIndex - startOffset + 1
                                if (dayNum < 1 || dayNum > daysInMonth) {
                                    Box(Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val date      = month.atDay(dayNum)
                                    val isPast    = date.isBefore(today)
                                    val isAfterMax = maxDate != null && date.isAfter(maxDate)
                                    val isDisabled = isPast || isAfterMax || !enabled
                                    val isSelected = parsed == date
                                    val isToday    = date == today

                                    DayCell(
                                        day        = dayNum,
                                        isSelected = isSelected,
                                        isToday    = isToday,
                                        isDisabled = isDisabled,
                                        modifier   = Modifier.weight(1f),
                                        onClick    = { if (!isDisabled) onDateSelected(date.toString()) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Selected date label ───────────────────────────────────
            if (parsed != null) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(Modifier.size(8.dp))
                    }
                    Text(
                        "Selected: ${parsed.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
                            "${parsed.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${parsed.dayOfMonth}, ${parsed.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day:        Int,
    isSelected: Boolean,
    isToday:    Boolean,
    isDisabled: Boolean,
    modifier:   Modifier = Modifier,
    onClick:    () -> Unit
) {
    val primary      = MaterialTheme.colorScheme.primary
    val onPrimary    = MaterialTheme.colorScheme.onPrimary
    val outline      = MaterialTheme.colorScheme.outline
    val onSurface    = MaterialTheme.colorScheme.onSurface
    val surfaceVar   = MaterialTheme.colorScheme.surfaceVariant

    val bgColor   = when {
        isSelected -> primary
        else       -> Color.Transparent
    }
    val textColor = when {
        isSelected -> onPrimary
        isDisabled -> onSurface.copy(alpha = 0.3f)
        isToday    -> primary
        else       -> onSurface
    }
    val borderColor = when {
        isToday && !isSelected -> primary
        else                   -> Color.Transparent
    }

    Box(
        modifier          = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bgColor)
            .then(
                if (isToday && !isSelected)
                    Modifier.clip(CircleShape)
                else Modifier
            )
            .clickable(enabled = !isDisabled, onClick = onClick),
        contentAlignment  = Alignment.Center
    ) {
        if (isToday && !isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.12f))
            )
        }
        Text(
            text      = day.toString(),
            style     = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color     = textColor,
            textAlign = TextAlign.Center
        )
    }
}
