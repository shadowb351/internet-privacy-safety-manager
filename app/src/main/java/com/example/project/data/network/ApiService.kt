package com.example.project.data.network

import com.example.project.data.model.ScanRequest
import com.example.project.data.model.ScanResponse
import com.example.project.data.model.AnalyzeBehaviorRequest
import com.example.project.data.model.AnalyzeBehaviorResponse
import com.example.project.data.model.ExplainComboRequest
import com.example.project.data.model.ExplainComboResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("scan")
    suspend fun scanApps(@Body request: ScanRequest): Response<ScanResponse>

    @POST("analyze-behavior")
    suspend fun analyzeBehavior(@Body request: AnalyzeBehaviorRequest): Response<AnalyzeBehaviorResponse>

    @POST("explain-combo")
    suspend fun explainCombo(@Body request: ExplainComboRequest): Response<ExplainComboResponse>
}
