package com.example.project.data.sync

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.example.project.data.db.AppDatabase
import com.example.project.data.db.PermissionLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

object PermissionSyncManager {

    suspend fun syncPermissions(context: Context) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val database = AppDatabase.getDatabase(context)
            val dao = database.permissionLogDao()

            try {
                // Use reflection since getPackagesForOps is a hidden API
                val method = AppOpsManager::class.java.getMethod("getPackagesForOps", IntArray::class.java)
                val opCodes = intArrayOf(26 /* OP_CAMERA */, 27 /* OP_RECORD_AUDIO */, 1 /* OP_FINE_LOCATION */)
                
                val packagesForOps = method.invoke(appOpsManager, opCodes) as? List<*>
                if (packagesForOps != null) {
                    for (packageOps in packagesForOps) {
                        if (packageOps == null) continue
                        val getPackageNameMethod = packageOps.javaClass.getMethod("getPackageName")
                        val packageName = getPackageNameMethod.invoke(packageOps) as String
                        
                        val getOpsMethod = packageOps.javaClass.getMethod("getOps")
                        val ops = getOpsMethod.invoke(packageOps) as? List<*>
                        
                        if (ops != null) {
                            for (opEntry in ops) {
                                if (opEntry == null) continue
                                
                                val getOpMethod = opEntry.javaClass.getMethod("getOp")
                                val opCode = getOpMethod.invoke(opEntry) as Int
                                
                                val getTimeMethod = opEntry.javaClass.getMethod("getTime")
                                val accessTime = getTimeMethod.invoke(opEntry) as Long
                                
                                if (accessTime > 0) {
                                    val permissionName = when (opCode) {
                                        26 -> "Camera"
                                        27 -> "Microphone"
                                        1 -> "Location"
                                        else -> "Unknown"
                                    }
                                    
                                    val accessType = determineAccessType(packageName, usageStatsManager, accessTime)
                                    val isSuspicious = accessType == "BACKGROUND"

                                    val appName = getAppName(context, packageName)
                                    val lastSaved = dao.getLatestTimestamp(packageName, permissionName) ?: 0L

                                    Log.d("PrivacyLens", "Last $permissionName access: $accessTime")
                                    Log.d("PrivacyLens", "Last saved: $lastSaved")
                                    Log.d("PrivacyLens", "Access type: $accessType")

                                    if (accessTime > lastSaved) {
                                        val entity = PermissionLogEntity(
                                            appName = appName,
                                            packageName = packageName,
                                            permissionType = permissionName,
                                            timestamp = accessTime,
                                            accessType = accessType,
                                            isSuspicious = isSuspicious
                                        )
                                        dao.insertLog(entity)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun determineAccessType(packageName: String, usageStatsManager: UsageStatsManager, accessTimestamp: Long): String {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 * 24 // 24 hours ago
        
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = android.app.usage.UsageEvents.Event()
        
        var lastResumeTime = 0L
        var lastPauseTime = 0L
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName) {
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastResumeTime = event.timeStamp
                } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED ||
                           event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED) {
                    lastPauseTime = event.timeStamp
                    if (accessTimestamp in lastResumeTime..lastPauseTime) {
                        return "FOREGROUND"
                    }
                }
            }
        }
        
        // If the app is currently in foreground (resumed but not yet paused)
        if (lastResumeTime > lastPauseTime && accessTimestamp >= lastResumeTime) {
            return "FOREGROUND"
        }
        
        // Fallback: 10-second margin around resume/pause to account for slight timestamp differences
        if (Math.abs(lastResumeTime - accessTimestamp) < 10000 || Math.abs(lastPauseTime - accessTimestamp) < 10000) {
            return "FOREGROUND"
        }
        
        return "BACKGROUND"
    }
}
