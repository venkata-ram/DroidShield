package dev.droidshield.data.checks.tamper

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md TAMPER #10, simplified for V1. The seed list's
 * original idea — the engine/registry checksumming its own bytecode
 * presence/integrity — needs the same DEX-hashing mechanism as
 * [DexIntegrityCheck], scoped to exactly the class files that back the
 * engine, which requires build-time tooling to enumerate reliably (not
 * yet implemented — see DECISIONS.md D023 follow-ups).
 *
 * This V1 version is a weaker proxy: it verifies that
 * [dev.droidshield.engine.ThreatDetectionEngine] itself is still loadable
 * via reflection. It cannot detect the engine's bytecode being *modified*
 * (only being *removed/renamed*, which a reflection lookup can see) — that
 * gap is intentional and documented rather than silently claimed as full
 * coverage.
 */
class EngineSelfPresenceCheck : ThreatCheck {
    override val id: String = "tamper.engine_self_presence"
    override val category: ThreatCategory = ThreatCategory.TAMPER
    override val severity: Severity = Severity.HIGH

    override fun evaluate(context: CheckContext): CheckResult {
        val missing = try {
            Class.forName("dev.droidshield.engine.ThreatDetectionEngine")
            false
        } catch (e: ClassNotFoundException) {
            true
        }

        return CheckResult(id, category, severity, detected = missing)
    }
}
