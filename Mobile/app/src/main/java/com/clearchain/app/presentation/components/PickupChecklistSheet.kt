package com.clearchain.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R

private const val CHECKLIST_SIZE = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupChecklistSheet(onDismiss: () -> Unit, onNext: () -> Unit) {
    val checklistItems = listOf(
        stringResource(R.string.checklist_item_1),
        stringResource(R.string.checklist_item_2),
        stringResource(R.string.checklist_item_3),
        stringResource(R.string.checklist_item_4),
        stringResource(R.string.checklist_item_5)
    )
    var checkedItems by remember { mutableStateOf(setOf<Int>()) }
    val allChecked = checkedItems.size == CHECKLIST_SIZE

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.label_pickup_verification_checklist),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${checkedItems.size}/$CHECKLIST_SIZE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            checklistItems.forEachIndexed { index, item ->
                val checked = index in checkedItems
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clickable {
                            checkedItems = if (checked) checkedItems - index else checkedItems + index
                        },
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked         = checked,
                        onCheckedChange = {
                            checkedItems = if (checked) checkedItems - index else checkedItems + index
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        item,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Button(
                onClick  = onNext,
                enabled  = allChecked,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text(stringResource(R.string.next))
            }
        }
    }
}
