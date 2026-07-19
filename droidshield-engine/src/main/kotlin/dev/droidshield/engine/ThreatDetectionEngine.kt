package dev.droidshield.engine

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.ThreatCheck
import dev.droidshield.domain.ThreatReporter
import dev.droidshield.domain.TelemetryEvent
import dev.droidshield.domain.TelemetrySink

/**
 * Runs a [Set] of [ThreatCheck]s and reports results. Knows nothing about
 * any specific check — only the domain contract. See ARCHITECTURE.md §3
 * and DECISIONS.md D022.
 */
class ThreatDetectionEngine(
    private val checks: Set<ThreatCheck>,
    private val reporter: ThreatReporter,
    private val telemetrySink: TelemetrySink,
    private val order: CheckOrder = CheckOrder.Unseeded,
) {
    fun runAll(context: CheckContext): List<CheckResult> {
        telemetrySink.capture(TelemetryEvent("engine_initialized", mapOf("checkCount" to checks.size)))

        val ordered = order.apply(checks)
        val results = mutableListOf<CheckResult>()

        for (check in ordered) {
            val started = System.nanoTime()
            val result = try {
                check.evaluate(context)
            } catch (t: Throwable) {
                telemetrySink.capture(
                    TelemetryEvent(
                        "check_error",
                        mapOf("checkId" to check.id, "errorType" to (t::class.simpleName ?: "Unknown")),
                    ),
                )
                continue
            }
            val durationMs = (System.nanoTime() - started) / 1_000_000

            telemetrySink.capture(
                TelemetryEvent(
                    "check_executed",
                    mapOf("checkId" to check.id, "durationMs" to durationMs, "category" to check.category.name),
                ),
            )

            results += result
            if (result.detected) {
                reporter.onThreatDetected(result)
            }
        }

        return results
    }
}
