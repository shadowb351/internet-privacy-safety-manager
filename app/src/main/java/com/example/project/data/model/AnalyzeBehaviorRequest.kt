package com.example.project.data.model

data class AnalyzeBehaviorRequest(
    val app_name: String,
    val permission_access_logs: List<PermissionLogItem>,
    val cached_privacy_policy_text: String
)

data class PermissionLogItem(
    val permissionType: String,
    val timestamp: Long,
    val wasBackground: Boolean
)
