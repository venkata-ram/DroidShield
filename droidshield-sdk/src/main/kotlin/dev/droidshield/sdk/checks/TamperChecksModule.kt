package dev.droidshield.sdk.checks

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.droidshield.data.checks.tamper.ApkSignatureCheck
import dev.droidshield.data.checks.tamper.AssetTamperCheck
import dev.droidshield.data.checks.tamper.DexIntegrityCheck
import dev.droidshield.data.checks.tamper.EngineSelfPresenceCheck
import dev.droidshield.data.checks.tamper.InstallerSourceCheck
import dev.droidshield.data.checks.tamper.ManifestTamperCheck
import dev.droidshield.data.checks.tamper.NativeLibraryIntegrityCheck
import dev.droidshield.domain.ThreatCheck
import dev.droidshield.sdk.DroidShieldConfig

/**
 * Several TAMPER checks need integrator-supplied expected values (pinned
 * signature hash, DEX CRCs, etc.) — those `@Provides` methods take
 * [DroidShieldConfig] as a parameter, which Dagger resolves from the
 * `@BindsInstance` binding on [dev.droidshield.sdk.DroidShieldComponent].
 * This keeps the module itself an `object` (no constructor), consistent
 * with every other category module.
 */
@Module
object TamperChecksModule {
    @Provides @IntoSet
    fun providesApkSignatureCheck(config: DroidShieldConfig): ThreatCheck =
        ApkSignatureCheck(config.expectedApkSignatureSha256Hashes)

    @Provides @IntoSet
    fun providesInstallerSourceCheck(config: DroidShieldConfig): ThreatCheck =
        InstallerSourceCheck(config.expectedInstallerPackage)

    @Provides @IntoSet
    fun providesDexIntegrityCheck(config: DroidShieldConfig): ThreatCheck =
        DexIntegrityCheck(config.expectedDexCrc32ByEntryName)

    @Provides @IntoSet
    fun providesNativeLibraryIntegrityCheck(config: DroidShieldConfig): ThreatCheck =
        NativeLibraryIntegrityCheck(config.nativeLibraryFileName, config.expectedNativeLibrarySha256)

    @Provides @IntoSet
    fun providesManifestTamperCheck(config: DroidShieldConfig): ThreatCheck =
        ManifestTamperCheck(config.expectedManifestDebuggable, config.expectedManifestAllowBackup)

    @Provides @IntoSet
    fun providesAssetTamperCheck(config: DroidShieldConfig): ThreatCheck =
        AssetTamperCheck(config.tamperCheckedAssetPath, config.expectedTamperCheckedAssetSha256)

    @Provides @IntoSet
    fun providesEngineSelfPresenceCheck(): ThreatCheck = EngineSelfPresenceCheck()
}
