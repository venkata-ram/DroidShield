# droidshield-data-android

The module most contributors touch. Contains `AndroidCheckContext` (the
concrete `CheckContext` — see DECISIONS.md D018) and, going forward, one
small class per `ThreatCheck` implementation, organized by category
(`checks/root/`, `checks/debugger/`, etc.).

Actual checks are not yet implemented — see DECISIONS.md D023 and
`CHECKS_SEED_LIST.md` for the researched candidates to implement, one
category at a time (ROOT first).

## Adding a new check (once the template exists)

1. Copy `checks/TEMPLATE.kt` into the right category package.
2. Implement `evaluate()`.
3. Register it via one `@Provides @IntoSet` line in that category's Dagger
   module (see ARCHITECTURE.md §6 — not yet wired, tracked in
   `droidshield-sdk/OVERVIEW.md`).
4. Add a unit test next to it.
