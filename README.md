# DroidShield

Runtime threat detection for Android applications — an open-source foundation for
RASP architectures and backend-driven enforcement.

DroidShield answers one question at runtime: **is this app running in an environment I should trust?** It runs 40 checks across five threat categories — root, debugger, hooking frameworks, emulator, and tamper/repackaging — and hands you the results. What you do with them is entirely your call.

## Why

Once your APK is on someone else's device, you do not control the environment it runs in. An attacker with a rooted phone can hook your code with Frida, attach a debugger to your auth logic, repackage and re-sign your APK, or run it in an emulator farm to script fraud at scale. Server-side validation remains essential, but runtime evidence can give the server signals it cannot observe on its own.

Commercial RASP products often combine detection, prevention, enforcement, a hosted
backend, and a management dashboard. DroidShield deliberately provides the detection
and evidence layer rather than claiming that entire product surface: no hosted backend,
no automatic blocking, no bundled analytics vendor, and every check implementation
is available for inspection.

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

That's the whole runtime setup — no `resolutionStrategy`, no extra plumbing. Release-seeded check ordering additionally uses the Gradle plugin, which installs the same way:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.github.venkata-ram.DroidShield") version "0.3.1"
}
```

See **[INTEGRATION.md](INTEGRATION.md)** for what the plugin does and the other integration scenarios.

## What's different about it

- **Release-seeded check ordering.** A Gradle plugin derives a reproducible seed from the project and version; the engine uses it to shuffle check execution. This adds variation between releases and can disrupt tooling that assumes a fixed sequence, but it is not code injection or a guarantee against bypasses.
- **Native checks where they matter.** Anti-debug (`ptrace` self-attach), `/proc/self/maps` scanning, trampoline-hook detection, and native checksumming live in C++, not Kotlin. This gives attackers an additional native analysis surface; it does not make the checks unpatchable.
- **No DroidShield-hosted backend.** No dashboard, hosted service, or default phone-home behavior. `ThreatReporter` is a one-method interface you implement; your threat signals go where *you* send them.
- **No bundled analytics vendor.** Telemetry defaults to a no-op sink. The SDK does use documented runtime libraries such as Dagger, Kotlin coroutines, and AndroidX.
- **Small contribution path.** Add one check class plus one Dagger `@Provides @IntoSet` binding — no need to understand the Gradle plugin or engine internals.

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

No client-side check is unbypassable. An attacker with full device control and enough time can defeat client-side controls. DroidShield combines multiple signals and optional release-specific ordering so integrators can make better-informed server decisions. Treat its results as evidence, not as an oracle, and validate sensitive operations on your server.
