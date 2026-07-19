package dev.droidshield.domain

/**
 * Security-signal callback. Kept separate from [dev.droidshield.domain.TelemetrySink]
 * on purpose — see DECISIONS.md D010.
 */
interface ThreatReporter {
    fun onThreatDetected(result: CheckResult)
}
