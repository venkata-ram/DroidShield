package dev.droidshield.sdk

import dagger.Module
import dagger.Provides
import dev.droidshield.domain.ThreatReporter
import javax.inject.Singleton

/** ARCHITECTURE.md §6. Defaults to [LogcatThreatReporter] — see DECISIONS.md D011. */
@Module
class ReportingModule(private val reporter: ThreatReporter) {
    @Provides @Singleton
    fun providesReporter(): ThreatReporter = reporter
}
