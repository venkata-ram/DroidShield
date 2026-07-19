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
    /**
     * Defaults to `true` to match the *platform's* default for
     * `android:allowBackup`, which is `true` when the attribute is omitted
     * from the manifest. Defaulting this to `false` made
     * [dev.droidshield.data.checks.tamper.ManifestTamperCheck] report a
     * tamper on every clean run of any app that hadn't explicitly opted
     * out of backup. An integrator who does set
     * `android:allowBackup="false"` must set this to `false` to match.
     */
    val expectedManifestAllowBackup: Boolean = true,
    val expectedInstallerPackage: String = "com.android.vending",
    /**
     * Optional seed for release-specific check ordering. Null means
     * unseeded/deterministic ordering. See DECISIONS.md D026 for how the
     * Gradle plugin generates it.
     */
    val polymorphicSeed: Long? = null,
    /** Minimum delay between check runs triggered by [DroidShieldGuarded] methods. */
    val guardedMethodMinIntervalMillis: Long = 30_000L,
) {
    init {
        require(guardedMethodMinIntervalMillis >= 0L) {
            "guardedMethodMinIntervalMillis must be non-negative"
        }
    }
}
