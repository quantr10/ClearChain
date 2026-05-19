package com.clearchain.app.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.core.content.FileProvider
import com.clearchain.app.util.ImageUtils
import com.google.accompanist.permissions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerDialog(
    onPhotoSelected: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var isCompressing by remember { mutableStateOf(false) }

    // Camera permission state
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            showPreview = true  // ✅ Show preview instead of immediate upload
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            photoUri = it
            showPreview = true  // ✅ Show preview instead of immediate upload
        }
    }

    fun createImageFile(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        val imageFileName = "PICKUP_${timeStamp}.jpg"
        
        val storageDir = File(context.cacheDir, "pickup_photos")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        val imageFile = File(storageDir, imageFileName)
        
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    if (showPreview && photoUri != null) {
        PhotoPreviewDialog(
            photoUri = photoUri,
            onConfirm = {
                val sourceUri = photoUri!!
                isCompressing = true
                showPreview = false
                scope.launch {
                    val compressedUri = withContext(Dispatchers.IO) {
                        try {
                            val file = ImageUtils.compressImage(context, sourceUri)
                            Uri.fromFile(file)
                        } catch (_: Exception) {
                            sourceUri // fall back to original on error
                        }
                    }
                    isCompressing = false
                    onPhotoSelected(compressedUri)
                }
            },
            onRetake = {
                showPreview = false
                photoUri = null
                // Don't dismiss picker, let user choose again
            },
            onDismiss = {
                showPreview = false
                photoUri = null
                onDismiss()
            }
        )
    } else if (isCompressing) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.label_processing_photo)) },
            text = {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.label_compressing_image))
                }
            },
            confirmButton = {}
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.label_add_proof_photo)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.label_proof_photo_hint))

                    OutlinedButton(
                        onClick = {
                            if (cameraPermissionState.status.isGranted) {
                                photoUri = createImageFile()
                                cameraLauncher.launch(photoUri!!)
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = stringResource(R.string.cd_camera)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_take_photo_camera))
                    }

                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.cd_gallery)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_choose_gallery))
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}