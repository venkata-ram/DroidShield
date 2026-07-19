# DroidShield — Integration Guide

Full setup and usage details. For a short overview, see [README.md](README.md).

## Contents

- [Requirements](#requirements)
- [Setup via JitPack](#setup-via-jitpack)
- [Consuming a local build](#consuming-a-local-build)
- [Usage by scenario](#usage-by-scenario)
- [Modules](#modules)

---

## Requirements

Minimum SDK 24. JDK 17 is required to build.

## Setup via JitPack

DroidShield is **not on Maven Central** — the `dev.droidshield` namespace needs verified
ownership of `droidshield.dev`. It's consumed via JitPack, which builds straight from a
Git tag. JitPack rewrites the group ID to `com.github.<user>.<repo>`, so the coordinates
below differ from the `dev.droidshield` group the artifacts are built with.

### 1. Add the repository and map the plugin ID

The Gradle plugin needs an explicit mapping: a plugin marker has to live at a group ID
equal to the plugin ID, and JitPack's group rewriting breaks that convention. The
`resolutionStrategy` below is what bridges it.

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "dev.droidshield") {
                useModule("com.github.venkata-ram.DroidShield:droidshield-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### 2. Apply the plugin and depend on the SDK

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dev.droidshield") version "0.1.0"   // generates the per-build polymorphic seed
}

dependencies {
    implementation("com.github.venkata-ram.DroidShield:droidshield-sdk:0.1.0")
}
```

`droidshield-sdk` pulls in `-domain`, `-data-android`, `-native`, and `-engine`
transitively — you only ever declare the one coordinate.

## Consuming a local build

To integrate against uncommitted changes, publish to your local Maven repository and
point the consuming project at `mavenLocal()`. Coordinates are the real `dev.droidshield`
group in this case, and the plugin marker works normally, so no `resolutionStrategy` is
needed:

```bash
./gradlew publishAllToMavenLocal
```

```kotlin
implementation("dev.droidshield:droidshield-sdk:0.1.0")
```

`publishAllToMavenLocal` covers the five library modules *and* `droidshield-gradle-plugin`,
which is a separate included build (DECISIONS.md D027) and would otherwise be skipped.

---

## Usage by scenario

### Scenario 1 — Just tell me what's on this device

The zero-config path. Results go to logcat under the `DroidShield` tag.

```kotlin
class MyApp : Application() {
    private val scope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        val shield = DroidShield.init(this)
        scope.launch { shield.runChecksSuspending() }
    }
}
```

> Use `runChecksSuspending()`, not `runChecks()`. Checks do blocking disk I/O and one attempts a socket connect — the blocking variant will stall app startup if you call it directly from `onCreate`.

### Scenario 2 — React to threats in my own way

Implement `ThreatReporter`. It fires once per *detected* threat.

```kotlin
class MyReporter(private val analytics: Analytics) : ThreatReporter {
    override fun onThreatDetected(result: CheckResult) {
        analytics.track("threat", mapOf("id" to result.checkId))

        if (result.severity == Severity.CRITICAL) {
            // Your policy, your call: degrade the feature, force re-auth,
            // flag the session server-side, or hard-block.
        }
    }
}

DroidShield.init(context, reporter = MyReporter(analytics))
```

DroidShield deliberately never shows a dialog or kills your process. It's headless — it reports, you decide.

For the stronger version of this — sending results to your backend and letting the **server** decide what happens, so the policy isn't hardcoded into an APK the attacker controls — see **[SERVER_DRIVEN_DECISIONS.md](SERVER_DRIVEN_DECISIONS.md)**.

### Scenario 3 — Detect a repackaged APK

Tamper checks need values only you can know, supplied via `DroidShieldConfig`. Omit a field and its check simply goes quiet rather than false-positiving.

```kotlin
DroidShield.init(
    context,
    config = DroidShieldConfig(
        expectedApkSignatureSha256Hashes = setOf("A1:B2:..."), // your release signing cert
        expectedInstallerPackage = "com.android.vending",       // Play Store only
        expectedManifestDebuggable = false,
        expectedManifestAllowBackup = false,                    // must match your manifest
        expectedNativeLibrarySha256 = "..."
    )
)
```

> `expectedManifestAllowBackup` defaults to `true` because that's the *platform* default when the attribute is absent. If your manifest sets `android:allowBackup="false"`, set this to `false` too — otherwise every clean run reports a tamper.

### Scenario 4 — Turn on polymorphic builds

Apply the `dev.droidshield` plugin, then feed the generated seed in:

```kotlin
import dev.droidshield.generated.DroidShieldBuildSeed

DroidShield.init(
    context,
    config = DroidShieldConfig(polymorphicSeed = DroidShieldBuildSeed.SEED)
)
```

The seed is derived from your project path and version, so it's stable across incremental builds (no spurious recompiles) and changes when you bump the version for a release. Pin it in CI with `-PdroidshieldSeed=12345` when you need to reproduce a specific build's ordering.

Leave `polymorphicSeed` null and checks run in deterministic declaration order — which is what you want in tests.

### Scenario 5 — Run checks periodically, not just at startup

A one-shot check at launch is easy to sidestep: attach Frida after startup. Re-run on a background thread whenever it matters — app resume, before a payment, on session refresh.

```kotlin
class ThreatWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        DroidShield.init(applicationContext).runChecks()  // already off the main thread
        return Result.success()
    }
}
```

### Scenario 6 — Send operational telemetry somewhere

`TelemetrySink` is separate from `ThreatReporter` on purpose. Threat signals are security data; telemetry is "is the SDK healthy" data (`engine_run_started`, `check_executed`, `check_error`). Conflating them would force anyone wanting basic usage analytics into the threat-reporting business.

```kotlin
class PostHogSink(private val posthog: PostHog) : TelemetrySink {
    override fun capture(event: TelemetryEvent) =
        posthog.capture(event.name, event.properties)
}

DroidShield.init(context, telemetrySink = PostHogSink(posthog))
```

Default payloads carry no PII and no raw stack traces. Anything richer is your explicit opt-in.

### Scenario 7 — Add your own check

```kotlin
// droidshield-data-android/.../checks/root/MyCheck.kt
class MyCheck : ThreatCheck {
    override val id = "my_check"
    override val category = ThreatCategory.ROOT
    override val severity = Severity.HIGH

    override fun evaluate(context: CheckContext): CheckResult {
        val ctx = context as AndroidCheckContext
        val detected = /* your detection logic */
        return CheckResult(id, category, severity, detected, detail = null)
    }
}
```

Then one line in `RootChecksModule`:

```kotlin
@Provides @IntoSet
fun providesMyCheck(): ThreatCheck = MyCheck()
```

That's it. The engine picks it up, the polymorphic ordering includes it, telemetry wraps it, and a thrown exception is caught and reported as `check_error` rather than crashing the run.

---

## Modules

| Module | What's in it |
|---|---|
| `droidshield-domain` | `ThreatCheck`, `CheckResult`, `ThreatReporter`, `TelemetrySink`. Pure Kotlin, no Android imports. |
| `droidshield-data-android` | 36 checks using Android APIs — `PackageManager`, `Build`, `/proc`. Most contributions land here. |
| `droidshield-native` | C++ checks + JNI bridge → `libdroidshield.so`. |
| `droidshield-engine` | `ThreatDetectionEngine` + `CheckOrder`. Knows nothing about specific checks. |
| `droidshield-gradle-plugin` | Build-time polymorphic seed generation. |
| `droidshield-sdk` | The public `.aar` — the `DroidShield` facade and Dagger graph. |
| `sample-app` | Working end-to-end integration. |

Dagger 2 (not Hilt) — a library shouldn't force `@HiltAndroidApp` onto its integrators.
