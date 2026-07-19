package dev.droidshield.nativelayer.checks

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import dev.droidshield.nativelayer.NativeBridge

/** CHECKS_SEED_LIST.md DEBUGGER #6 — native-layer, see DECISIONS.md D002. */
class PtraceSelfAttachCheck : ThreatCheck {
    override val id: String = "debugger.ptrace_self_attach"
    override val category: ThreatCategory = ThreatCategory.DEBUGGER
    override val severity: Severity = Severity.HIGH

    override fun evaluate(context: CheckContext): CheckResult =
        CheckResult(id, category, severity, detected = NativeBridge.ptraceSelfAttachDetectsTracer())
}
