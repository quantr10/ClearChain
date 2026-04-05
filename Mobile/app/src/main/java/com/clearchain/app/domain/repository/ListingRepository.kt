package com.clearchain.app.domain.repository

import android.net.Uri
import com.clearchain.app.data.remote.dto.FoodAnalysisData
import com.clearchain.app.domain.model.Listing

interface ListingRepository {

    suspend fun createListing(
        title: String,
        description: String,
        category: String,
        quantity: Int,
        unit: String,
        expiryDate: String,
        pickupTimeStart: String,
        pickupTimeEnd: String,
        imageUrl: String? = null
    ): Result<Listing>

    suspend fun getMyListings(page: Int = 1, pageSize: Int = 20): Result<List<Listing>>

    // ═══ UPDATED: Added location params (Part 2) ═══
    suspend fun getAllListings(
        status: String? = null,
        category: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        radiusKm: Int? = null,
        page: Int = 1,
        pageSize: Int = 50
    ): Result<List<Listing>>

    suspend fun getListingById(id: String): Result<Listing>

    suspend fun updateListing(
        id: String,
        title: String,
        description: String,
        category: String,
        quantity: Int,
        unit: String,
        expiryDate: String,
        pickupTimeStart: String,
        pickupTimeEnd: String,
        imageUrl: String? = null
    ): Result<Listing>

    suspend fun deleteListing(id: String): Result<Unit>

    suspend fun updateListingQuantity(listingId: String, newQuantity: Int): Result<Listing>

    suspend fun analyzeImage(imageUri: Uri): Result<FoodAnalysisData>

    suspend fun saveAnalysis(analysisData: FoodAnalysisData): Result<Unit>

    suspend fun uploadFoodImage(imageUri: Uri): Result<String>
}