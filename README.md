# DroidShield

Runtime application self-protection (RASP) for Android.

DroidShield answers one question at runtime: **is this app running in an environment I should trust?** It runs 40 checks across five threat categories — root, debugger, hooking frameworks, emulator, and tamper/repackaging — and hands you the results. What you do with them is entirely your call.

## Why

Once your APK is on someone else's device, you have no control over the environment it runs in. An attacker with a rooted phone can hook your code with Frida, attach a debugger to your auth logic, repackage and re-sign your APK, or run it in an emulator farm to script fraud at scale. Server-side validation doesn't help, because the client is the thing being lied to.

Commercial RASP vendors solve this with a backend you must send device data to, per-seat pricing, and a black box you can't audit. DroidShield is the opposite: no backend, no third-party dependencies in the core, and every check readable in one file.

## Quick start

DroidShield is distributed through **JitPack**, which builds it straight from a Git tag. Add the repository:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then depend on the SDK — `droidshield-sdk` pulls in the domain, check, native and engine modules transitively, so this is the only coordinate you declare:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.venkata-ram.DroidShield:droidshield-sdk:0.3.1")
}
```

```kotlin
val shield = DroidShield.init(context)
val results = shield.runChecksSuspending()   // 40 checks, on Dispatchers.IO
```

Each check returns a `CheckResult`:

```kotlin
data class CheckResult(
    val checkId: String,
    val category: ThreatCategory,   // ROOT, DEBUGGER, HOOK, EMULATOR, TAMPER
    val severity: Severity,         // LOW, MEDIUM, HIGH, CRITICAL
    val detected: Boolean,
    val detail: String? = null
)
```

That's the whole runtime setup — no `resolutionStrategy`, no extra plumbing. Polymorphic builds additionally want the Gradle plugin, which installs the same way:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.github.venkata-ram.DroidShield") version "0.3.1"
}
```

See **[INTEGRATION.md](INTEGRATION.md)** for what the plugin does and the other integration scenarios.

## What's different about it

- **Polymorphic builds.** A Gradle plugin generates a per-build seed at compile time; the engine uses it to shuffle check ordering. A Frida script tuned against your v2.1 build doesn't cleanly transfer to v2.2. Bypasses stop being write-once.
- **Native checks where they matter.** Anti-debug (`ptrace` self-attach), `/proc/self/maps` scanning, trampoline-hook detection, and native checksumming live in C++, not Kotlin — harder to patch out than a bytecode-level `if`.
- **No backend. Ever.** No dashboard, no hosted service, no phone-home. `ThreatReporter` is a one-method interface you implement; your threat signals go where *you* send them.
- **Zero third-party dependencies in the core.** The published artifact never pulls in an analytics SDK, so it can't bloat your app or leak your users' data.
- **Contributing a check is a one-file job.** One class plus one `@Provides @IntoSet` line — no need to read the Gradle plugin, the ASM layer, or the engine.

DroidShield is headless: it never shows a dialog or kills your process. It reports, you decide.

## Sample app

[`sample-app/`](sample-app) is a standalone Gradle build that consumes DroidShield from JitPack exactly as an integrator would — no project dependencies:

```bash
cd sample-app && ../gradlew assembleDebug
```

## Documentation

- [CONTRIBUTING.md](CONTRIBUTING.md) — development setup, check design rules, tests, and pull-request expectations
- [INTEGRATION.md](INTEGRATION.md) — setup, configuration, and eight usage scenarios
- [SERVER_DRIVEN_DECISIONS.md](SERVER_DRIVEN_DECISIONS.md) — letting your backend, not the APK, decide how to respond to threats
- [ARCHITECTURE.md](ARCHITECTURE.md) — module layering and the extensibility contract
- [DECISIONS.md](DECISIONS.md) — append-only rationale log

## An honest caveat

No client-side check is unbypassable. An attacker with full device control and enough time wins eventually — that's the nature of running code on hardware someone else owns. DroidShield's goal is to raise the cost: make bypasses expensive, per-build, and noisy enough that you see them in your telemetry. Treat its signals as strong evidence, not as an oracle, and keep validating on your server.
