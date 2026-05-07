package com.example.project.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.project.data.sync.PermissionSyncManager

class PermissionSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        PermissionSyncManager.syncPermissions(context)
        return Result.success()
    }
}
