package dev.droidshield.engine

import com.google.common.truth.Truth.assertThat
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import dev.droidshield.domain.ThreatReporter
import dev.droidshield.domain.TelemetryEvent
import dev.droidshield.domain.TelemetrySink
import org.junit.Test

private object FakeContext : CheckContext

private class FixedCheck(
    override val id: String,
    override val category: ThreatCategory,
    private val detected: Boolean,
    private val throwOnEvaluate: Boolean = false,
) : ThreatCheck {
    override val severity: Severity = Severity.HIGH
    override fun evaluate(context: CheckContext): CheckResult {
        if (throwOnEvaluate) error("boom")
        return CheckResult(id, category, severity, detected)
    }
}

private class RecordingReporter : ThreatReporter {
    val reported = mutableListOf<CheckResult>()
    override fun onThreatDetected(result: CheckResult) {
        reported += result
    }
}

private class RecordingTelemetrySink : TelemetrySink {
    val events = mutableListOf<TelemetryEvent>()
    override fun capture(event: TelemetryEvent) {
        events += event
    }
}

class ThreatDetectionEngineTest {

    @Test
    fun `only detected results are reported`() {
        val reporter = RecordingReporter()
        val engine = ThreatDetectionEngine(
            checks = setOf(
                FixedCheck("clean", ThreatCategory.ROOT, detected = false),
                FixedCheck("dirty", ThreatCategory.DEBUGGER, detected = true),
            ),
            reporter = reporter,
            telemetrySink = RecordingTelemetrySink(),
        )

        val results = engine.runAll(FakeContext)

        assertThat(results).hasSize(2)
        assertThat(reporter.reported.map { it.checkId }).containsExactly("dirty")
    }

    @Test
    fun `a check that throws is skipped, not fatal to the run`() {
        val reporter = RecordingReporter()
        val telemetry = RecordingTelemetrySink()
        val engine = ThreatDetectionEngine(
            checks = setOf(
                FixedCheck("boom", ThreatCategory.ROOT, detected = false, throwOnEvaluate = true),
                FixedCheck("survives", ThreatCategory.ROOT, detected = true),
            ),
            reporter = reporter,
            telemetrySink = telemetry,
        )

        val results = engine.runAll(FakeContext)

        assertThat(results.map { it.checkId }).containsExactly("survives")
        assertThat(telemetry.events.any { it.name == "check_error" }).isTrue()
    }

    @Test
    fun `seeded order is deterministic for the same seed`() {
        val checks = (1..10).map {
            FixedCheck("check-$it", ThreatCategory.ROOT, detected = false)
        }.toSet()

        val orderA = CheckOrder.Seeded(seed = 42L).apply(checks).map { it.id }
        val orderB = CheckOrder.Seeded(seed = 42L).apply(checks).map { it.id }
        val orderC = CheckOrder.Seeded(seed = 99L).apply(checks).map { it.id }

        assertThat(orderA).isEqualTo(orderB)
        assertThat(orderA).isNotEqualTo(orderC)
        assertThat(orderA.toSet()).isEqualTo(checks.map { it.id }.toSet())
    }

    @Test
    fun `seeded subset always keeps at least one check per category`() {
        val checks = setOf(
            FixedCheck("root-1", ThreatCategory.ROOT, detected = false),
            FixedCheck("root-2", ThreatCategory.ROOT, detected = false),
            FixedCheck("debugger-1", ThreatCategory.DEBUGGER, detected = false),
        )

        val subset = CheckOrder.Seeded(seed = 7L, subsetFraction = 0.34).apply(checks)

        assertThat(subset.map { it.category }.toSet()).containsExactly(ThreatCategory.ROOT, ThreatCategory.DEBUGGER)
    }
}
