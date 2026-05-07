package com.example.project.data.model

data class AnalyzeBehaviorResponse(
    val suspicion_score: String, // Low / Medium / High
    val flagged_events: List<String>,
    val policy_contradiction: Boolean,
    val plain_english_summary: String
)
