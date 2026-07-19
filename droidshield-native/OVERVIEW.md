# droidshield-native

C++ checks that are meaningfully harder to defeat in native code — see
DECISIONS.md D002 for the boundary rule (not everything belongs here).

Currently contains only a JNI-bridge scaffold (`NativeBridge.isLoaded()`)
proving the CMake/JNI wiring builds — no real checks yet (DECISIONS.md
D023). First real candidates per `CHECKS_SEED_LIST.md`: ptrace self-attach
anti-debug, `/proc/self/maps` Frida/Xposed signature scan, native
signature/checksum verification.

Own build toolchain (CMake/NDK) and own contribution bar (C++ + security
background) — kept separate from `droidshield-data-android` for exactly
that reason (ARCHITECTURE.md §3).
