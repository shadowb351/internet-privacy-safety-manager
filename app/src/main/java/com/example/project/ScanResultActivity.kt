package com.example.project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.data.model.AppScanResult
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScanResultActivity : AppCompatActivity() {

    private lateinit var rvScanResults: RecyclerView
    private lateinit var btnDone: ExtendedFloatingActionButton
    
    // UI Elements
    private lateinit var contentScrollView: ScrollView
    private lateinit var errorOverlay: LinearLayout
    private lateinit var tvErrorMessage: TextView
    
    // Risk Counters
    private lateinit var tvLowRiskCount: TextView
    private lateinit var tvMediumRiskCount: TextView
    private lateinit var tvHighRiskCount: TextView
    private lateinit var tvGlobalRiskLevel: TextView
    
    // Chip Groups
    private lateinit var cgDataTypes: ChipGroup
    private lateinit var cgSharingEntities: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scan_result)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindViews()

        btnDone.setOnClickListener {
            finish()
        }

        rvScanResults.layoutManager = LinearLayoutManager(this)

        val errorMessage = intent.getStringExtra("error_message")
        if (errorMessage != null) {
            showErrorState(errorMessage)
            return
        }

        val jsonResults = intent.getStringExtra("scan_results")
        if (jsonResults != null) {
            val type = object : TypeToken<List<AppScanResult>>() {}.type
            val results: List<AppScanResult> = Gson().fromJson(jsonResults, type)
            setupData(results)
        } else {
            showErrorState("No data received.")
        }
    }

    private fun bindViews() {
        rvScanResults = findViewById(R.id.rvScanResults)
        btnDone = findViewById(R.id.btnDone)
        contentScrollView = findViewById(R.id.contentScrollView)
        errorOverlay = findViewById(R.id.errorOverlay)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        
        tvLowRiskCount = findViewById(R.id.tvLowRiskCount)
        tvMediumRiskCount = findViewById(R.id.tvMediumRiskCount)
        tvHighRiskCount = findViewById(R.id.tvHighRiskCount)
        tvGlobalRiskLevel = findViewById(R.id.tvGlobalRiskLevel)
        
        cgDataTypes = findViewById(R.id.cgDataTypes)
        cgSharingEntities = findViewById(R.id.cgSharingEntities)
    }

    private fun showErrorState(message: String) {
        contentScrollView.visibility = View.GONE
        errorOverlay.visibility = View.VISIBLE
        tvErrorMessage.text = message
    }

    private fun setupData(results: List<AppScanResult>) {
        contentScrollView.visibility = View.VISIBLE
        errorOverlay.visibility = View.GONE
        
        // 1. Setup adapter for individual apps
        val adapter = ScanResultAdapter(results) { packageName ->
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
        rvScanResults.adapter = adapter

        // 2. Calculate risk counts
        var low = 0
        var medium = 0
        var high = 0
        var totalRisk = 0
        
        results.forEach { app ->
            when {
                app.riskScore < 30 -> low++
                app.riskScore < 70 -> medium++
                else -> high++
            }
            totalRisk += app.riskScore
        }
        
        tvLowRiskCount.text = low.toString()
        tvMediumRiskCount.text = medium.toString()
        tvHighRiskCount.text = high.toString()
        
        if (results.isNotEmpty()) {
            val avgRisk = totalRisk / results.size
            when {
                avgRisk < 30 -> {
                    tvGlobalRiskLevel.text = "Low Risk"
                    tvGlobalRiskLevel.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                }
                avgRisk < 70 -> {
                    tvGlobalRiskLevel.text = "Medium Risk"
                    tvGlobalRiskLevel.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                }
                else -> {
                    tvGlobalRiskLevel.text = "High Risk"
                    tvGlobalRiskLevel.setTextColor(android.graphics.Color.parseColor("#F44336"))
                }
            }
        }

        // 3. Aggregate Data Types and Entities
        val allDataTypes = results.flatMap { it.dataCollected }.distinct()
        val allEntities = results.flatMap { it.dataSharingEntities ?: emptyList() }.distinct()

        populateChips(cgDataTypes, allDataTypes, "#4CAF50") // Green dots for data
        populateChips(cgSharingEntities, allEntities, "#F44336") // Red dots for entities
    }

    private fun populateChips(chipGroup: ChipGroup, items: List<String>, dotColor: String) {
        chipGroup.removeAllViews()
        if (items.isEmpty()) {
            val tv = TextView(this)
            tv.text = "None detected."
            tv.setTextColor(android.graphics.Color.GRAY)
            chipGroup.addView(tv)
            return
        }
        
        items.forEach { item ->
            val chip = Chip(this)
            chip.text = item
            chip.setTextColor(android.graphics.Color.WHITE)
            chip.setChipBackgroundColorResource(android.R.color.transparent)
            chip.chipStrokeWidth = 2f
            chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#333333"))
            
            // Set circle icon using a built-in drawable (e.g., presence_online is a small green dot, but we can tint it)
            chip.isChipIconVisible = true
            chip.setChipIconResource(android.R.drawable.presence_online)
            chip.chipIconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(dotColor))
            
            chip.isClickable = false
            chip.isCheckable = false
            chipGroup.addView(chip)
        }
    }
}

class ScanResultAdapter(
    private val results: List<AppScanResult>,
    private val onRemoveClicked: (String) -> Unit
) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val tvDataTypesCount: TextView = view.findViewById(R.id.tvDataTypesCount)
        val tvRiskScore: TextView = view.findViewById(R.id.tvRiskScore)
        val btnRemovePermissions: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnRemovePermissions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = results[position]
        
        holder.tvAppName.text = item.appName
        val count = item.dataCollected.size
        holder.tvDataTypesCount.text = "$count data types"
        
        val riskColor: String
        val riskText: String
        when {
            item.riskScore < 30 -> {
                riskColor = "#4CAF50"
                riskText = "Low"
            }
            item.riskScore < 70 -> {
                riskColor = "#FFC107"
                riskText = "Medium"
            }
            else -> {
                riskColor = "#F44336"
                riskText = "High"
            }
        }
        
        holder.tvRiskScore.text = riskText
        holder.tvRiskScore.setTextColor(android.graphics.Color.parseColor(riskColor))
        
        val drawable = holder.tvRiskScore.background
        drawable.setTint(android.graphics.Color.parseColor(riskColor.replace("#", "#33")))
        
        holder.btnRemovePermissions.setOnClickListener {
            onRemoveClicked(item.packageName)
        }
    }

    override fun getItemCount() = results.size
}
