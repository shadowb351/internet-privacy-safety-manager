package com.example.project.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.project.util.DangerousComboResult
import com.example.project.util.PermissionAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrivacyFixViewModel(application: Application) : AndroidViewModel(application) {

    private val _dangerousCombos = MutableLiveData<List<DangerousComboResult>>()
    val dangerousCombos: LiveData<List<DangerousComboResult>> get() = _dangerousCombos

    private val analyzer = PermissionAnalyzer(application)

    fun scanDeviceForCombos() {
        viewModelScope.launch(Dispatchers.IO) {
            val results = analyzer.scanForDangerousCombos()
            // In a real app we would sort by severity here
            _dangerousCombos.postValue(results)
        }
    }
}
