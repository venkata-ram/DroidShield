package dev.droidshield.domain

/**
 * Operational/usage data, not a security signal. Keep payloads minimal and
 * free of PII by default — see ARCHITECTURE.md §4a.
 */
data class TelemetryEvent(
    val name: String,
    val properties: Map<String, Any?> = emptyMap(),
)
