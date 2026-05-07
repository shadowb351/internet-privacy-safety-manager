package com.example.project.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.project.data.db.AppDatabase
import com.example.project.data.model.AnalyzeBehaviorRequest
import com.example.project.data.model.PermissionLogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeeklyReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val logs = db.permissionLogDao().getAllLogs()
            
            if (logs.isEmpty()) {
                return@withContext Result.success()
            }

            // In a real scenario, we group by appName and send to /analyze-behavior per app
            // Here we just simulate logging it or hitting the API with a single app's logs as an example.
            val groupedLogs = logs.groupBy { it.appName }

            // val retrofit = RetrofitClient.getInstance().create(ApiService::class.java)

            // for ((appName, appLogs) in groupedLogs) {
            //     val requestLogs = appLogs.map { 
            //         PermissionLogItem(it.permissionType, it.timestamp, it.wasBackground) 
            //     }
            //     val request = AnalyzeBehaviorRequest(
            //          app_name = appName, 
            //          permission_access_logs = requestLogs, 
            //          cached_privacy_policy_text = "MOCK POLICY"
            //     )
            //     val response = retrofit.analyzeBehavior(request)
            //     // Handle response: save suspicion score, notify user, etc.
            // }

            // Clear old logs
            val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
            db.permissionLogDao().deleteOldLogs(oneWeekAgo)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
