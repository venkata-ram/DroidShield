package dev.droidshield.data.checks.root

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.io.File

/**
 * CHECKS_SEED_LIST.md ROOT #1. Weakest, most well-known check — one signal
 * among many, per the seed list's own framing.
 */
class SuBinaryPathCheck : ThreatCheck {
    override val id: String = "root.su_binary_path"
    override val category: ThreatCategory = ThreatCategory.ROOT
    override val severity: Severity = Severity.MEDIUM

    private val knownPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/system/su",
        "/system/bin/.ext/.su",
        "/system/usr/we-need-root/su",
        "/cache/su",
        "/data/su",
    )

    override fun evaluate(context: CheckContext): CheckResult {
        val match = knownPaths.firstOrNull { File(it).exists() }
        return CheckResult(id, category, severity, detected = match != null, detail = match)
    }
}
