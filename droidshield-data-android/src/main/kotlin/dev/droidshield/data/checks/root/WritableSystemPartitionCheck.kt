package dev.droidshield.data.checks.root

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.io.File

/**
 * CHECKS_SEED_LIST.md ROOT #5. Reads `/proc/mounts` rather than attempting
 * an actual write to `/system` — a read-only parse is enough signal and
 * avoids the check itself needing elevated permissions or leaving a file
 * behind.
 */
class WritableSystemPartitionCheck : ThreatCheck {
    override val id: String = "root.writable_system_partition"
    override val category: ThreatCategory = ThreatCategory.ROOT
    override val severity: Severity = Severity.MEDIUM

    override fun evaluate(context: CheckContext): CheckResult {
        val mounts = try {
            File("/proc/mounts").readLines()
        } catch (e: Exception) {
            return CheckResult(id, category, severity, detected = false)
        }

        val systemMountRw = mounts.any { line ->
            val fields = line.split(" ")
            fields.size >= 4 &&
                (fields[1] == "/system" || fields[1] == "/") &&
                fields[3].split(",").contains("rw")
        }

        return CheckResult(id, category, severity, detected = systemMountRw)
    }
}
