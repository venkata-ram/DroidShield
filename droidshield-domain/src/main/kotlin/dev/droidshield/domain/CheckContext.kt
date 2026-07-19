package dev.droidshield.domain

/**
 * Opaque marker passed into [ThreatCheck.evaluate]. Deliberately empty here
 * — the domain module has zero Android imports (see ARCHITECTURE.md §2),
 * so it cannot know about `android.content.Context`. The concrete,
 * Android-backed implementation lives in `droidshield-data-android`
 * (`AndroidCheckContext`), which is what a Kotlin/Android check actually
 * receives and casts/destructures. See DECISIONS.md D018.
 */
interface CheckContext
