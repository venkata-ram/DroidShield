package dev.droidshield.sdk

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dev.droidshield.engine.ThreatDetectionEngine
import dev.droidshield.sdk.checks.ChecksModule
import javax.inject.Singleton

/** ARCHITECTURE.md §6. */
@Singleton
@Component(modules = [ChecksModule::class, ReportingModule::class, TelemetryModule::class, EngineModule::class])
internal interface DroidShieldComponent {
    fun engine(): ThreatDetectionEngine

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance context: Context,
            @BindsInstance config: DroidShieldConfig,
            reportingModule: ReportingModule,
            telemetryModule: TelemetryModule,
        ): DroidShieldComponent
    }
}
