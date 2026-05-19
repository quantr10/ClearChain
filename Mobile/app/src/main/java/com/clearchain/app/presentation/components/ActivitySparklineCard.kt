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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.data.remote.dto.ActivityItemData
import java.text.SimpleDateFormat
import java.util.*

/**
 * Aggregates activity items into daily counts for the last [days] days.
 * Returns a list of (dayLabel, count) from oldest to newest.
 */
fun buildDailyActivityCounts(
    activities: List<ActivityItemData>,
    days: Int = 7
): List<Pair<String, Int>> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val counts = mutableMapOf<String, Int>()
    val keys = mutableListOf<String>()
    for (i in days - 1 downTo 0) {
        val cal = today.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, -i)
        val key = fmt.format(cal.time)
        counts[key] = 0
        keys.add(key)
    }

    activities.forEach { item ->
        val key = item.timestamp.take(10)
        if (counts.containsKey(key)) counts[key] = (counts[key] ?: 0) + 1
    }

    return keys.map { key ->
        val cal = Calendar.getInstance()
        try { cal.time = fmt.parse(key)!! } catch (_: Exception) {}
        val label = dayLabels[cal.get(Calendar.DAY_OF_WEEK) - 1]
        label to (counts[key] ?: 0)
    }
}

@Composable
fun ActivitySparklineCard(
    title: String,
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || data.all { it.second == 0 }) return

    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val dotColor  = MaterialTheme.colorScheme.primary
    val maxVal    = data.maxOf { it.second }.coerceAtLeast(1).toFloat()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
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
                    .height(72.dp)
            ) {
                val w = size.width
                val h = size.height
                val n = data.size
                if (n < 2) return@Canvas

                val stepX = w / (n - 1).toFloat()

                fun xAt(i: Int) = i * stepX
                fun yAt(v: Int) = h - (v / maxVal) * h * 0.85f - h * 0.05f

                // Fill path
                val fillPath = Path().apply {
                    moveTo(xAt(0), h)
                    lineTo(xAt(0), yAt(data[0].second))
                    for (i in 1 until n) {
                        val cpX = xAt(i - 1) + stepX / 2f
                        cubicTo(cpX, yAt(data[i - 1].second), cpX, yAt(data[i].second), xAt(i), yAt(data[i].second))
                    }
                    lineTo(xAt(n - 1), h)
                    close()
                }
                drawPath(fillPath, brush = Brush.verticalGradient(listOf(fillColor, fillColor.copy(alpha = 0f))))

                // Line path
                val linePath = Path().apply {
                    moveTo(xAt(0), yAt(data[0].second))
                    for (i in 1 until n) {
                        val cpX = xAt(i - 1) + stepX / 2f
                        cubicTo(cpX, yAt(data[i - 1].second), cpX, yAt(data[i].second), xAt(i), yAt(data[i].second))
                    }
                }
                drawPath(linePath, color = lineColor, style = Stroke(width = 3f))

                // Dots
                data.forEachIndexed { i, (_, v) ->
                    drawCircle(color = dotColor, radius = 5f, center = Offset(xAt(i), yAt(v)))
                    drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 3f, center = Offset(xAt(i), yAt(v)))
                }
            }

            // Day labels
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                data.forEach { (label, _) ->
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
