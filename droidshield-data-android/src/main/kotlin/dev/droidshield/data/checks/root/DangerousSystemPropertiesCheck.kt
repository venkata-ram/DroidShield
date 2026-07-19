package dev.droidshield.data.checks.root

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md ROOT #6. Reads `android.os.SystemProperties` via
 * reflection since it's a non-public API — greylisted rather than fully
 * blocked as of recent Android hidden-API policy, but this can start
 * failing on a future Android version, in which case [evaluate] degrades
 * to `detected = false` rather than throwing (see the engine's
 * catch-and-skip behavior in ThreatDetectionEngine, which would otherwise
 * treat a reflection failure as a check_error rather than a clean result —
 * this check chooses "assume clean" instead to avoid a brittle-reflection
 * false positive).
 */
class DangerousSystemPropertiesCheck : ThreatCheck {
    override val id: String = "root.dangerous_system_properties"
    override val category: ThreatCategory = ThreatCategory.ROOT
    override val severity: Severity = Severity.MEDIUM

    override fun evaluate(context: CheckContext): CheckResult {
        val roDebuggable = getSystemProperty("ro.debuggable")
        val roSecure = getSystemProperty("ro.secure")

        val detected = roDebuggable == "1" || roSecure == "0"
        return CheckResult(
            id,
            category,
            severity,
            detected,
            detail = "ro.debuggable=$roDebuggable ro.secure=$roSecure",
        )
    }

    private fun getSystemProperty(key: String): String? = try {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java)
        get.invoke(null, key) as? String
    } catch (e: Exception) {
        null
    }
}
