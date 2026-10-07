package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.ReviewsResponse
import com.clearchain.app.data.remote.dto.SubmitReviewRequest
import com.clearchain.app.data.remote.dto.SubmitReviewResponse
import retrofit2.http.*

interface ReviewApi {

    @POST("reviews")
    suspend fun submitReview(@Body request: SubmitReviewRequest): SubmitReviewResponse

    // The reviews left on one pickup (at most one per side).
    @GET("reviews/pickup/{id}")
    suspend fun getReviewsForPickup(@Path("id") id: String): ReviewsResponse
}
