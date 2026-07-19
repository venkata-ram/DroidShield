package dev.droidshield.domain

/**
 * Contract every threat check implements. See ARCHITECTURE.md §4 for the
 * extensibility rationale and DECISIONS.md D016 for the threading
 * constraint on [evaluate].
 *
 * evaluate() must not perform blocking disk/network I/O on the calling
 * thread when invoked from a lifecycle callback (e.g. Application.onCreate)
 * — see DECISIONS.md D016.
 */
interface ThreatCheck {
    val id: String
    val category: ThreatCategory
    val severity: Severity
    fun evaluate(context: CheckContext): CheckResult
}
