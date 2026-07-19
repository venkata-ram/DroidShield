package dev.droidshield.data.checks.emulator

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.io.File

/** CHECKS_SEED_LIST.md EMULATOR #5 — a strong Android-emulator signal. */
class QemuArtifactCheck : ThreatCheck {
    override val id: String = "emulator.qemu_artifact"
    override val category: ThreatCategory = ThreatCategory.EMULATOR
    override val severity: Severity = Severity.HIGH

    private val qemuPaths = listOf("/dev/qemu_pipe", "/dev/socket/qemud", "/dev/socket/baseband_genyd")

    override fun evaluate(context: CheckContext): CheckResult {
        val match = qemuPaths.firstOrNull { File(it).exists() }
        return CheckResult(id, category, severity, detected = match != null, detail = match)
    }
}
