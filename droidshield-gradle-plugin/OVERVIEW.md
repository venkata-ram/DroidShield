# droidshield-gradle-plugin

Provides three focused Android build features:

- Instruments application methods explicitly annotated with
  `@DroidShieldGuarded`, injecting a background runtime-check trigger through
  AGP's supported ASM instrumentation API. Dependency classes are not touched.
- Attaches `verifyDroidShieldReleaseHardening` to release builds and rejects a
  debuggable release or one without R8 minification and resource shrinking.
- Generates `DroidShieldBuildSeed.kt` for reproducible, release-specific check
  ordering. The seed can be pinned with `-PdroidshieldSeed=<n>`.

Instrumentation and hardening enforcement can be disabled independently through
the `droidShield` extension. Neither feature claims to make client code
unbypassable. Contributors adding a threat check should not need this module.
