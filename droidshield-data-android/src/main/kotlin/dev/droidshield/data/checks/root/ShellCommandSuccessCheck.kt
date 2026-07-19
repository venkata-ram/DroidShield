package dev.droidshield.data.checks.root

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.util.concurrent.TimeUnit

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
        var process: Process? = null
        val detected = try {
            process = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()

            // On a rooted device the su daemon typically prompts the user
            // for consent, and `id` emits nothing until they answer. Wait
            // *before* reading: readText() runs to EOF, so reading first
            // would block the calling thread for as long as the dialog sat
            // unanswered — indefinitely if the app is in the background,
            // and this runs on the caller's thread (D016).
            //
            // `id` writes well under a pipe buffer, so a process that has
            // already exited has its full output buffered and ready; there
            // is no deadlock risk in waiting first here.
            val exited = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!exited) {
                // Inconclusive, not clean — but reporting a threat off a
                // timeout would false-positive on a slow device, so stay
                // conservative and let the other ROOT checks carry it.
                false
            } else {
                val output = process.inputStream.bufferedReader().readText()
                process.exitValue() == 0 && output.contains("uid=")
            }
        } catch (e: Exception) {
            false
        } finally {
            process?.destroy()
        }

        return CheckResult(id, category, severity, detected)
    }

    private companion object {
        const val TIMEOUT_SECONDS = 2L
    }
}
