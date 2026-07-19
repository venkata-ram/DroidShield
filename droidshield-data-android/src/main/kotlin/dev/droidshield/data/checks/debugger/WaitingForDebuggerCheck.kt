package dev.droidshield.data.checks.debugger

import android.os.Debug
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/** CHECKS_SEED_LIST.md DEBUGGER #4 — the "Wait for Debugger" developer-options scenario. */
class WaitingForDebuggerCheck : ThreatCheck {
    override val id: String = "debugger.waiting_for_debugger"
    override val category: ThreatCategory = ThreatCategory.DEBUGGER
    override val severity: Severity = Severity.LOW

    override fun evaluate(context: CheckContext): CheckResult =
        CheckResult(id, category, severity, detected = Debug.waitingForDebugger())
}
