package com.example.project.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.ui.adapter.PrivacyFixAdapter
import com.example.project.viewmodel.PrivacyFixViewModel
import com.google.android.material.button.MaterialButton

class PrivacyFixActivity : AppCompatActivity() {

    private lateinit var viewModel: PrivacyFixViewModel
    private lateinit var adapter: PrivacyFixAdapter

    private lateinit var llScanState: LinearLayout
    private lateinit var btnRunScan: MaterialButton
    private lateinit var rvPrivacyIssues: RecyclerView
    private lateinit var tvNoIssues: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_fix)

        viewModel = ViewModelProvider(this).get(PrivacyFixViewModel::class.java)

        llScanState = findViewById(R.id.llScanState)
        btnRunScan = findViewById(R.id.btnRunScan)
        rvPrivacyIssues = findViewById(R.id.rvPrivacyIssues)
        tvNoIssues = findViewById(R.id.tvNoIssues)

        adapter = PrivacyFixAdapter(emptyList()) { packageName ->
            openAppSettings(packageName)
        }
        rvPrivacyIssues.adapter = adapter

        btnRunScan.setOnClickListener {
            // Give UI feedback. In a real app we'd show a loading indicator here.
            btnRunScan.text = "Scanning..."
            btnRunScan.isEnabled = false
            viewModel.scanDeviceForCombos()
        }

        viewModel.dangerousCombos.observe(this) { combos ->
            llScanState.visibility = View.GONE
            
            if (combos.isEmpty()) {
                tvNoIssues.visibility = View.VISIBLE
                rvPrivacyIssues.visibility = View.GONE
            } else {
                tvNoIssues.visibility = View.GONE
                rvPrivacyIssues.visibility = View.VISIBLE
                adapter.updateData(combos)
            }
        }
    }

    private fun openAppSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
