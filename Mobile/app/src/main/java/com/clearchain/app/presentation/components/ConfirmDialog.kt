package com.clearchain.app.presentation.components

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.clearchain.app.R
import com.clearchain.app.util.HapticUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

/**
 * The single dialog shell for the whole app. Every [AlertDialog] routes through
 * here so the container colour, title treatment, button styling and haptics stay
 * identical everywhere.
 *
 * - Simple confirmations: pass [title] / [message] / [confirmLabel].
 * - Custom body UI (text fields, pickers, lists, images): pass [content]; it is
 *   laid out in a column with 12.dp spacing directly under the optional [message].
 * - Info / picker dialogs with no confirm action: set [showConfirmButton] = false.
 * - Blocking progress dialogs: set [dismissible] = false and hide both buttons.
 */
@Composable
fun ConfirmDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    message: String = "",
    confirmLabel: String = "",
    dismissLabel: String = "",
    isDestructive: Boolean = false,
    icon: ImageVector? = null,
    confirmIcon: ImageVector? = null,
    confirmEnabled: Boolean = true,
    confirmLoading: Boolean = false,
    dismissEnabled: Boolean = true,
    showConfirmButton: Boolean = true,
    showDismissButton: Boolean = true,
    dismissible: Boolean = true,
    onConfirm: () -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val accent = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val resolvedConfirm = confirmLabel.ifEmpty { stringResource(R.string.btn_confirm) }
    val resolvedDismiss = dismissLabel.ifEmpty { stringResource(R.string.btn_cancel) }

    val iconSlot: (@Composable () -> Unit)? = icon?.let {
        { Icon(imageVector = it, contentDescription = null, tint = accent) }
    }

    val titleSlot: (@Composable () -> Unit)? = title.takeIf { it.isNotBlank() }?.let { text ->
        {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified
            )
        }
    }

    val bodySlot: (@Composable () -> Unit)? =
        if (message.isBlank() && content == null) {
            null
        } else {
            {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (message.isNotBlank()) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    content?.invoke(this)
                }
            }
        }

    val dismissSlot: (@Composable () -> Unit)? =
        if (!showDismissButton) {
            null
        } else {
            {
                ClearChainOutlinedButton(
                    text = resolvedDismiss,
                    onClick = onDismiss,
                    enabled = dismissEnabled
                )
            }
        }

    AlertDialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible
        ),
        icon = iconSlot,
        title = titleSlot,
        text = bodySlot,
        confirmButton = {
            if (showConfirmButton) {
                ClearChainButton(
                    text = resolvedConfirm,
                    onClick = {
                        if (isDestructive) HapticUtils.warning(context)
                        onConfirm()
                    },
                    enabled = confirmEnabled,
                    loading = confirmLoading,
                    icon = confirmIcon,
                    fillMaxWidth = false,
                    containerColor = accent,
                    contentColor = if (isDestructive) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
            }
        },
        dismissButton = dismissSlot
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotoPickerDialog(
    onPhotoSelected: (Uri) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.label_add_photo),
    message: String = stringResource(R.string.msg_photo_source),
    previewMessage: String = stringResource(R.string.msg_use_this_photo),
    confirmLabel: String = stringResource(R.string.action_confirm_upload)
) {
    val context = LocalContext.current

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var cameraTarget by remember { mutableStateOf<Uri?>(null) }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) pickedUri = cameraTarget }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) pickedUri = uri }

    fun launchCamera() {
        val dir = File(context.cacheDir, "photo_uploads").apply { mkdirs() }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(dir, "IMG_${System.currentTimeMillis()}.jpg")
        )
        cameraTarget = uri
        cameraLauncher.launch(uri)
    }

    val current = pickedUri
    if (current == null) {
        ConfirmDialog(
            onDismiss = onDismiss,
            icon = Icons.Default.AddAPhoto,
            title = title,
            message = message,
            dismissLabel = stringResource(R.string.cancel),
            showConfirmButton = false
        ) {
            ClearChainButton(
                text = stringResource(R.string.action_take_photo_camera),
                onClick = {
                    if (cameraPermission.status.isGranted) {
                        launchCamera()
                    } else {
                        cameraPermission.launchPermissionRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.PhotoCamera
            )
            ClearChainOutlinedButton(
                text = stringResource(R.string.action_choose_gallery),
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Photo
            )
        }
    } else {
        ConfirmDialog(
            onDismiss = onDismiss,
            onConfirm = { onPhotoSelected(current) },
            icon = Icons.Default.Image,
            title = stringResource(R.string.label_photo_preview),
            message = previewMessage,
            confirmLabel = confirmLabel,
            dismissLabel = stringResource(R.string.cancel)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(240.dp)
            ) {
                AsyncImage(
                    model = current,
                    contentDescription = stringResource(R.string.cd_preview_image),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            ClearChainOutlinedButton(
                text = stringResource(R.string.action_retake),
                onClick = { pickedUri = null },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
