package dev.droidshield.data.checks.root

import android.os.Build
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/** CHECKS_SEED_LIST.md ROOT #4. */
class BuildTagsCheck : ThreatCheck {
    override val id: String = "root.build_tags"
    override val category: ThreatCategory = ThreatCategory.ROOT
    override val severity: Severity = Severity.LOW

    override fun evaluate(context: CheckContext): CheckResult {
        val detected = Build.TAGS?.contains("test-keys") == true
        return CheckResult(id, category, severity, detected, detail = Build.TAGS)
    }
}
