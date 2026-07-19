package dev.droidshield.data.checks.hook

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.io.File

/**
 * CHECKS_SEED_LIST.md HOOK #1. Widely-known to be bypassable via `strstr`
 * hooking (per the seed list) — combine with others, never rely on alone.
 */
class ProcMapsSignatureCheck : ThreatCheck {
    override val id: String = "hook.proc_maps_signature"
    override val category: ThreatCategory = ThreatCategory.HOOK
    override val severity: Severity = Severity.MEDIUM

    private val signatures = listOf("frida", "xposed", "substrate")

    override fun evaluate(context: CheckContext): CheckResult {
        val match = try {
            File("/proc/self/maps").readLines()
                .firstOrNull { line -> signatures.any { line.contains(it, ignoreCase = true) } }
        } catch (e: Exception) {
            null
        }

        return CheckResult(id, category, severity, detected = match != null)
    }
}
