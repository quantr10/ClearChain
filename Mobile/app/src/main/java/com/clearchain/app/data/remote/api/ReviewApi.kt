package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.ReviewsResponse
import com.clearchain.app.data.remote.dto.SubmitReviewRequest
import com.clearchain.app.data.remote.dto.SubmitReviewResponse
import retrofit2.http.*

interface ReviewApi {

    @POST("reviews")
    suspend fun submitReview(@Body request: SubmitReviewRequest): SubmitReviewResponse

    @GET("reviews/organization/{id}")
    suspend fun getReviewsForOrganization(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): ReviewsResponse

    @GET("reviews/my")
    suspend fun getMyReviews(): ReviewsResponse

    // The reviews left on one pickup (at most one per side). Unlike the organization list this is
    // not paged, so an older pickup's review is still found.
    @GET("reviews/pickup/{id}")
    suspend fun getReviewsForPickup(@Path("id") id: String): ReviewsResponse
}
