package dev.droidshield.sdk

import android.content.Context
import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.ThreatReporter
import dev.droidshield.domain.TelemetrySink

/**
 * The only class most integrators ever import (ARCHITECTURE.md §3). Not a
 * singleton object holding global mutable state on purpose — [init]
 * returns an instance so a host app can (in principle) run more than one
 * configured instance, e.g. for testing, though the common case is to keep
 * the single returned instance around for the app's lifetime.
 */
class DroidShield private constructor(
    private val component: DroidShieldComponent,
    private val androidContext: Context,
) {
    /** Runs every registered check once and returns all results (not just detected ones). */
    fun runChecks(): List<CheckResult> {
        val engine = component.engine()
        return engine.runAll(AndroidCheckContext(androidContext))
    }

    companion object {
        /**
         * @param reporter defaults to [LogcatThreatReporter] — see DECISIONS.md D011.
         * @param telemetrySink defaults to [NoOpTelemetrySink] — see DECISIONS.md D011.
         */
        fun init(
            context: Context,
            config: DroidShieldConfig = DroidShieldConfig(),
            reporter: ThreatReporter = LogcatThreatReporter(),
            telemetrySink: TelemetrySink = NoOpTelemetrySink(),
        ): DroidShield {
            val applicationContext = context.applicationContext
            val component = DaggerDroidShieldComponent.factory().create(
                applicationContext,
                config,
                ReportingModule(reporter),
                TelemetryModule(telemetrySink),
            )
            return DroidShield(component, applicationContext)
        }
    }
}
