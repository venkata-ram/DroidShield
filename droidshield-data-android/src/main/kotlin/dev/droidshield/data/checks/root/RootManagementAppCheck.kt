package dev.droidshield.data.checks.root

import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md ROOT #2. Android 11+ package-visibility restrictions
 * can cause false negatives unless the host app declares a `<queries>`
 * entry for these packages — documented here rather than worked around,
 * since DroidShield can't inject manifest `<queries>` into the host app
 * from this module.
 */
class RootManagementAppCheck : ThreatCheck {
    override val id: String = "root.root_management_app"
    override val category: ThreatCategory = ThreatCategory.ROOT
    override val severity: Severity = Severity.HIGH

    private val knownPackages = listOf(
        "com.topjohnwu.magisk",
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot",
    )

    override fun evaluate(context: CheckContext): CheckResult {
        val androidContext = (context as AndroidCheckContext).androidContext
        val pm = androidContext.packageManager

        val found = knownPackages.firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                false
            }
        }

        return CheckResult(id, category, severity, detected = found != null, detail = found)
    }
}
