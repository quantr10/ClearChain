package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.util.dialPhone
import com.clearchain.app.util.sendEmail

/**
 * One party of a transaction for admin screens: avatar and name with the role underneath, and
 * small round contact buttons, sized like the other action buttons on the detail screens. The avatar opens the party's public profile.
 */
@Composable
fun PartyContactRow(
    role: String,
    name: String,
    avatarUrl: String?,
    email: String?,
    phone: String?,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AvatarImage(imageUrl = avatarUrl, name = name, size = 40, onClick = onOpenProfile)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Both buttons are always drawn so every party row lines up; one is dimmed and inert
        // when that contact detail is missing.
        ContactActionButton(
            icon = Icons.Default.Email,
            contentDescription = stringResource(R.string.action_send_email),
            enabled = !email.isNullOrBlank()
        ) { sendEmail(context, email.orEmpty()) }
        ContactActionButton(
            icon = Icons.Default.Phone,
            contentDescription = stringResource(R.string.action_call),
            enabled = !phone.isNullOrBlank()
        ) { dialPhone(context, phone.orEmpty()) }
    }
}

@Composable
private fun ContactActionButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(28.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(16.dp))
    }
}
