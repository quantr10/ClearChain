package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.clearchain.app.R
import com.clearchain.app.ui.theme.BrandGreen
import java.util.Locale

/**
 * Profile summary card shown as the first section of both the account detail
 * screen and the public profile screen. Both screens render an identical card;
 * only the account detail screen passes [onEdit] to reveal the edit button.
 */
@Composable
fun ProfileSummaryCard(
    name: String,
    roleLabel: String,
    verified: Boolean,
    averageRating: Double,
    reviewCount: Int,
    modifier: Modifier = Modifier,
    profilePictureUrl: String? = null,
    onEdit: (() -> Unit)? = null
) {
    val reviewText = if (reviewCount > 0 && averageRating > 0.0) {
        stringResource(
            R.string.profile_rating_summary,
            String.format(Locale.getDefault(), "%.1f", averageRating),
            reviewCount
        )
    } else {
        stringResource(R.string.profile_detail_no_reviews)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!profilePictureUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profilePictureUrl,
                        contentDescription = name,
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = roleLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (verified) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = stringResource(R.string.label_verified_badge),
                                modifier = Modifier.size(14.dp),
                                tint = BrandGreen
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (averageRating > 0.0) {
                                Color(0xFFFFC107)
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = reviewText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (onEdit != null) {
                ClearChainActionIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.cd_edit_profile),
                    onClick = onEdit,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}
