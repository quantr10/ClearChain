package com.clearchain.app.util

import android.net.Uri

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    object NavigateUp : UiEvent()
    data class ShareFile(val uri: Uri, val mimeType: String = "application/pdf", val title: String = "Share") : UiEvent()
}