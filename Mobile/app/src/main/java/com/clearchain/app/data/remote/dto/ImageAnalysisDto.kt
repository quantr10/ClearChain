package com.clearchain.app.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AnalyzeImageResponse(
    val success: Boolean,
    val message: String,
    val data: FoodAnalysisData?
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class FoodAnalysisData(
    val title: String,
    val category: String,
    val expiryDate: String,
    val notes: String,
    val imageUrl: String = "",
    val confidence: Double,
    val freshnessScore: Int,
    val qualityGrade: String,
    val detectedItems: List<DetectedItem>,
    val analyzedAt: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DetectedItem(
    val name: String,
    val confidence: Double
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UploadImageResponse(
    val success: Boolean,
    val message: String,
    val imageUrl: String?
)