package com.example.project.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.db.AppDatabase
import com.example.project.ui.adapter.PermissionReportAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View
import android.graphics.Color
import com.example.project.data.db.PermissionLogEntity
import com.example.project.data.sync.PermissionSyncManager

class PermissionReportActivity : AppCompatActivity() {

    private lateinit var adapter: PermissionReportAdapter
    private lateinit var tvTotalEventsCount: TextView
    private lateinit var tvBackgroundAlertsCount: TextView
    private lateinit var rvPermissionEvents: RecyclerView
    private var allLogs: List<PermissionLogEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_report)
        
        tvTotalEventsCount = findViewById(R.id.tvTotalEventsCount)
        tvBackgroundAlertsCount = findViewById(R.id.tvBackgroundAlertsCount)
        rvPermissionEvents = findViewById(R.id.rvPermissionEvents)

        adapter = PermissionReportAdapter(emptyList()) { packageName ->
            openAppSettings(packageName)
        }
        rvPermissionEvents.adapter = adapter

        val btnClearReport = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClearReport)
        btnClearReport.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                db.permissionLogDao().deleteAllLogs()
                loadLogs()
            }
        }

        syncAndLoadLogs()
    }
    
    override fun onResume() {
        super.onResume()
        syncAndLoadLogs()
    }
    
    private fun syncAndLoadLogs() {
        lifecycleScope.launch(Dispatchers.IO) {
            PermissionSyncManager.syncPermissions(applicationContext)
            loadLogs()
        }
    }

    private fun loadLogs() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            allLogs = db.permissionLogDao().getAllLogs()
            
            withContext(Dispatchers.Main) {
                updateStats()
                filterAndDisplayLogs()
            }
        }
    }
    
    private fun updateStats() {
        tvTotalEventsCount.text = allLogs.size.toString()
        val bgCount = allLogs.count { it.accessType == "BACKGROUND" }
        tvBackgroundAlertsCount.text = bgCount.toString()
    }
    
    private fun filterAndDisplayLogs() {
        val filtered = allLogs.filter { it.accessType == "BACKGROUND" }
        adapter.updateData(filtered)
    }


    private fun openAppSettings(packageName: String) {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
