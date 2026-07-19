package dev.droidshield.data.checks.root

import android.app.ActivityManager
import android.content.Context
import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md ROOT #3. Weak by construction on modern Android:
 * since Android 8 (API 26), [ActivityManager.getRunningAppProcesses] only
 * returns the calling app's own processes, not a system-wide list — so
 * this check can only ever see root/su processes that happen to share this
 * app's process, which is rare. Kept as a low-severity signal for older
 * API levels where the broader list was still visible, rather than
 * removed, per the seed list's "individually weak, combine many" framing.
 */
class RunningProcessRootCheck : ThreatCheck {
    override val id: String = "root.running_process_scan"
    override val category: ThreatCategory = ThreatCategory.ROOT
    override val severity: Severity = Severity.LOW

    private val suspiciousProcessNames = listOf("su", "magisk", "supersu", "kinguser")

    override fun evaluate(context: CheckContext): CheckResult {
        val androidContext = (context as AndroidCheckContext).androidContext
        val am = androidContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return CheckResult(id, category, severity, detected = false)

        val match = am.runningAppProcesses.orEmpty().firstOrNull { info ->
            suspiciousProcessNames.any { info.processName.contains(it, ignoreCase = true) }
        }

        return CheckResult(id, category, severity, detected = match != null, detail = match?.processName)
    }
}
