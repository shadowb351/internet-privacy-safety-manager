package com.example.project.data.model

data class ScanResponse(
    val status: String,
    val count: Int,
    val results: List<AppScanResult>? = null
)

data class AppScanResult(
    val appName: String,
    val packageName: String,
    val dataCollected: List<String>,
    val dataSharingEntities: List<String> = emptyList(),
    val thirdPartySharing: Boolean,
    val sensitiveDataDetected: List<String>,
    val summary: String,
    val riskScore: Int,
    val riskLevel: String? = null,
    val detectedDomain: String? = null,
    val unnecessaryDataCollected: List<String> = emptyList(),
    val riskReasons: List<String> = emptyList()
)
