package com.example.project.data.model

data class ExplainComboRequest(
    val app_name: String,
    val permissions_held: List<String>,
    val combo_type: String
)
