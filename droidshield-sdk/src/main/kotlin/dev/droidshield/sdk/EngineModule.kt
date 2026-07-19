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
        // An explicit config value wins; otherwise fall back to the seed the
        // Gradle plugin generated into the host app, so applying the plugin
        // gives release-seeded ordering with no config wiring. Null from both
        // means unseeded/deterministic ordering. See DECISIONS.md D038.
        val seed = config.polymorphicSeed ?: GeneratedBuildSeed.value
        val order = seed?.let { CheckOrder.Seeded(it) } ?: CheckOrder.Unseeded
        return ThreatDetectionEngine(checks, reporter, telemetrySink, order)
    }
}
