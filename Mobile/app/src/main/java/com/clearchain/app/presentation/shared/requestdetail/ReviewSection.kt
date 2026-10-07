package com.clearchain.app.presentation.shared.requestdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.presentation.components.ClearChainButtonDefaults
import com.clearchain.app.presentation.components.ClearChainSurfaceCard
import com.clearchain.app.presentation.components.SectionCard
import com.clearchain.app.util.DateTimeUtils
import java.util.Locale

private val StarAmber = Color(0xFFFFC107)

/**
 * A rating on a pickup, shown on its own: the score in a tile, the stars beside it with a word for
 * the score and the date underneath, then what was said. It says nothing about who wrote it, since
 * the card's [title] ("Your rating", "Rating from NGO") already does. Used for the NGO's own rating
 * and for the rating a grocery or an admin looks at, so all three read the same.
 */
@Composable
internal fun ReviewSection(
    title: String,
    rating: Int,
    comment: String?,
    createdAt: String
) {
    val word = when (rating) {
        1 -> R.string.review_rating_poor
        2 -> R.string.review_rating_below_avg
        3 -> R.string.review_rating_average
        4 -> R.string.review_rating_good
        5 -> R.string.review_rating_excellent
        else -> null
    }?.let { stringResource(it) }
    val date = DateTimeUtils.formatDate(createdAt)
    val description = stringResource(R.string.cd_rated_out_of_five, rating) + ", " + date

    SectionCard(title) {
        Row(
            // One announcement for the whole row instead of a number, five unlabeled icons and a date.
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        String.format(Locale.getDefault(), "%.1f", rating.toFloat()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { index ->
                        val filled = index < rating
                        Icon(
                            imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (filled) StarAmber else MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Text(
                    listOfNotNull(word, date).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // The same grey block the dispute card uses for free text; a rating with no comment simply
        // has no block.
        comment?.takeIf { it.isNotBlank() }?.let { text ->
            ClearChainSurfaceCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ClearChainButtonDefaults.Height)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** What a grocery sees before the NGO has rated: five faint stars and a line saying so. */
@Composable
internal fun NoReviewYet(hint: String) {
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = hint },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) {
                Icon(
                    Icons.Default.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        // fill = false: the line takes only the room it needs but may wrap, so a longer translation
        // never pushes past the card edge.
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
