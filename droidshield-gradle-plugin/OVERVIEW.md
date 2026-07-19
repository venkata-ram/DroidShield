# droidshield-gradle-plugin

Generates `DroidShieldBuildSeed.kt` and wires it into an Android/Kotlin
consumer's main source set. The seed is stable for a project version and can
be pinned with `-PdroidshieldSeed=<n>` for reproducibility.

The plugin does not instrument or rewrite host-app bytecode. The generated seed
only affects check ordering when the host passes it to `DroidShieldConfig`.
Contributors adding a check should not need to read this module.
