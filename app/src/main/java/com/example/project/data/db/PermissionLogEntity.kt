package com.example.project.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permission_logs")
data class PermissionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val appName: String,
    val packageName: String,
    val permissionType: String,
    val timestamp: Long,
    val accessType: String, // "FOREGROUND" or "BACKGROUND"
    val isSuspicious: Boolean
)
