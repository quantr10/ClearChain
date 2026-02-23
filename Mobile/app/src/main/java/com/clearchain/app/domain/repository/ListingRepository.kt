package com.clearchain.app.domain.repository

import com.clearchain.app.domain.model.Listing
import kotlinx.coroutines.flow.Flow

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

    suspend fun getMyListings(): Result<List<Listing>>

    suspend fun getAllListings(
        status: String? = null,
        category: String? = null
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
}