package com.example.project.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PermissionLogDao {
    @Insert
    suspend fun insertLog(log: PermissionLogEntity)

    @Query("SELECT * FROM permission_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<PermissionLogEntity>

    @Query("SELECT * FROM permission_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getLogsSince(sinceTimestamp: Long): List<PermissionLogEntity>
    
    @Query("DELETE FROM permission_logs WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldLogs(beforeTimestamp: Long)
    
    @Query("DELETE FROM permission_logs")
    suspend fun deleteAllLogs()
    
    @Query("SELECT COUNT(*) > 0 FROM permission_logs WHERE packageName = :packageName AND timestamp = :timestamp")
    suspend fun hasLogForTimestamp(packageName: String, timestamp: Long): Boolean

    @Query("SELECT MAX(timestamp) FROM permission_logs WHERE packageName = :packageName AND permissionType = :permissionType")
    suspend fun getLatestTimestamp(packageName: String, permissionType: String): Long?
}
