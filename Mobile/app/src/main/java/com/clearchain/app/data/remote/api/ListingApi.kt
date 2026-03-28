package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.CreateListingRequest
import com.clearchain.app.data.remote.dto.ListingResponse
import com.clearchain.app.data.remote.dto.ListingsResponse
import com.clearchain.app.data.remote.dto.UpdateListingQuantityRequest
import retrofit2.http.*

interface ListingApi {

    @POST("listings")
    suspend fun createListing(
        @Body request: CreateListingRequest
    ): ListingResponse

    @GET("listings/grocery/my")
    suspend fun getMyListings(): ListingsResponse

    @GET("listings/{id}")
    suspend fun getListingById(
        @Path("id") id: String
    ): ListingResponse

    // ═══ UPDATED: Added lat/lng/radiusKm query params (Part 2) ═══
    @GET("listings")
    suspend fun getAllListings(
        @Query("status") status: String? = null,
        @Query("category") category: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radiusKm") radiusKm: Int? = null
    ): ListingsResponse

    @PUT("listings/{id}")
    suspend fun updateListing(
        @Path("id") id: String,
        @Body request: CreateListingRequest
    ): ListingResponse

    @DELETE("listings/{id}")
    suspend fun deleteListing(
        @Path("id") id: String
    ): ListingResponse

    @PUT("listings/{id}/quantity")
    suspend fun updateListingQuantity(
        @Path("id") listingId: String,
        @Body request: UpdateListingQuantityRequest
    ): ListingResponse
}