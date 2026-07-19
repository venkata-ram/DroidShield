package dev.droidshield.data.checks.tamper

import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.util.zip.ZipFile

/**
 * CHECKS_SEED_LIST.md TAMPER #3 and #6 (merged: multi-DEX integrity is the
 * same mechanism as single-DEX, just applied to every `classesN.dex`
 * entry instead of only the primary one — repackaging tools often target
 * secondary DEX files specifically to evade a naive single-file check, per
 * the seed list's note on #6).
 *
 * Compares each DEX entry's CRC32 (read from the APK's ZIP central
 * directory, which `ZipFile` exposes without decompressing the entry) to
 * [expectedCrc32ByEntryName], supplied by the integrator at build time —
 * DroidShield has no way to know the "correct" checksum of the
 * integrator's own build output.
 */
class DexIntegrityCheck(
    private val expectedCrc32ByEntryName: Map<String, Long>,
) : ThreatCheck {
    override val id: String = "tamper.dex_integrity"
    override val category: ThreatCategory = ThreatCategory.TAMPER
    override val severity: Severity = Severity.HIGH

    override fun evaluate(context: CheckContext): CheckResult {
        if (expectedCrc32ByEntryName.isEmpty()) {
            return CheckResult(id, category, severity, detected = false, detail = "not configured")
        }

        val androidContext = (context as AndroidCheckContext).androidContext
        val apkPath = androidContext.applicationInfo.sourceDir

        val mismatched = try {
            ZipFile(apkPath).use { zip ->
                expectedCrc32ByEntryName.filter { (entryName, expectedCrc) ->
                    val entry = zip.getEntry(entryName)
                    entry == null || entry.crc != expectedCrc
                }.keys
            }
        } catch (e: Exception) {
            return CheckResult(id, category, severity, detected = false, detail = "unreadable: ${e.message}")
        }

        return CheckResult(id, category, severity, detected = mismatched.isNotEmpty(), detail = mismatched.joinToString(","))
    }
}
