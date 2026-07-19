package dev.droidshield.sdk.checks

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.droidshield.data.checks.root.BuildTagsCheck
import dev.droidshield.data.checks.root.DangerousSystemPropertiesCheck
import dev.droidshield.data.checks.root.RootBuildFingerprintCheck
import dev.droidshield.data.checks.root.RootManagementAppCheck
import dev.droidshield.data.checks.root.RunningProcessRootCheck
import dev.droidshield.data.checks.root.ShellCommandSuccessCheck
import dev.droidshield.data.checks.root.SuBinaryPathCheck
import dev.droidshield.data.checks.root.WritableSystemPartitionCheck
import dev.droidshield.domain.ThreatCheck

/**
 * ARCHITECTURE.md §6 — contributors add one `@Provides @IntoSet` line here
 * for a new ROOT check, per DECISIONS.md D009. This module never needs to
 * change to accommodate other categories.
 */
@Module
object RootChecksModule {
    @Provides @IntoSet
    fun providesSuBinaryPathCheck(): ThreatCheck = SuBinaryPathCheck()

    @Provides @IntoSet
    fun providesRootManagementAppCheck(): ThreatCheck = RootManagementAppCheck()

    @Provides @IntoSet
    fun providesRunningProcessRootCheck(): ThreatCheck = RunningProcessRootCheck()

    @Provides @IntoSet
    fun providesBuildTagsCheck(): ThreatCheck = BuildTagsCheck()

    @Provides @IntoSet
    fun providesWritableSystemPartitionCheck(): ThreatCheck = WritableSystemPartitionCheck()

    @Provides @IntoSet
    fun providesDangerousSystemPropertiesCheck(): ThreatCheck = DangerousSystemPropertiesCheck()

    @Provides @IntoSet
    fun providesRootBuildFingerprintCheck(): ThreatCheck = RootBuildFingerprintCheck()

    @Provides @IntoSet
    fun providesShellCommandSuccessCheck(): ThreatCheck = ShellCommandSuccessCheck()
}
