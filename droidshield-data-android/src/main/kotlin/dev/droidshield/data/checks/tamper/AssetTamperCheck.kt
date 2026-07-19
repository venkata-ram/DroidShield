package dev.droidshield.data.checks.tamper

import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.security.MessageDigest

/**
 * CHECKS_SEED_LIST.md TAMPER #7. Hashes a specific asset the integrator
 * names ([assetPath], relative to `assets/`) and compares to
 * [expectedSha256Hex] — for critical bundled config/key-material assets
 * where the integrator wants to detect post-build modification.
 */
class AssetTamperCheck(
    private val assetPath: String,
    private val expectedSha256Hex: String,
) : ThreatCheck {
    override val id: String = "tamper.asset_integrity"
    override val category: ThreatCategory = ThreatCategory.TAMPER
    override val severity: Severity = Severity.MEDIUM

    override fun evaluate(context: CheckContext): CheckResult {
        if (expectedSha256Hex.isBlank()) {
            return CheckResult(id, category, severity, detected = false, detail = "not configured")
        }

        val androidContext = (context as AndroidCheckContext).androidContext

        val actualHash = try {
            val digest = MessageDigest.getInstance("SHA-256")
            androidContext.assets.open(assetPath).use { input ->
                digest.digest(input.readBytes())
            }.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            return CheckResult(id, category, severity, detected = false, detail = "unreadable: ${e.message}")
        }

        return CheckResult(
            id,
            category,
            severity,
            detected = !actualHash.equals(expectedSha256Hex, ignoreCase = true),
            detail = assetPath,
        )
    }
}
