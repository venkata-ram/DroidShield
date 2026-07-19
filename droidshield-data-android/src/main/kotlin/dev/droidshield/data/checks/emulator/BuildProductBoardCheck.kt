package dev.droidshield.data.checks.emulator

import android.os.Build
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/** CHECKS_SEED_LIST.md EMULATOR #2. */
class BuildProductBoardCheck : ThreatCheck {
    override val id: String = "emulator.build_product_board"
    override val category: ThreatCategory = ThreatCategory.EMULATOR
    override val severity: Severity = Severity.MEDIUM

    private val markers = listOf("sdk_gphone", "vbox86p", "vbox86", "sdk_x86", "emulator", "simulator")

    override fun evaluate(context: CheckContext): CheckResult {
        val candidates = listOf(Build.PRODUCT, Build.BOARD).filterNotNull()
        val match = candidates.firstOrNull { value -> markers.any { value.contains(it, ignoreCase = true) } }

        return CheckResult(id, category, severity, detected = match != null, detail = match)
    }
}
