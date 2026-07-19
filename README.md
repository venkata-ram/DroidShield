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
    implementation("com.github.venkata-ram.DroidShield:droidshield-sdk:0.4.0")
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

That's the whole runtime setup — no `resolutionStrategy`, no extra plumbing. The
Gradle plugin adds release-seeded ordering, guarded-method instrumentation, and a
release-hardening gate:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.github.venkata-ram.DroidShield") version "0.4.0"
}
```

Mark security-sensitive entry points and the plugin injects a background threat-check
trigger at the beginning of each method:

```kotlin
@DroidShieldGuarded("checkout.submit")
fun submitPayment() {
    // original application code
}
```

Injected triggers are rate-limited and never run checks on the caller thread. Release
builds also fail when they are debuggable or omit R8 minification/resource shrinking.

See **[INTEGRATION.md](docs/INTEGRATION.md)** for what the plugin does and the other integration scenarios.

## What's different about it

- **Checks at the operations that matter.** Annotate login, payment, token, or other sensitive methods with `@DroidShieldGuarded`; the Gradle plugin injects a non-blocking, rate-limited threat-check trigger into each method's bytecode.
- **Release hardening as a build invariant.** Release builds fail fast if they are debuggable or ship without R8 minification and resource shrinking. Teams can explicitly downgrade the gate when migration requires it.
- **Release-seeded check ordering.** The plugin derives a reproducible seed and the SDK picks it up automatically to shuffle check execution between releases. Feed a build-time secret (`-PdroidshieldSeedSecret` / `DROIDSHIELD_SEED_SECRET`) and the ordering can't be recomputed from the shipped APK; without one, the build warns that it can.
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

- [CONTRIBUTING.md](docs/CONTRIBUTING.md) — development setup, check design rules, tests, and pull-request expectations
- [INTEGRATION.md](docs/INTEGRATION.md) — setup, configuration, and nine usage scenarios
- [SERVER_DRIVEN_DECISIONS.md](docs/SERVER_DRIVEN_DECISIONS.md) — letting your backend, not the APK, decide how to respond to threats
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — module layering and the extensibility contract
- [DECISIONS.md](docs/DECISIONS.md) — append-only rationale log
- [SOURCES.md](docs/SOURCES.md) — citations for the 40 implemented threat checks

## An honest caveat

No client-side check is unbypassable. An attacker with full device control and enough time can defeat client-side controls. DroidShield combines multiple signals and optional release-specific ordering so integrators can make better-informed server decisions. Treat its results as evidence, not as an oracle, and validate sensitive operations on your server.
