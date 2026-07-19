package dev.droidshield.data.checks.debugger

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md DEBUGGER #5. Measures wall-clock time across a small,
 * fixed amount of CPU-bound work; a large overrun suggests single-stepping
 * or a breakpoint pausing execution mid-check. [thresholdMillis] is
 * intentionally generous to keep false positives low on slow/loaded real
 * devices — this check is a coarse signal, not a precise one, per the seed
 * list's framing of every technique here as individually weak.
 */
class TimingAnomalyCheck(
    private val thresholdMillis: Long = 200L,
) : ThreatCheck {
    override val id: String = "debugger.timing_anomaly"
    override val category: ThreatCategory = ThreatCategory.DEBUGGER
    override val severity: Severity = Severity.LOW

    override fun evaluate(context: CheckContext): CheckResult {
        val started = System.nanoTime()

        var acc = 0L
        for (i in 0 until 1_000_000) {
            acc += i
        }

        val elapsedMillis = (System.nanoTime() - started) / 1_000_000
        val detected = elapsedMillis > thresholdMillis

        return CheckResult(id, category, severity, detected, detail = "${elapsedMillis}ms (acc=$acc)")
    }
}
