package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DisputeData(
    val id: String,
    val pickupRequestId: String,
    val initiatorId: String,
    val initiatorName: String,
    val reason: String,
    val ngoStatement: String? = null,
    val groceryStatement: String? = null,
    val photoEvidenceUrl: String? = null,
    val status: String,           // open, under_review, resolved_ngo, resolved_grocery, dismissed
    val adminResolution: String? = null,
    val createdAt: String,
    val resolvedAt: String? = null
)

@Serializable
data class DisputeResponse(
    val message: String = "",
    val data: DisputeData? = null
)

