# droidshield-engine

Not yet implemented (scaffold only — see DECISIONS.md D023).

Will hold:
- `ThreatDetectionEngine` — runs a `Set<ThreatCheck>`, collects `CheckResult`s.
- The polymorphic injection engine (seeded shuffling of check
  subset/ordering per build, per ARCHITECTURE.md §1/§8).

Depends only on `droidshield-domain`. Never add an Android or check-specific
dependency here — see DECISIONS.md D022 for why.
