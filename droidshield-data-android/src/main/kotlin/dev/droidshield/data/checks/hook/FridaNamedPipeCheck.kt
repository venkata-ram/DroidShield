package dev.droidshield.data.checks.hook

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.io.File

/**
 * CHECKS_SEED_LIST.md HOOK #3 — named pipes frida-server uses for external
 * communication, scanned by filename pattern under common tmp locations.
 */
class FridaNamedPipeCheck : ThreatCheck {
    override val id: String = "hook.frida_named_pipe"
    override val category: ThreatCategory = ThreatCategory.HOOK
    override val severity: Severity = Severity.MEDIUM

    private val candidateDirs = listOf("/data/local/tmp", "/proc/self/fd")

    override fun evaluate(context: CheckContext): CheckResult {
        val match = candidateDirs
            .mapNotNull { dir -> runCatching { File(dir).listFiles() }.getOrNull() }
            .flatMap { it.toList() }
            .firstOrNull { it.name.contains("frida", ignoreCase = true) || it.name.contains("linjector", ignoreCase = true) }

        return CheckResult(id, category, severity, detected = match != null, detail = match?.path)
    }
}
