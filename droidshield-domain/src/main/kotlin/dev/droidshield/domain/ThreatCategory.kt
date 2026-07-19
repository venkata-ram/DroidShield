package dev.droidshield.domain

/**
 * Append-only. Do not rename or remove a value — every ThreatCheck
 * implementation across every contributor's fork depends on this enum.
 * See DECISIONS.md D008.
 */
enum class ThreatCategory {
    ROOT,
    DEBUGGER,
    HOOK,
    EMULATOR,
    TAMPER,
}
