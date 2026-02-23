package com.clearchain.app.data.repository

import com.clearchain.app.data.remote.api.ListingApi
import com.clearchain.app.data.remote.dto.CreateListingRequest
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.repository.ListingRepository
import com.clearchain.app.data.remote.dto.UpdateListingQuantityRequest
import javax.inject.Inject

class ListingRepositoryImpl @Inject constructor(
    private val listingApi: ListingApi
) : ListingRepository {

    override suspend fun createListing(
        title: String,
        description: String,
        category: String,
        quantity: Int,
        unit: String,
        expiryDate: String,
        pickupTimeStart: String,
        pickupTimeEnd: String,
        imageUrl: String?
    ): Result<Listing> {
        return try {
            val response = listingApi.createListing(
                CreateListingRequest(
                    title = title,
                    description = description,
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    expiryDate = expiryDate,
                    pickupTimeStart = pickupTimeStart,
                    pickupTimeEnd = pickupTimeEnd,
                    imageUrl = imageUrl
                )
            )
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyListings(): Result<List<Listing>> {
        return try {
            val response = listingApi.getMyListings()
            Result.success(response.data.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllListings(
        status: String?,
        category: String?
    ): Result<List<Listing>> {
        return try {
            val response = listingApi.getAllListings(status, category)
            Result.success(response.data.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getListingById(id: String): Result<Listing> {
        return try {
            val response = listingApi.getListingById(id)
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateListing(
        id: String,
        title: String,
        description: String,
        category: String,
        quantity: Int,
        unit: String,
        expiryDate: String,
        pickupTimeStart: String,
        pickupTimeEnd: String,
        imageUrl: String?
    ): Result<Listing> {
        return try {
            val response = listingApi.updateListing(
                id,
                CreateListingRequest(
                    title = title,
                    description = description,
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    expiryDate = expiryDate,
                    pickupTimeStart = pickupTimeStart,
                    pickupTimeEnd = pickupTimeEnd,
                    imageUrl = imageUrl
                )
            )
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteListing(id: String): Result<Unit> {
        return try {
            listingApi.deleteListing(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateListingQuantity(
        listingId: String, 
        newQuantity: Int
    ): Result<Listing> {
        return try {
            val response = listingApi.updateListingQuantity(
                listingId = listingId,
                request = UpdateListingQuantityRequest(newQuantity)
            )
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}