package com.clearchain.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearchain.app.R
import com.clearchain.app.ui.theme.StatusColors

// Chart vocabulary shared by the admin dashboard, the admin statistics screen and
// the NGO / grocery analytics screen, so a bar means the same thing everywhere.
//
// Every chart here pairs its color with a written label: identity never rests on
// hue alone, which is also what keeps the category palette usable for readers who
// cannot separate its red from its green.

data class BarData(val label: String, val value: Int, val color: Color)

/**
 * Horizontal bars, one per row, ranked by whatever order the caller passes.
 * Reads by length; the row label carries identity and the value sits at the tip.
 */
@Composable
fun BarChartContent(
    bars: List<BarData>,
    modifier: Modifier = Modifier,
    valueSuffix: String = ""
) {
    val maxVal = bars.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        bars.forEach { bar ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        bar.label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        "${bar.value}$valueSuffix",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = bar.color
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(bar.value.toFloat() / maxVal)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(bar.color)
                    )
                }
            }
        }
    }
}

/**
 * Vertical columns with the value on the cap and the label underneath - the form the
 * admin dashboard uses for a request-status breakdown.
 */
@Composable
fun ColumnBarChart(
    bars: List<BarData>,
    modifier: Modifier = Modifier,
    height: Int = 120
) {
    val maxValue = bars.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            // A zero column still shows a sliver so the slot reads as empty rather than missing.
            val heightFraction = (bar.value.toFloat() / maxValue).coerceAtLeast(0.02f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    bar.value.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = bar.color
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height((height * heightFraction).dp)
                        .fillMaxWidth(0.6f)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(bar.color)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    // Every label reserves two lines so a wrapping one ("Ready for
                    // pickup") doesn't lift its column off the shared baseline.
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Part-to-whole ring with a legend beside it. Only pass slices of ONE dimension -
 * a ring whose slices come from two different questions double-counts.
 */
@Composable
fun DonutChart(
    slices: List<BarData>,
    modifier: Modifier = Modifier,
    centerValue: String? = null,
    centerLabel: String? = null,
    footnote: String? = null,
    diameter: Int = 104,
    /** Put the legend before the ring, so the ring sits on the trailing edge. */
    legendFirst: Boolean = false
) {
    val visible = slices.filter { it.value > 0 }
    val total = visible.sumOf { it.value }.coerceAtLeast(1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (legendFirst) {
            // No weight here: the legend keeps its own width so the ring follows it
            // instead of being pushed out to the trailing edge.
            DonutLegend(visible, footnote)
            DonutRing(visible, total, centerValue, centerLabel, diameter)
        } else {
            DonutRing(visible, total, centerValue, centerLabel, diameter)
            DonutLegend(visible, footnote, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DonutRing(
    visible: List<BarData>,
    total: Int,
    centerValue: String?,
    centerLabel: String?,
    diameter: Int
) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter.dp)) {
            var startAngle = -90f
            val strokeWidth = size.minDimension * 0.22f
            visible.forEach { slice ->
                val sweep = 360f * slice.value / total
                drawArc(
                    color      = slice.color,
                    startAngle = startAngle,
                    // A 2-degree gap in place of a stroke keeps neighbouring slices apart.
                    sweepAngle = (sweep - 2f).coerceAtLeast(0.5f),
                    useCenter  = false,
                    style      = Stroke(width = strokeWidth),
                    topLeft    = Offset(strokeWidth / 2, strokeWidth / 2),
                    size       = Size(size.width - strokeWidth, size.height - strokeWidth)
                )
                startAngle += sweep
            }
        }
        if (centerValue != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    centerValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                centerLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutLegend(
    visible: List<BarData>,
    footnote: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        visible.forEach { slice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(slice.color)
                )
                Text(
                    "${slice.label}: ${slice.value}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        footnote?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Circular icon badge, label, then the value in a tinted pill - the impact cell the
 * profile and analytics screens already use.
 */
@Composable
fun ImpactStatCell(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f)) {
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A single ratio against its limit, with the sentence that explains the number. */
@Composable
fun RateContent(
    percent: Int,
    description: String,
    modifier: Modifier = Modifier,
    goodThreshold: Int = 70,
    barColor: Color? = null,
    spacing: Dp = 8.dp
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor
                ?: if (percent >= goodThreshold) com.clearchain.app.ui.theme.BrandGreen
                   else MaterialTheme.colorScheme.error,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {}
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The six pickup-request statuses as bars, in pipeline order and always in the same
 * colours. Every screen that charts this breakdown - the admin report, and an NGO or
 * grocery looking at its own - builds its bars here, so a colour or a label means the
 * same thing wherever the reader sees it.
 */
@Composable
fun requestStatusBars(
    pending: Int,
    approved: Int,
    ready: Int,
    completed: Int,
    cancelled: Int,
    rejected: Int
): List<BarData> = listOf(
    BarData(stringResource(R.string.status_pending),   pending,   StatusColors.Pending),
    BarData(stringResource(R.string.status_approved),  approved,  StatusColors.Approved),
    BarData(stringResource(R.string.status_ready),     ready,     StatusColors.Ready),
    BarData(stringResource(R.string.status_completed), completed, StatusColors.Completed),
    BarData(stringResource(R.string.status_cancelled), cancelled, StatusColors.Expired),
    BarData(stringResource(R.string.status_rejected),  rejected,  StatusColors.Rejected)
)
