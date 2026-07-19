package dev.droidshield.data.checks.tamper

import android.content.pm.InstallSourceInfo
import android.os.Build
import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md TAMPER #2. Documented as weak alone — sideloading is
 * common and legitimate in many markets — so this is `LOW` severity by
 * default and treated as one signal, never a standalone verdict.
 */
class InstallerSourceCheck(
    private val expectedInstallerPackage: String = "com.android.vending",
) : ThreatCheck {
    override val id: String = "tamper.installer_source"
    override val category: ThreatCategory = ThreatCategory.TAMPER
    override val severity: Severity = Severity.LOW

    override fun evaluate(context: CheckContext): CheckResult {
        val androidContext = (context as AndroidCheckContext).androidContext
        val pm = androidContext.packageManager

        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info: InstallSourceInfo = pm.getInstallSourceInfo(androidContext.packageName)
                info.installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(androidContext.packageName)
            }
        } catch (e: Exception) {
            null
        }

        return CheckResult(
            id,
            category,
            severity,
            detected = installer != expectedInstallerPackage,
            detail = installer,
        )
    }
}
