package com.clearchain.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.ActivityItemData
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Aggregates activity items into daily counts for the last [days] days.
 * Returns a list of (dayLabel, count) from oldest to newest.
 */
fun buildDailyActivityCounts(
    activities: List<ActivityItemData>,
    days: Int = 7
): List<Pair<String, Int>> {
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val labelFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val parsedDates = activities.mapNotNull { parseActivityDate(it.timestamp) }
    val endDate = parsedDates.maxOrNull() ?: LocalDate.now()
    val startDate = endDate.minusDays((days - 1).toLong())

    val counts = mutableMapOf<String, Int>()
    val keys = mutableListOf<String>()
    for (i in 0 until days) {
        val date = startDate.plusDays(i.toLong())
        val key = date.format(dateFormatter)
        counts[key] = 0
        keys.add(key)
    }

    parsedDates.forEach { date ->
        val key = date.format(dateFormatter)
        if (counts.containsKey(key)) counts[key] = (counts[key] ?: 0) + 1
    }

    if (parsedDates.isEmpty() && activities.isNotEmpty()) {
        counts[keys.last()] = activities.size
    }

    return keys.map { key ->
        val label = runCatching {
            LocalDate.parse(key, dateFormatter).format(labelFormatter)
        }.getOrDefault(key.takeLast(2))
        label to (counts[key] ?: 0)
    }
}

private fun parseActivityDate(timestamp: String): LocalDate? {
    val offsetDate = runCatching {
        OffsetDateTime.parse(timestamp)
    }.getOrNull()?.atZoneSameInstant(ZoneId.systemDefault())?.toLocalDate()
    if (offsetDate != null) return offsetDate

    return Regex("\\d{4}-\\d{2}-\\d{2}").find(timestamp)?.value?.let { date ->
        runCatching { LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
    }
}

@Composable
fun ActivitySparklineCard(
    title: String,
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val dotColor  = MaterialTheme.colorScheme.primary
    val hasData = data.isNotEmpty() && data.any { it.second > 0 }
    val chartData = if (data.isNotEmpty()) data else listOf(
        "Sun" to 0, "Mon" to 0, "Tue" to 0, "Wed" to 0, "Thu" to 0, "Fri" to 0, "Sat" to 0
    )
    val maxVal = chartData.maxOf { it.second }.coerceAtLeast(1).toFloat()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = "Last 7 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
            ) {
                val w = size.width
                val h = size.height
                val n = chartData.size
                if (n < 2) return@Canvas

                val stepX = w / (n - 1).toFloat()

                fun xAt(i: Int) = i * stepX
                fun yAt(v: Int): Float {
                    val normalized = if (hasData) v / maxVal else 0.18f
                    return h - normalized * h * 0.85f - h * 0.05f
                }

                // Fill path
                val fillPath = Path().apply {
                    moveTo(xAt(0), h)
                    lineTo(xAt(0), yAt(chartData[0].second))
                    for (i in 1 until n) {
                        val cpX = xAt(i - 1) + stepX / 2f
                        cubicTo(cpX, yAt(chartData[i - 1].second), cpX, yAt(chartData[i].second), xAt(i), yAt(chartData[i].second))
                    }
                    lineTo(xAt(n - 1), h)
                    close()
                }
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        listOf(
                            if (hasData) fillColor else fillColor.copy(alpha = 0.05f),
                            fillColor.copy(alpha = 0f)
                        )
                    )
                )

                // Line path
                val linePath = Path().apply {
                    moveTo(xAt(0), yAt(chartData[0].second))
                    for (i in 1 until n) {
                        val cpX = xAt(i - 1) + stepX / 2f
                        cubicTo(cpX, yAt(chartData[i - 1].second), cpX, yAt(chartData[i].second), xAt(i), yAt(chartData[i].second))
                    }
                }
                drawPath(
                    linePath,
                    color = if (hasData) lineColor else lineColor.copy(alpha = 0.35f),
                    style = Stroke(width = 3f)
                )

                // Dots
                chartData.forEachIndexed { i, (_, v) ->
                    drawCircle(
                        color = if (hasData) dotColor else dotColor.copy(alpha = 0.35f),
                        radius = 5f,
                        center = Offset(xAt(i), yAt(v))
                    )
                    drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 3f, center = Offset(xAt(i), yAt(v)))
                }
            }

            if (!hasData) {
                Text(
                    text = stringResource(R.string.no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Day labels
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                chartData.forEach { (label, _) ->
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
