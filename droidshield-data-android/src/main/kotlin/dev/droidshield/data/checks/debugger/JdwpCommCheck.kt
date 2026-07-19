package dev.droidshield.data.checks.debugger

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.io.File

/**
 * CHECKS_SEED_LIST.md DEBUGGER #7 — community-contributed technique
 * (flagged in OWASP's own issue tracker per the seed list) surfacing JDWP
 * debuggability by scanning task command names under `/proc/self/task/`.
 */
class JdwpCommCheck : ThreatCheck {
    override val id: String = "debugger.jdwp_task_comm"
    override val category: ThreatCategory = ThreatCategory.DEBUGGER
    override val severity: Severity = Severity.LOW

    override fun evaluate(context: CheckContext): CheckResult {
        val detected = try {
            File("/proc/self/task").listFiles()
                ?.mapNotNull { taskDir ->
                    runCatching { File(taskDir, "comm").readText().trim() }.getOrNull()
                }
                ?.any { it.contains("jdwp", ignoreCase = true) }
                ?: false
        } catch (e: Exception) {
            false
        }

        return CheckResult(id, category, severity, detected)
    }
}
