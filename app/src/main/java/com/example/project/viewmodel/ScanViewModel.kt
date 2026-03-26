package com.example.project.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.data.model.AppInfo
import com.example.project.data.model.ScanRequest
import com.example.project.data.network.RetrofitClient
import kotlinx.coroutines.launch

class ScanViewModel : ViewModel() {

    private val _scanStatus = MutableLiveData<String>()
    val scanStatus: LiveData<String> = _scanStatus

    private val _scanResults = MutableLiveData<List<com.example.project.data.model.AppScanResult>>()
    val scanResults: LiveData<List<com.example.project.data.model.AppScanResult>> = _scanResults

    fun scanApps(apps: List<AppInfo>) {
        viewModelScope.launch {
            try {
                Log.d("ScanViewModel", "Sending ${apps.size} apps to backend...")
                val request = ScanRequest(apps)
                val response = RetrofitClient.apiService.scanApps(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("ScanViewModel", "Response: $body")
                    
                    // Generate mock detailed results if backend doesn't provide them yet
                    val results = body?.results ?: apps.map { app ->
                        com.example.project.data.model.AppScanResult(
                            appName = app.name,
                            packageName = app.packageName,
                            dataCollected = listOf("Location", "Device ID"),
                            dataSharingEntities = listOf("Admob", "Meta Platforms, Inc."),
                            thirdPartySharing = true,
                            sensitiveDataDetected = listOf("Location"),
                            summary = "This app collects location data and shares it with third parties.",
                            riskScore = 65
                        )
                    }
                    
                    _scanStatus.value = "Success: ${body?.status}, Count: ${body?.count}"
                    _scanResults.value = results
                } else {
                    Log.e("ScanViewModel", "Error: ${response.code()}")
                    _scanStatus.value = "Error: ${response.code()}"
                    _scanResults.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("ScanViewModel", "Exception: ${e.message}")
                _scanStatus.value = "Failed: ${e.message}"
                _scanResults.value = emptyList()
            }
        }
    }
}
