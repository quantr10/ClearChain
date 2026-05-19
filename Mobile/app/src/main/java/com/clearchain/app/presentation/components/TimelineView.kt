package com.clearchain.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.domain.model.PickupRequestStatus

data class TimelineStep(
    val label: String,
    val description: String? = null,
    val timestamp: String? = null,
    val icon: ImageVector,
    val isCompleted: Boolean,
    val isActive: Boolean = false
)

@Composable
fun TimelineView(
    steps: List<TimelineStep>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        steps.forEachIndexed { index, step ->
            TimelineItem(
                step = step,
                isLast = index == steps.lastIndex
            )
        }
    }
}

@Composable
private fun TimelineItem(
    step: TimelineStep,
    isLast: Boolean
) {
    val dotColor = when {
        step.isActive    -> MaterialTheme.colorScheme.primary
        step.isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else             -> MaterialTheme.colorScheme.outlineVariant
    }
    val lineColor = if (step.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textColor = when {
        step.isActive    -> MaterialTheme.colorScheme.onSurface
        step.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        else             -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left: dot + connector line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (step.isActive || step.isCompleted) dotColor.copy(alpha = 0.15f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = Color.White
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(lineColor)
                )
            }
        }

        // Right: content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = step.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (step.isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
            step.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            step.timestamp?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/** Build timeline steps from a PickupRequestStatus */
@Composable
fun pickupRequestTimeline(status: PickupRequestStatus): List<TimelineStep> {
    val statusOrdinal = status.ordinal
    return listOf(
        TimelineStep(
            label = stringResource(R.string.timeline_request_submitted),
            description = stringResource(R.string.timeline_submitted_desc),
            icon = Icons.Default.Edit,
            isCompleted = statusOrdinal >= 0,
            isActive = statusOrdinal == 0
        ),
        TimelineStep(
            label = stringResource(R.string.timeline_approved_by_store),
            description = stringResource(R.string.timeline_approved_desc),
            icon = Icons.Default.ThumbUp,
            isCompleted = statusOrdinal >= 1,
            isActive = statusOrdinal == 1
        ),
        TimelineStep(
            label = stringResource(R.string.timeline_ready_for_pickup),
            description = stringResource(R.string.timeline_ready_desc),
            icon = Icons.Default.Inventory2,
            isCompleted = statusOrdinal >= 2,
            isActive = statusOrdinal == 2
        ),
        TimelineStep(
            label = stringResource(R.string.timeline_pickup_confirmed),
            description = stringResource(R.string.timeline_confirmed_desc),
            icon = Icons.Default.CheckCircle,
            isCompleted = statusOrdinal >= 3,
            isActive = statusOrdinal == 3
        )
    )
}
