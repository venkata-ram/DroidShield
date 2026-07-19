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

    /**
     * A fingerprint is
     * `brand/product/device:version/id/incremental:BUILD_TYPE/tags`, so the
     * build type and tags are their own delimited fields. Matching the bare
     * substring "eng" anywhere in the string (as this did) is far too loose
     * — it hits any brand, product or incremental that merely happens to
     * contain those three letters, on a device that is not modified at all.
     * Anchor to the delimited fields instead.
     */
    private val customRomFingerprint =
        Regex(""":(eng|userdebug)/|[/:]test-keys(\b|$)""", RegexOption.IGNORE_CASE)

    override fun evaluate(context: CheckContext): CheckResult {
        val fingerprint = Build.FINGERPRINT ?: ""
        val detected = customRomFingerprint.containsMatchIn(fingerprint)
        return CheckResult(id, category, severity, detected = detected, detail = fingerprint)
    }
}
