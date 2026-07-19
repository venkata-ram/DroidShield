package dev.droidshield.data.checks.emulator

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.io.File

/** CHECKS_SEED_LIST.md EMULATOR #6. */
class CpuInfoCheck : ThreatCheck {
    override val id: String = "emulator.cpu_info"
    override val category: ThreatCategory = ThreatCategory.EMULATOR
    override val severity: Severity = Severity.MEDIUM

    private val markers = listOf("goldfish", "ranchu", "intel virtual", "vbox")

    override fun evaluate(context: CheckContext): CheckResult {
        val cpuInfo = try {
            File("/proc/cpuinfo").readText()
        } catch (e: Exception) {
            return CheckResult(id, category, severity, detected = false)
        }

        val match = markers.firstOrNull { cpuInfo.contains(it, ignoreCase = true) }
        return CheckResult(id, category, severity, detected = match != null, detail = match)
    }
}
