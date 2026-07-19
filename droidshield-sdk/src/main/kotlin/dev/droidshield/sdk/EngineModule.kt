package dev.droidshield.sdk

import dagger.Module
import dagger.Provides
import dev.droidshield.domain.ThreatCheck
import dev.droidshield.domain.ThreatReporter
import dev.droidshield.domain.TelemetrySink
import dev.droidshield.engine.CheckOrder
import dev.droidshield.engine.ThreatDetectionEngine
import javax.inject.Singleton

@Module
object EngineModule {
    @Provides @Singleton
    fun providesEngine(
        checks: Set<@JvmSuppressWildcards ThreatCheck>,
        reporter: ThreatReporter,
        telemetrySink: TelemetrySink,
        config: DroidShieldConfig,
    ): ThreatDetectionEngine {
        val order = config.polymorphicSeed?.let { CheckOrder.Seeded(it) } ?: CheckOrder.Unseeded
        return ThreatDetectionEngine(checks, reporter, telemetrySink, order)
    }
}
