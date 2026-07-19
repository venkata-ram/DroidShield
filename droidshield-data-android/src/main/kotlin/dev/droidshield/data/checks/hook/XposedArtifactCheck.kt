package dev.droidshield.data.checks.hook

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/** CHECKS_SEED_LIST.md HOOK #7 — Xposed-specific class present on the classpath. */
class XposedArtifactCheck : ThreatCheck {
    override val id: String = "hook.xposed_artifact"
    override val category: ThreatCategory = ThreatCategory.HOOK
    override val severity: Severity = Severity.HIGH

    override fun evaluate(context: CheckContext): CheckResult {
        val detected = try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        return CheckResult(id, category, severity, detected)
    }
}
