package dev.droidshield.domain

/**
 * Operational-health callback. Kept separate from [ThreatReporter] on
 * purpose — see DECISIONS.md D010.
 */
interface TelemetrySink {
    fun capture(event: TelemetryEvent)
}
