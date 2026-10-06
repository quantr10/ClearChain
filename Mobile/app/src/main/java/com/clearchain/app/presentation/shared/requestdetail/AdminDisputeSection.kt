package com.clearchain.app.presentation.shared.requestdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.DisputeListItemData
import com.clearchain.app.data.remote.dto.MyDisputeData
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.dispute.DisputeStatusBadge
import com.clearchain.app.presentation.dispute.NgoDisputeReason

/**
 * The dispute on a pickup, as the admin sees it: where it stands (open, under review, resolved),
 * what the NGO reported, and the action that moves it to the next step. The party contact
 * details are already on the request screen, so they are not repeated here.
 */
@Composable
internal fun AdminDisputeSection(
    dispute: DisputeListItemData,
    isStartingReview: Boolean,
    onStartReview: () -> Unit,
    onResolve: () -> Unit
) {
    DisputeSummaryCard(
        status = dispute.status,
        reason = dispute.reason,
        description = dispute.ngoStatement,
        groceryStatement = dispute.groceryStatement,
        adminResolution = dispute.adminResolution,
        photoUrl = dispute.photoEvidenceUrl
    ) {
        when (dispute.status) {
            "open" -> ClearChainButton(
                text = stringResource(R.string.dispute_start_review),
                icon = Icons.Default.Visibility,
                onClick = onStartReview,
                loading = isStartingReview,
                enabled = !isStartingReview
            )
            "under_review" -> ClearChainButton(
                text = stringResource(R.string.dispute_mark_resolved),
                icon = Icons.Default.CheckCircle,
                onClick = onResolve
            )
        }
    }
}

/** The NGO's own dispute on a pickup: the same card the admin sees, without the actions. */
@Composable
internal fun MyDisputeSection(dispute: MyDisputeData) {
    DisputeSummaryCard(
        status = dispute.status,
        reason = dispute.reason,
        description = dispute.ngoStatement,
        groceryStatement = null,
        adminResolution = dispute.adminResolution,
        photoUrl = dispute.photoEvidenceUrl
    )
}

@Composable
private fun DisputeSummaryCard(
    status: String,
    reason: String,
    description: String?,
    groceryStatement: String?,
    adminResolution: String?,
    photoUrl: String?,
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    SectionCard(
        title = stringResource(R.string.disputes),
        trailing = { DisputeStatusBadge(status) }
    ) {
        // Reasons are stored as keys; older rows may hold free text, which is shown as-is.
        val reasonLabel = NgoDisputeReason.fromKey(reason)?.let { stringResource(it.labelRes) } ?: reason
        DisputeField(stringResource(R.string.dispute_reason_section), reasonLabel)

        description?.takeIf { it.isNotBlank() }?.let {
            DisputeField(stringResource(R.string.dispute_ngo_statement), it)
        }
        groceryStatement?.takeIf { it.isNotBlank() }?.let {
            DisputeField(stringResource(R.string.dispute_grocery_statement), it)
        }
        adminResolution?.takeIf { it.isNotBlank() }?.let {
            DisputeField(stringResource(R.string.dispute_admin_resolution), it)
        }

        photoUrl?.takeIf { it.isNotBlank() }?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = stringResource(R.string.dispute_photo_evidence),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        actions()
    }
}

@Composable
private fun DisputeField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // One line is exactly as tall as the action buttons; longer text grows from there.
        ClearChainSurfaceCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ClearChainButtonDefaults.Height)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
internal fun ResolveDisputeDialog(
    outcome: String,
    groceryStatement: String,
    note: String,
    isResolving: Boolean,
    onOutcomeChange: (String) -> Unit,
    onGroceryStatementChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val outcomes = listOf(
        "resolved_ngo" to stringResource(R.string.dispute_outcome_ngo),
        "resolved_grocery" to stringResource(R.string.dispute_outcome_grocery),
        "dismissed" to stringResource(R.string.dispute_outcome_dismissed)
    )

    ConfirmDialog(
        onDismiss = onDismiss,
        icon = Icons.Default.Gavel,
        title = stringResource(R.string.dispute_resolve_title),
        confirmLabel = stringResource(R.string.dispute_mark_resolved),
        dismissLabel = stringResource(R.string.cancel),
        confirmEnabled = !isResolving,
        confirmLoading = isResolving,
        onConfirm = onConfirm
    ) {
        Text(stringResource(R.string.dispute_resolve_outcome_label), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            outcomes.forEach { (value, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOutcomeChange(value) }
                ) {
                    RadioButton(selected = outcome == value, onClick = { onOutcomeChange(value) })
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        ClearChainTextField(
            value = groceryStatement,
            onValueChange = onGroceryStatementChange,
            label = stringResource(R.string.dispute_grocery_statement),
            isOptional = true,
            placeholder = stringResource(R.string.dispute_grocery_statement_placeholder),
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        ClearChainTextField(
            value = note,
            onValueChange = onNoteChange,
            label = stringResource(R.string.dispute_resolution_note),
            placeholder = stringResource(R.string.dispute_resolution_note_placeholder),
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
