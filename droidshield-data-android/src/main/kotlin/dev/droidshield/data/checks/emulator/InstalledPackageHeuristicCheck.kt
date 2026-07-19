package dev.droidshield.data.checks.emulator

import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/** CHECKS_SEED_LIST.md EMULATOR #10. */
class InstalledPackageHeuristicCheck : ThreatCheck {
    override val id: String = "emulator.installed_package_heuristic"
    override val category: ThreatCategory = ThreatCategory.EMULATOR
    override val severity: Severity = Severity.LOW

    private val genymotionPackages = listOf(
        "com.genymotion.genyd",
        "com.genymobile.gplay",
    )

    override fun evaluate(context: CheckContext): CheckResult {
        val pm = (context as AndroidCheckContext).androidContext.packageManager

        val genymotionFound = genymotionPackages.any { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                false
            }
        }

        val playStoreMissing = try {
            pm.getPackageInfo("com.android.vending", 0)
            false
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            true
        }

        return CheckResult(
            id,
            category,
            severity,
            detected = genymotionFound || playStoreMissing,
            detail = "genymotion=$genymotionFound playStoreMissing=$playStoreMissing",
        )
    }
}
