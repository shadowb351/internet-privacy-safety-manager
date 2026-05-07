package com.example.project.data.model

data class ExplainComboResponse(
    val risk_explanation: String,
    val safe_to_revoke: List<String>,
    val severity: String // Low / Medium / High / Critical
)
