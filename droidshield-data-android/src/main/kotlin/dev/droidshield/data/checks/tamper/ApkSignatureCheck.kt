package dev.droidshield.data.checks.tamper

import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.security.MessageDigest

/**
 * CHECKS_SEED_LIST.md TAMPER #1 — the core anti-repackaging check. Compares
 * the running app's signing certificate hash against [expectedSha256Hashes]
 * (lowercase hex), which the integrator supplies at construction time
 * (there's no way for DroidShield itself to know the "correct" signature —
 * that's the integrator's own release-signing secret).
 */
class ApkSignatureCheck(
    private val expectedSha256Hashes: Set<String>,
) : ThreatCheck {
    override val id: String = "tamper.apk_signature"
    override val category: ThreatCategory = ThreatCategory.TAMPER
    override val severity: Severity = Severity.CRITICAL

    override fun evaluate(context: CheckContext): CheckResult {
        if (expectedSha256Hashes.isEmpty()) {
            // Not configured — this check can't do anything meaningful
            // without a pinned hash to compare against.
            return CheckResult(id, category, severity, detected = false, detail = "not configured")
        }

        val androidContext = (context as AndroidCheckContext).androidContext
        val actualHashes = try {
            readSigningCertificateHashes(androidContext)
        } catch (e: Exception) {
            return CheckResult(id, category, severity, detected = false, detail = "unreadable: ${e.message}")
        }

        val mismatch = actualHashes.isEmpty() || actualHashes.none { it in expectedSha256Hashes }

        return CheckResult(id, category, severity, detected = mismatch, detail = actualHashes.joinToString(","))
    }

    private fun readSigningCertificateHashes(androidContext: android.content.Context): List<String> {
        val pm = androidContext.packageManager
        val packageName = androidContext.packageName

        val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            // apkContentsSigners in both branches, deliberately.
            // signingCertificateHistory returns every certificate this
            // package has *ever* been signed with, including ones rotated
            // away from — so an attacker holding a superseded key could
            // repackage the app and still match a pinned hash. Only the
            // certificate that actually signed the running APK is a valid
            // anti-repackaging comparison, and that is what
            // apkContentsSigners reports for single and multiple signers
            // alike.
            info.signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            info.signatures ?: emptyArray()
        }

        val digest = MessageDigest.getInstance("SHA-256")
        return signatures.map { signature ->
            digest.reset()
            digest.digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
