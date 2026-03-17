package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdateProfileRequest(
    val name: String,
    val phone: String?,
    val address: String?,
    val location: String?,
    val hours: String?
)