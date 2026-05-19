package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.SubmitReportRequest
import retrofit2.http.*

interface ReportApi {

    @POST("reports")
    suspend fun submitReport(@Body request: SubmitReportRequest): Any
}
