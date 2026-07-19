package dev.droidshield.sdk.checks

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.droidshield.data.checks.emulator.BatteryStatusCheck
import dev.droidshield.data.checks.emulator.BuildFingerprintCheck
import dev.droidshield.data.checks.emulator.BuildProductBoardCheck
import dev.droidshield.data.checks.emulator.CpuInfoCheck
import dev.droidshield.data.checks.emulator.InstalledPackageHeuristicCheck
import dev.droidshield.data.checks.emulator.NetworkInterfaceCheck
import dev.droidshield.data.checks.emulator.QemuArtifactCheck
import dev.droidshield.data.checks.emulator.SensorAbsenceCheck
import dev.droidshield.data.checks.emulator.TelephonyPropertiesCheck
import dev.droidshield.domain.ThreatCheck

@Module
object EmulatorChecksModule {
    @Provides @IntoSet
    fun providesBuildFingerprintCheck(): ThreatCheck = BuildFingerprintCheck()

    @Provides @IntoSet
    fun providesBuildProductBoardCheck(): ThreatCheck = BuildProductBoardCheck()

    @Provides @IntoSet
    fun providesTelephonyPropertiesCheck(): ThreatCheck = TelephonyPropertiesCheck()

    @Provides @IntoSet
    fun providesSensorAbsenceCheck(): ThreatCheck = SensorAbsenceCheck()

    @Provides @IntoSet
    fun providesQemuArtifactCheck(): ThreatCheck = QemuArtifactCheck()

    @Provides @IntoSet
    fun providesCpuInfoCheck(): ThreatCheck = CpuInfoCheck()

    @Provides @IntoSet
    fun providesNetworkInterfaceCheck(): ThreatCheck = NetworkInterfaceCheck()

    @Provides @IntoSet
    fun providesBatteryStatusCheck(): ThreatCheck = BatteryStatusCheck()

    @Provides @IntoSet
    fun providesInstalledPackageHeuristicCheck(): ThreatCheck = InstalledPackageHeuristicCheck()
}
