package dev.droidshield.data.checks.hook

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.io.File

/** CHECKS_SEED_LIST.md HOOK #4 — loaded native libraries flagged by name. */
class SuspiciousLibraryCheck : ThreatCheck {
    override val id: String = "hook.suspicious_loaded_library"
    override val category: ThreatCategory = ThreatCategory.HOOK
    override val severity: Severity = Severity.HIGH

    private val suspiciousLibraryNames = listOf(
        "frida-agent.so",
        "frida-gadget.so",
        "libfrida",
        "libsubstrate",
        "libxposed",
    )

    override fun evaluate(context: CheckContext): CheckResult {
        val match = try {
            File("/proc/self/maps").readLines()
                .mapNotNull { line -> line.substringAfterLast('/', missingDelimiterValue = "").ifBlank { null } }
                .firstOrNull { fileName -> suspiciousLibraryNames.any { fileName.contains(it, ignoreCase = true) } }
        } catch (e: Exception) {
            null
        }

        return CheckResult(id, category, severity, detected = match != null, detail = match)
    }
}
