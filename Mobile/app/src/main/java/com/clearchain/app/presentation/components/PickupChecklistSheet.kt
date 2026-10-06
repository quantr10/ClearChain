package com.clearchain.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clearchain.app.R

private const val CHECKLIST_SIZE = 5

/** The five checks an NGO ticks before confirming a pickup; Next stays disabled until all are ticked. */
@Composable
fun PickupChecklistDialog(onDismiss: () -> Unit, onNext: () -> Unit) {
    val checklistItems = listOf(
        stringResource(R.string.checklist_item_1),
        stringResource(R.string.checklist_item_2),
        stringResource(R.string.checklist_item_3),
        stringResource(R.string.checklist_item_4),
        stringResource(R.string.checklist_item_5)
    )
    var checkedItems by remember { mutableStateOf(setOf<Int>()) }
    val allChecked = checkedItems.size == CHECKLIST_SIZE

    ConfirmDialog(
        onDismiss = onDismiss,
        icon = Icons.Default.CheckCircle,
        title = stringResource(R.string.label_pickup_verification_checklist),
        message = stringResource(R.string.msg_pickup_checklist),
        confirmLabel = stringResource(R.string.next),
        dismissLabel = stringResource(R.string.cancel),
        confirmEnabled = allChecked,
        onConfirm = onNext
    ) {
        Text(
            "${checkedItems.size}/$CHECKLIST_SIZE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            checklistItems.forEachIndexed { index, item ->
                val checked = index in checkedItems
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            checkedItems = if (checked) checkedItems - index else checkedItems + index
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        item,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (checked) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            checkedItems = if (checked) checkedItems - index else checkedItems + index
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
