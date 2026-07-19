package dev.droidshield.data.checks.tamper

import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.io.File
import java.security.MessageDigest

/**
 * CHECKS_SEED_LIST.md TAMPER #4. Unlike HOOK #5's runtime checksum (see
 * droidshield-native — compares on-disk vs in-memory bytes to catch
 * runtime hooking), this is a pure on-disk file hash against an
 * integrator-supplied expected value, catching static patching of the
 * `.so` file itself. No native code needed for a file-level hash — see
 * DECISIONS.md D002 for the native-vs-Kotlin boundary rule.
 */
class NativeLibraryIntegrityCheck(
    private val libraryFileName: String,
    private val expectedSha256Hex: String,
) : ThreatCheck {
    override val id: String = "tamper.native_library_integrity"
    override val category: ThreatCategory = ThreatCategory.TAMPER
    override val severity: Severity = Severity.HIGH

    override fun evaluate(context: CheckContext): CheckResult {
        if (expectedSha256Hex.isBlank()) {
            return CheckResult(id, category, severity, detected = false, detail = "not configured")
        }

        val androidContext = (context as AndroidCheckContext).androidContext
        val nativeLibDir = androidContext.applicationInfo.nativeLibraryDir
        val libraryFile = File(nativeLibDir, libraryFileName)

        if (!libraryFile.exists()) {
            // Reported clean before, which was backwards: reaching here
            // means the integrator pinned a hash for this library (the
            // blank-hash case already returned above), so the library is
            // expected to be present. A pinned library that has vanished
            // is exactly the stripping/patching this check exists to
            // catch — failing open let an attacker defeat it by deleting
            // the file rather than modifying it.
            return CheckResult(id, category, severity, detected = true, detail = "library not found: $libraryFileName")
        }

        val actualHash = try {
            val digest = MessageDigest.getInstance("SHA-256")
            libraryFile.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            return CheckResult(id, category, severity, detected = false, detail = "unreadable: ${e.message}")
        }

        return CheckResult(
            id,
            category,
            severity,
            detected = !actualHash.equals(expectedSha256Hex, ignoreCase = true),
            detail = actualHash,
        )
    }
}
