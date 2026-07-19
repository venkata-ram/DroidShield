package dev.droidshield.nativelayer.checks

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import dev.droidshield.nativelayer.NativeBridge

/** CHECKS_SEED_LIST.md HOOK #5 — native-layer, see DECISIONS.md D002. */
class NativeCodeChecksumCheck : ThreatCheck {
    override val id: String = "hook.native_code_checksum"
    override val category: ThreatCategory = ThreatCategory.HOOK
    override val severity: Severity = Severity.HIGH

    override fun evaluate(context: CheckContext): CheckResult =
        CheckResult(id, category, severity, detected = NativeBridge.nativeCodeChecksumMismatch())
}
