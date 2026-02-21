package com.example.project

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.viewModels
import com.example.project.databinding.ActivityMainBinding
import com.example.project.viewmodel.ScanViewModel
import com.example.project.data.model.AppInfo

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppAdapter
    private var allApps: List<AppModel> = emptyList()
    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        loadInstalledApps()
        setupListeners()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(emptyList()) { selectedCount ->
            updateScanButton(selectedCount)
        }
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter
    }

    private fun loadInstalledApps() {
        val packageManager = packageManager
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)

        allApps = packages
            .filter { it.applicationInfo != null && !isSystemApp(it.applicationInfo!!) }
            .map {
                val appInfo = it.applicationInfo!!
                AppModel(
                    name = appInfo.loadLabel(packageManager).toString(),
                    packageName = it.packageName,
                    icon = appInfo.loadIcon(packageManager)
                )
            }
            .sortedBy { it.name.lowercase() }

        adapter.updateData(allApps)
        updateScanButton(0)
    }

    private fun isSystemApp(applicationInfo: ApplicationInfo): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    private fun setupListeners() {
        // Search
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString()
            filterApps(query)
        }

        // Select All
        binding.btnSelectAll.setOnClickListener {
            val areAllSelected = adapter.getSelectedCount() == adapter.itemCount && adapter.itemCount > 0
            adapter.selectAll(!areAllSelected)
        }

        // Scan Button
        binding.btnScan.setOnClickListener {
            val selectedApps = adapter.getSelectedApps()
            if (selectedApps.isNotEmpty()) {
                val appInfos = selectedApps.map { AppInfo(it.name, it.packageName) }
                viewModel.scanApps(appInfos)
            } else {
                Toast.makeText(this, "Please select apps to scan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.scanStatus.observe(this) { status ->
            Toast.makeText(this, status, Toast.LENGTH_LONG).show()
        }
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        adapter.updateData(filtered)
    }

    private fun updateScanButton(count: Int) {
        binding.btnScan.text = if (count > 0) "Scan $count Selected Apps" else "Scan Selected Apps"
        binding.btnScan.isEnabled = count > 0 // Optional: disable if 0
    }
}