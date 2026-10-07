package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReviewData(
    val id: String,
    val pickupRequestId: String,
    val reviewerId: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: String
)

@Serializable
data class ReviewsResponse(
    val message: String = "",
    val data: List<ReviewData> = emptyList()
)

@Serializable
data class SubmitReviewRequest(
    val pickupRequestId: String,
    val rating: Int,
    val comment: String? = null
)

@Serializable
data class SubmitReviewResponse(
    val message: String = "",
    val data: ReviewData? = null
)
