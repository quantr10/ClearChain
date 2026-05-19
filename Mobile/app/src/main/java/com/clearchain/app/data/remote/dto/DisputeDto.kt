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

@Serializable
data class DisputeListResponse(
    val message: String = "",
    val data: List<DisputeData> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalPages: Int = 1
)

@Serializable
data class AddStatementRequest(
    val statement: String
)
