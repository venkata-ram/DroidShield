package dev.droidshield.data.checks.tamper

import android.content.pm.ApplicationInfo
import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md TAMPER #5. Verifies the running app's manifest flags
 * match what the integrator says the build actually declared
 * ([expectedDebuggable], [expectedAllowBackup]) — catches manifest-level
 * repackaging (e.g. flipping `allowBackup` to exfiltrate app data via
 * `adb backup`).
 */
class ManifestTamperCheck(
    private val expectedDebuggable: Boolean = false,
    // `true` matches the platform default for a manifest that omits
    // android:allowBackup — see DroidShieldConfig.expectedManifestAllowBackup.
    private val expectedAllowBackup: Boolean = true,
) : ThreatCheck {
    override val id: String = "tamper.manifest_flags"
    override val category: ThreatCategory = ThreatCategory.TAMPER
    override val severity: Severity = Severity.MEDIUM

    override fun evaluate(context: CheckContext): CheckResult {
        val applicationInfo = (context as AndroidCheckContext).androidContext.applicationInfo
        val actualDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val actualAllowBackup = (applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0

        val detected = actualDebuggable != expectedDebuggable || actualAllowBackup != expectedAllowBackup

        return CheckResult(
            id,
            category,
            severity,
            detected,
            detail = "debuggable=$actualDebuggable allowBackup=$actualAllowBackup",
        )
    }
}
