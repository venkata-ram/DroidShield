# droidshield-sdk

The public `.aar`. Will contain the `DroidShield` facade, the Dagger 2
component (`DroidShieldComponent`), and the wiring connecting
`data-android` + `native` implementations to `engine` at runtime
(ARCHITECTURE.md §6).

Not yet implemented (DECISIONS.md D023) — currently just the module shell
with dependencies declared (`domain`, `data-android`, `native`, `engine`,
Dagger + kapt) so the graph is ready to wire once at least one check
category exists in `droidshield-data-android` to bind.

Consumer ProGuard rules ship from here (`consumer-rules.pro`,
DECISIONS.md D014) — currently empty pending real Dagger-generated code.
