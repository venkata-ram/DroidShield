package dev.droidshield.data.checks.hook

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.io.File

/**
 * CHECKS_SEED_LIST.md HOOK #8 — Magisk module mount points associated with
 * LSPosed. Reads `/proc/self/maps` for module path fragments rather than
 * listing `/data/adb/modules` directly, since that directory is typically
 * not readable by a non-root app process.
 */
class LsposedZygiskCheck : ThreatCheck {
    override val id: String = "hook.lsposed_zygisk"
    override val category: ThreatCategory = ThreatCategory.HOOK
    override val severity: Severity = Severity.HIGH

    private val markers = listOf("lspd", "lsposed", "zygisk")

    override fun evaluate(context: CheckContext): CheckResult {
        val match = try {
            File("/proc/self/maps").readLines()
                .firstOrNull { line -> markers.any { line.contains(it, ignoreCase = true) } }
        } catch (e: Exception) {
            null
        }

        return CheckResult(id, category, severity, detected = match != null)
    }
}
