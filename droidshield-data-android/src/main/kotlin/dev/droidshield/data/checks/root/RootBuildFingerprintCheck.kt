package dev.droidshield.data.checks.root

import android.os.Build
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md ROOT #8. Explicitly noted in the seed list as
 * overlapping with emulator detection (a custom/generic build fingerprint
 * is a shared signal for both categories) — kept as a separate check here
 * with `ThreatCategory.ROOT` because a custom ROM isn't necessarily an
 * emulator, and `dev.droidshield.data.checks.emulator.BuildFingerprintCheck`
 * checks for emulator-specific values rather than "custom build" ones.
 */
class RootBuildFingerprintCheck : ThreatCheck {
    override val id: String = "root.custom_rom_fingerprint"
    override val category: ThreatCategory = ThreatCategory.ROOT
    override val severity: Severity = Severity.LOW

    private val customRomMarkers = listOf("test-keys", "userdebug", "eng")

    override fun evaluate(context: CheckContext): CheckResult {
        val fingerprint = Build.FINGERPRINT ?: ""
        val match = customRomMarkers.firstOrNull { fingerprint.contains(it, ignoreCase = true) }
        return CheckResult(id, category, severity, detected = match != null, detail = fingerprint)
    }
}
