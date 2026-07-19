package dev.droidshield.data.checks.debugger

import android.content.pm.ApplicationInfo
import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/** CHECKS_SEED_LIST.md DEBUGGER #2 — catches `android:debuggable="true"` repackaging. */
class DebuggableFlagCheck : ThreatCheck {
    override val id: String = "debugger.debuggable_flag"
    override val category: ThreatCategory = ThreatCategory.DEBUGGER
    override val severity: Severity = Severity.HIGH

    override fun evaluate(context: CheckContext): CheckResult {
        val androidContext = (context as AndroidCheckContext).androidContext
        val flags = androidContext.applicationInfo.flags
        val detected = (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return CheckResult(id, category, severity, detected)
    }
}
