package com.example.project.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

data class DangerousComboResult(
    val appName: String,
    val packageName: String,
    val comboName: String,
    val severity: String,
    val permissionsFound: List<String>
)

class PermissionAnalyzer(private val context: Context) {

    private val SURVEILLANCE_COMBO = listOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.READ_CONTACTS
    )

    private val DATA_HARVESTING_COMBO = listOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE, // Or modern equivalents
        android.Manifest.permission.READ_CONTACTS
    )

    private val FULL_MONITORING_COMBO = listOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA,
        // Using BACKGROUND_LOCATION as a proxy for "Background Monitoring" per the spec.
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    fun scanForDangerousCombos(): List<DangerousComboResult> {
        val pm = context.packageManager
        val results = mutableListOf<DangerousComboResult>()
        val flags = PackageManager.GET_PERMISSIONS
        val packages = pm.getInstalledPackages(flags)

        for (packageInfo in packages) {
            val permissions = packageInfo.requestedPermissions ?: continue
            val appFlags = packageInfo.applicationInfo?.flags ?: continue

            // Skip system apps for user safety (usually they have these permissions naturally)
            if ((appFlags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue
            }

            val appName = packageInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName

            if (hasAllPermissions(permissions, SURVEILLANCE_COMBO)) {
                results.add(DangerousComboResult(
                    appName, packageInfo.packageName, "Surveillance Combo \uD83D\uDEA8", "Critical", SURVEILLANCE_COMBO
                ))
            } else if (hasAllPermissions(permissions, DATA_HARVESTING_COMBO)) {
                results.add(DangerousComboResult(
                    appName, packageInfo.packageName, "Data Harvesting Combo \uD83D\uDEA8", "High", DATA_HARVESTING_COMBO
                ))
            } else if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && hasAllPermissions(permissions, FULL_MONITORING_COMBO)) {
                results.add(DangerousComboResult(
                    appName, packageInfo.packageName, "Full Monitoring Combo \uD83D\uDEA8", "Critical", FULL_MONITORING_COMBO
                ))
            }
        }
        return results
    }

    private fun hasAllPermissions(requested: Array<String>, required: List<String>): Boolean {
        // Simple string matching. In actual app, also check if they are GRANTED via pm.checkPermission.
        val requestedSet = requested.toSet()
        return required.all { requestedSet.contains(it) }
    }
}
