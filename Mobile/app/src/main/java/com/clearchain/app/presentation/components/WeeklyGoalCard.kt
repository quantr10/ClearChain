package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.ui.theme.BrandGreen

/**
 * Pickups completed in the last seven days, against the weekly target. Both sides of a
 * hand-over count the same event, so the NGO collecting the food and the store releasing
 * it see the same card - which is why it lives here rather than in either dashboard.
 *
 * [completed] must be a real count of the last seven days; a figure derived from an
 * all-time total would make the bar move for reasons that have nothing to do with a week.
 */
@Composable
fun WeeklyGoalCard(completed: Int, goal: Int, progress: Float) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.weekly_pickups),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "$completed / $goal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = if (progress >= 1f) BrandGreen else MaterialTheme.colorScheme.primary,
            drawStopIndicator = {}
        )
        Text(
            text = if (progress >= 1f) {
                stringResource(R.string.goal_reached)
            } else {
                stringResource(R.string.goal_remaining, goal - completed)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (progress >= 1f) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
