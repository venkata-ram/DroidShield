# DroidShield

Runtime application self-protection (RASP) for Android, as a plain `.aar` you drop into your app.

DroidShield answers one question at runtime: **is this app running in an environment I should trust?** It runs 41 checks across five threat categories — root, debugger, hooking frameworks, emulator, and tamper/repackaging — and hands you the results. What you do with them is entirely your call.

---

## The problem

Once your APK is on someone else's device, you have no control over the environment it runs in. An attacker with a rooted phone can:

- **Hook your code** with Frida or Xposed/LSPosed — rewrite the return value of `isPremiumUser()`, dump your API keys mid-flight, replay requests.
- **Attach a debugger** and single-step through your auth logic.
- **Repackage your APK** — strip the license check, inject an ad SDK, re-sign it, and put it on a third-party store.
- **Run it in an emulator farm** to script fraud at scale — fake signups, referral abuse, bot traffic.

Server-side validation doesn't help here, because the client is the thing being lied to. This matters most for fintech/UPI apps, wallets, games with in-app purchases, and anything with a paid tier.

Commercial RASP vendors solve this, but they come with a backend you must send device data to, per-seat pricing, and a black box you can't audit. DroidShield is the opposite of that.

## How DroidShield solves it

**One call gives you a list of results:**

```kotlin
val droidShield = DroidShield.init(context)
val results = droidShield.runChecksSuspending()   // 41 checks, on Dispatchers.IO
```

Each check is small and independent — `SuBinaryPathCheck` looks for `su` on the PATH, `TracerPidCheck` reads `/proc/self/status`, `FridaPortCheck` probes Frida's default port, `ApkSignatureCheck` compares your signing hash to the one you configured. Each returns a `CheckResult`:

```kotlin
data class CheckResult(
    val checkId: String,
    val category: ThreatCategory,   // ROOT, DEBUGGER, HOOK, EMULATOR, TAMPER
    val severity: Severity,          // LOW, MEDIUM, HIGH, CRITICAL
    val detected: Boolean,
    val detail: String? = null
)
```

No single check is decisive — root detection alone is famously easy to defeat. The value is in the breadth and in the fact that an attacker has to defeat *all* of them, silently, on *every* build.

### What's actually unique

**1. Polymorphic builds.** This is the headline feature. A Gradle plugin generates a per-build seed at compile time; the engine uses it to shuffle check ordering (and optionally run only a subset, while guaranteeing at least one check per category). A Frida script tuned against the execution sequence in your v2.1 build doesn't cleanly transfer to v2.2. Bypasses stop being write-once.

**2. Native checks where they matter.** Anti-debug (`ptrace` self-attach), `/proc/self/maps` scanning for injected libraries, trampoline-hook detection, and native code checksumming live in C++ (`libdroidshield.so`), not Kotlin — meaningfully harder to patch out than a bytecode-level `if`.

**3. No backend. Ever.** There is no dashboard, no hosted service, no phone-home. `ThreatReporter` is a one-method interface you implement; the default just writes to logcat. Your threat signals go where *you* send them.

**4. Zero third-party dependencies in the core.** Want PostHog telemetry? Implement `TelemetrySink` yourself in four lines. DroidShield's `.aar` never pulls in an analytics SDK, so it can't bloat your app or leak your users' data.

**5. Contributing a check is a one-file job.** Clean Architecture layering means `droidshield-domain` has zero Android imports. Adding a check = one class in `droidshield-data-android` + one `@Provides @IntoSet` line. No need to read the Gradle plugin, the ASM layer, or the engine.

---

## Setup

Minimum SDK 24. JDK 17 is required to build.

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

### Consuming a local build instead

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

## Not in V1

SSL pinning / network hardening, any hosted dashboard, BFSI compliance mapping, and a Rust native layer.

## Further reading

- **[SERVER_DRIVEN_DECISIONS.md](SERVER_DRIVEN_DECISIONS.md)** — system design for letting your backend, not the APK, decide how to respond to threats
- [ARCHITECTURE.md](ARCHITECTURE.md) — module layering and the extensibility contract
- [DECISIONS.md](DECISIONS.md) — append-only rationale log

## An honest caveat

No client-side check is unbypassable. An attacker with full device control and enough time wins eventually — that's the nature of running code on hardware someone else owns. DroidShield's goal is to raise the cost: make bypasses expensive, per-build, and noisy enough that you see them in your telemetry. Treat its signals as strong evidence, not as an oracle, and keep validating on your server.
