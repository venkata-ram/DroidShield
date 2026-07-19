package dev.droidshield.data.checks.emulator

import android.os.Build
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/** CHECKS_SEED_LIST.md EMULATOR #1. */
class BuildFingerprintCheck : ThreatCheck {
    override val id: String = "emulator.build_fingerprint"
    override val category: ThreatCategory = ThreatCategory.EMULATOR
    override val severity: Severity = Severity.MEDIUM

    private val markers = listOf("generic", "sdk", "genymotion", "android sdk built for", "google_sdk")

    override fun evaluate(context: CheckContext): CheckResult {
        val candidates = listOf(Build.FINGERPRINT, Build.MODEL, Build.MANUFACTURER, Build.HARDWARE)
            .filterNotNull()

        val match = candidates.firstOrNull { value -> markers.any { value.contains(it, ignoreCase = true) } }

        return CheckResult(id, category, severity, detected = match != null, detail = match)
    }
}
