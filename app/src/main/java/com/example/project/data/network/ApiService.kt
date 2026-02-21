package com.example.project.data.network

import com.example.project.data.model.ScanRequest
import com.example.project.data.model.ScanResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("scan")
    suspend fun scanApps(@Body request: ScanRequest): Response<ScanResponse>
}
