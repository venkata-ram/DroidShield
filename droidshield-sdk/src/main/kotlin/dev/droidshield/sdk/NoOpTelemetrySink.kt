package dev.droidshield.sdk

import dev.droidshield.domain.TelemetryEvent
import dev.droidshield.domain.TelemetrySink

/** Silent default — see DECISIONS.md D011. */
class NoOpTelemetrySink : TelemetrySink {
    override fun capture(event: TelemetryEvent) = Unit
}
