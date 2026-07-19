package dev.droidshield.sdk

import dagger.Module
import dagger.Provides
import dev.droidshield.domain.TelemetrySink
import javax.inject.Singleton

/** ARCHITECTURE.md §4a. Defaults to [NoOpTelemetrySink] — see DECISIONS.md D010/D011. */
@Module
class TelemetryModule(private val sink: TelemetrySink = NoOpTelemetrySink()) {
    @Provides @Singleton
    fun providesTelemetrySink(): TelemetrySink = sink
}
