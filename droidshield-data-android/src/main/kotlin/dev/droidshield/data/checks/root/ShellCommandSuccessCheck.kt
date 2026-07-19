package dev.droidshield.data.checks.root

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md ROOT #9. Attempts `su -c id`, which should fail with
 * a permission/not-found error on a non-rooted device; unexpected success
 * indicates root access.
 */
class ShellCommandSuccessCheck : ThreatCheck {
    override val id: String = "root.shell_command_success"
    override val category: ThreatCategory = ThreatCategory.ROOT
    override val severity: Severity = Severity.HIGH

    override fun evaluate(context: CheckContext): CheckResult {
        val detected = try {
            val process = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            exitCode == 0 && output.contains("uid=")
        } catch (e: Exception) {
            false
        }

        return CheckResult(id, category, severity, detected)
    }
}
