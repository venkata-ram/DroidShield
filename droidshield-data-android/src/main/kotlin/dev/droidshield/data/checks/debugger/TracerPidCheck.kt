package dev.droidshield.data.checks.debugger

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.io.File

/**
 * CHECKS_SEED_LIST.md DEBUGGER #3. `TracerPid` in `/proc/self/status` is
 * non-zero when a tracer (debugger, or a tool like Frida using ptrace) is
 * attached. This is a plain file read — no native code required, unlike
 * the ptrace-self-attach check (#6, see droidshield-native).
 */
class TracerPidCheck : ThreatCheck {
    override val id: String = "debugger.tracer_pid"
    override val category: ThreatCategory = ThreatCategory.DEBUGGER
    override val severity: Severity = Severity.MEDIUM

    override fun evaluate(context: CheckContext): CheckResult {
        val tracerPid = try {
            File("/proc/self/status").readLines()
                .firstOrNull { it.startsWith("TracerPid:") }
                ?.substringAfter(":")
                ?.trim()
                ?.toIntOrNull()
        } catch (e: Exception) {
            null
        }

        return CheckResult(id, category, severity, detected = (tracerPid ?: 0) != 0, detail = tracerPid?.toString())
    }
}
