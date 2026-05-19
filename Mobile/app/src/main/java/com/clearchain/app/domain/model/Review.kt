package com.clearchain.app.domain.model

data class Review(
    val id: String,
    val pickupRequestId: String,
    val reviewerId: String,
    val reviewerName: String,
    val reviewedId: String,
    val reviewedName: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String
)
