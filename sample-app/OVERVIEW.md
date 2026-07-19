# sample-app

End-to-end integration demo for DroidShield.

It is a **standalone Gradle build** — deliberately not included in the root
`settings.gradle.kts` — that resolves `droidshield-sdk` and the
`dev.droidshield` Gradle plugin from JitPack, exactly as an external
integrator would. Nothing here uses `project(":droidshield-sdk")`, so if a
release fails to publish correctly, this build breaks and says so.

```bash
cd sample-app && ../gradlew assembleDebug
```

Needs a `local.properties` with `sdk.dir=...` (Android Studio writes one when
you open the directory as a project) or `ANDROID_HOME` set in the environment.

`SampleApplication` applies the plugin-generated `DroidShieldBuildSeed` to
`DroidShieldConfig` and runs the checks on a coroutine, logging results under
the `DroidShieldSample` logcat tag. `MainActivity` exists only to give the
launcher something to point at — the SDK itself is headless (DECISIONS.md
D015).

Because the version is pinned to a published release, this app lags the
working tree by design. To exercise uncommitted SDK changes instead, run
`./gradlew publishAllToMavenLocal` in the root build, then add `mavenLocal()`
to this build's repositories and switch the coordinate to
`dev.droidshield:droidshield-sdk`.
