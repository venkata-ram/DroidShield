# droidshield-domain

Pure Kotlin, zero Android imports. If you're adding a new `ThreatCheck`
implementation, you don't need to read this module beyond the contract
below — you need `droidshield-data-android`'s `OVERVIEW.md` instead.

## Contents

- `ThreatCategory` — the 5 detection categories. Append-only (DECISIONS.md D008).
- `Severity` — LOW/MEDIUM/HIGH/CRITICAL.
- `CheckResult` — what a check returns.
- `CheckContext` — opaque marker; see its KDoc and DECISIONS.md D018 for why
  it isn't just `android.content.Context`.
- `ThreatCheck` — the interface every check implements.
- `ThreatReporter` — security-signal callback.
- `TelemetryEvent` / `TelemetrySink` — operational-signal callback, kept
  deliberately separate from `ThreatReporter` (DECISIONS.md D010).

## Why this module has no Android dependency

So the contract is testable on the JVM without an emulator/Robolectric, and
so it's the first thing a contributor reads — reading it doesn't require
knowing anything about Gradle, ASM, or Dagger.
