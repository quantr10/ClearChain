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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.*
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
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPreview by remember { mutableStateOf(false) }  // ✅ NEW

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

    // ✅ Show preview dialog if photo selected
    if (showPreview && photoUri != null) {
        PhotoPreviewDialog(
            photoUri = photoUri,
            onConfirm = {
                onPhotoSelected(photoUri!!)
                showPreview = false
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
    } else {
        // Original picker dialog
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Proof Photo") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Take a photo or choose from gallery to confirm pickup")
                    
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
                            contentDescription = "Camera"
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Take Photo")
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
                            contentDescription = "Gallery"
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Choose from Gallery")
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}