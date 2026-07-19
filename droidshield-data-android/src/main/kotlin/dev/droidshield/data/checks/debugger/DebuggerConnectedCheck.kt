package dev.droidshield.data.checks.debugger

import android.os.Debug
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md DEBUGGER #1. Trivially bypassable alone (hooking
 * this single function defeats it) — must be combined with others.
 */
class DebuggerConnectedCheck : ThreatCheck {
    override val id: String = "debugger.debug_isDebuggerConnected"
    override val category: ThreatCategory = ThreatCategory.DEBUGGER
    override val severity: Severity = Severity.LOW

    override fun evaluate(context: CheckContext): CheckResult =
        CheckResult(id, category, severity, detected = Debug.isDebuggerConnected())
}
