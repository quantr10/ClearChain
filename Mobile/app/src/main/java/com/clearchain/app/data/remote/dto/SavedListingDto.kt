package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SavedListingIdsResponse(
    val data: List<String> = emptyList()
)

@Serializable
data class SavedListingToggleResponse(
    val message: String = "",
    val saved: Boolean = false
)
