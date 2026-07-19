# droidshield-gradle-plugin

AGP Instrumentation API + ASM `AsmClassVisitorFactory` wiring that calls
into `droidshield-engine` at build time (ARCHITECTURE.md §3).

Currently a no-op scaffold (`DroidShieldPlugin`) — proves the
`java-gradle-plugin` + Kotlin setup resolves and applies cleanly. The
actual bytecode instrumentation is not implemented yet (DECISIONS.md
D023). This is the module contributors adding a check should *not* need to
read (ARCHITECTURE.md §1 goal).
