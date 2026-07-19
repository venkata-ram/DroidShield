package dev.droidshield.domain

data class CheckResult(
    val checkId: String,
    val category: ThreatCategory,
    val severity: Severity,
    val detected: Boolean,
    val detail: String? = null,
)
