package dev.droidshield.sdk

import android.util.Log
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.ThreatReporter

/** Zero-setup default — see DECISIONS.md D011. */
class LogcatThreatReporter(private val tag: String = "DroidShield") : ThreatReporter {
    override fun onThreatDetected(result: CheckResult) {
        Log.w(tag, "Threat detected: ${result.checkId} category=${result.category} severity=${result.severity} detail=${result.detail}")
    }
}
