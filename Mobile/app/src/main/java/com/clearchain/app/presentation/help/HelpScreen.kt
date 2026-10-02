package com.clearchain.app.presentation.help

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearchain.app.R
import com.clearchain.app.presentation.components.ClearChainCard
import com.clearchain.app.presentation.components.ScreenTitleRow
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.sendEmail

private data class FaqItem(val question: String, val answer: String)

private val FAQS = listOf(
    FaqItem(
        "How do I create a food listing?",
        "Go to your Grocery Dashboard and tap 'Create Listing'. Fill in the product details including name, category, quantity, expiry date, and pickup window. You can optionally add a photo for AI-assisted categorization."
    ),
    FaqItem(
        "How do NGOs request a pickup?",
        "NGOs browse available listings in 'Browse Food'. Tap on a listing and press 'Request Pickup'. Choose your preferred pickup date and time within the store's pickup window."
    ),
    FaqItem(
        "What happens after I approve a pickup request?",
        "The NGO is notified and will arrive at your store during the agreed pickup window. Mark the order as 'Ready' when the items are packed. After pickup, the NGO confirms receipt with a photo."
    ),
    FaqItem(
        "How does verification work?",
        "Both grocery stores and NGOs must be verified by ClearChain admins before full access. Complete your profile and submit supporting information. Verification typically takes 1–3 business days."
    ),
    FaqItem(
        "Can I save listings to review later?",
        "Yes! Tap the heart icon on any listing in Browse Food to save it. Access your saved listings by tapping the heart icon in the top bar of Browse Food."
    ),
    FaqItem(
        "What is the Inventory feature for?",
        "NGOs use Inventory to track food received from pickups. After a pickup is confirmed, items are automatically added to your inventory. You can then mark items as distributed to track your impact."
    ),
    FaqItem(
        "How do I open a dispute?",
        "If a completed pickup had issues (wrong items, poor condition, etc.), go to My Requests, find the completed request, and tap 'Dispute'. Describe the issue and submit — our team will review within 24 hours."
    ),
    FaqItem(
        "How are notifications sent?",
        "Push notifications are sent for new requests, status changes, upcoming pickups, and expiring listings. Manage notification preferences in Settings."
    ),
    FaqItem(
        "Can I change my pickup time?",
        "Contact the NGO or grocery store directly using the contact information on their profile. In-app rescheduling is not currently supported."
    ),
    FaqItem(
        "How do I delete my account?",
        "Go to Profile → tap the menu (⋮) → Delete Account. You will be asked to confirm with your password. This action soft-deletes your account — data is retained for audit purposes."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ScreenTitleRow(
                    title = stringResource(R.string.help),
                    onBack = onNavigateBack
                )
            }
            items(FAQS.size) { idx ->
                FaqCard(FAQS[idx])
            }
            item {
                ClearChainCard(
                    onClick = { sendEmail(context, "support@clearchain.app") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Still need help?",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Contact us at support@clearchain.app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun FaqCard(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevronRotation")
    ClearChainCard(onClick = { expanded = !expanded }) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    item.question,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    item.answer,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
