# droidshield-engine

Contains:
- `ThreatDetectionEngine` — runs a `Set<ThreatCheck>`, collects `CheckResult`s.
- `CheckOrder` — deterministic or seeded runtime ordering, with an internal
  optional subset mode. The public SDK currently configures ordering only.

Depends only on `droidshield-domain`. Never add an Android or check-specific
dependency here — see DECISIONS.md D022 for why.
