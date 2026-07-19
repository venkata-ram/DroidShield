package dev.droidshield.sdk

/**
 * Values only the integrator can know — DroidShield can't invent its own
 * "correct" signing hash or asset checksum for someone else's build. Every
 * field defaults to a no-op/unconfigured value so omitting a field simply
 * disables the check(s) that depend on it rather than failing.
 */
data class DroidShieldConfig(
    val expectedApkSignatureSha256Hashes: Set<String> = emptySet(),
    val expectedDexCrc32ByEntryName: Map<String, Long> = emptyMap(),
    val nativeLibraryFileName: String = "libdroidshield.so",
    val expectedNativeLibrarySha256: String = "",
    val tamperCheckedAssetPath: String = "",
    val expectedTamperCheckedAssetSha256: String = "",
    val expectedManifestDebuggable: Boolean = false,
    val expectedManifestAllowBackup: Boolean = false,
    val expectedInstallerPackage: String = "com.android.vending",
    /**
     * Per-build polymorphic seed (ARCHITECTURE.md §1/§8). Null means
     * unseeded/deterministic ordering. See DECISIONS.md D026 for how this
     * is meant to be generated at build time.
     */
    val polymorphicSeed: Long? = null,
)
