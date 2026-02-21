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

    fun scanApps(apps: List<AppInfo>) {
        viewModelScope.launch {
            try {
                Log.d("ScanViewModel", "Sending ${apps.size} apps to backend...")
                val request = ScanRequest(apps)
                val response = RetrofitClient.apiService.scanApps(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("ScanViewModel", "Response: $body")
                    _scanStatus.value = "Success: ${body?.status}, Count: ${body?.count}"
                } else {
                    Log.e("ScanViewModel", "Error: ${response.code()}")
                    _scanStatus.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("ScanViewModel", "Exception: ${e.message}")
                _scanStatus.value = "Failed: ${e.message}"
            }
        }
    }
}
