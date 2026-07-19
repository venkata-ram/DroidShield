# DroidShield — Architecture

Status: V1 scope. See `DECISIONS.md` for the append-only decision log and
`OVERVIEW.md` files inside each module for module-local detail.

## 1. Goals and non-goals for V1

**Goals**
- Detect common runtime threats (root, debugger, hooking frameworks, emulator,
  tamper/repackaging) on Android.
- Provide the detection and evidence layer that a host app can use within a broader
  RASP architecture or backend-driven enforcement system.
- Make adding a new check straightforward for an external contributor who has
  not read the ordering engine or Gradle plugin internals.
- Ship as open source: no hosted backend, bundled analytics vendor, or vendor
  lock-in. The host app decides where threat signals and optional telemetry go.
- Native (`.so`) layer for checks that are meaningfully harder to bypass in
  native code than in Java/Kotlin.
- Release-seeded ordering: the Gradle plugin generates a reproducible seed that
  the runtime engine can use to vary check execution order between versions.
  This is source generation, not host-app bytecode injection.

**Explicit non-goals for V1**
- Claiming to be a complete RASP product. Automatic prevention, enforcement,
  incident orchestration, and a management plane remain the host application's or
  backend's responsibility.
- SSL pinning / network hardening (`NetworkGuard`) — deferred, see
  `DECISIONS.md`.
- Any backend, dashboard, or hosted reporting service.
- BFSI/regulatory compliance mapping (RBI/SEBI/NPCI).
- Rust native layer (C++ only for V1).
- A specific analytics vendor integration. DroidShield ships a pluggable
  `TelemetrySink` contract (see §4a) but no PostHog/Firebase/Mixpanel
  dependency in the core `.aar`.

## 2. Layering — Clean Architecture applied to an SDK

DroidShield is a library, not an app, so "Clean Architecture" here means:
dependencies point inward, the innermost layer has zero Android/Gradle
imports, and outer layers are swappable without touching detection logic.

```
┌─────────────────────────────────────────────────────────┐
│  build-plugin        (Gradle/Kotlin source generation of  │
│                        a reproducible release seed)       │
├─────────────────────────────────────────────────────────┤
│  data                (native bridge / JNI, Android APIs:  │
│                        PackageManager, Build, /proc, etc) │
├─────────────────────────────────────────────────────────┤
│  domain              (ThreatCheck contract, CheckResult,  │
│                        ThreatCategory, Severity — pure     │
│                        Kotlin, no Android imports)         │
├─────────────────────────────────────────────────────────┤
│  presentation/api     (public SDK surface: DroidShield     │
│                        facade, ThreatReporter interface,   │
│                        Dagger entry point)                 │
└─────────────────────────────────────────────────────────┘
```

Dependency rule: `build-plugin` and `data` depend on `domain`. `domain`
depends on nothing Android-specific. `presentation/api` depends on `domain`
and wires `data` implementations into it via Dagger — it does not contain
detection logic itself.

This inversion is what lets a contributor add a check by touching only
`data` (or `domain` + `data` for something genuinely new) without reading
the Gradle plugin or ordering engine at all.

## 3. Module structure (Gradle modules)

```
droidshield/
├── droidshield-domain/          # pure Kotlin, no Android deps
├── droidshield-data-android/    # ThreatCheck implementations (Java/Kotlin side)
├── droidshield-native/          # C++ checks + JNI bridge, builds libdroidshield.so
├── droidshield-engine/          # check runner and seeded ordering
├── droidshield-gradle-plugin/   # build-seed source generation
├── droidshield-sdk/             # public runtime .aar — DroidShield facade, DI graph
└── sample-app/                  # demo app — standalone build, consumes JitPack artifacts
```

`sample-app` is not in the root build's `include(...)` list: it is its own
Gradle build that depends on the *published* `droidshield-sdk`, so it
exercises the real integration path rather than a project dependency
(DECISIONS.md D032). `droidshield-gradle-plugin` is likewise a separate
included build (D027).

### droidshield-domain
Pure Kotlin module. Contains:
- `ThreatCheck` interface
- `CheckResult`, `Severity`, `ThreatCategory` (sealed/enum types)
- `ThreatReporter` interface
- No Android SDK imports. This is what makes the contract testable on the
  JVM without an emulator, and what contributors read first.

### droidshield-data-android
Concrete `ThreatCheck` implementations that need Android APIs (`PackageManager`,
`Build`, `/proc` filesystem reads, `Debug.isDebuggerConnected()`, etc). Each
check is one small class. This is the module most contributors touch.

### droidshield-native
C++ checks that are meaningfully harder to defeat in native code: ptrace-based
anti-debug, `/proc/self/maps` scanning for Frida/Xposed signatures, native
signature/checksum verification. Exposes results to Kotlin via JNI. Kept
separate from `data-android` because it has its own build toolchain (CMake/
NDK) and its own contribution bar (C++ + security background).

### droidshield-engine
The check runner and seeded ordering implementation. This module does **not**
know about specific checks — it operates on the `ThreatCheck` contract only.
This is intentionally the smallest, most stable module: it should rarely need
to change when checks are added.

### droidshield-gradle-plugin
Generates a Kotlin source file containing a reproducible seed derived from the
consumer project and version. The host app passes that seed to `DroidShieldConfig`
to enable runtime ordering variation. It does not rewrite host-app bytecode.

### droidshield-sdk
The public `.aar`. Contains the `DroidShield` facade (the only class most
integrators ever import), the Dagger 2 dependency graph, and the wiring
that connects `data-android` + `native` implementations to the `engine` at
runtime. `ThreatReporter` is configured here.

## 4. The extensibility contract (domain layer)

```kotlin
// droidshield-domain

enum class ThreatCategory { ROOT, DEBUGGER, HOOK, EMULATOR, TAMPER }

enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }

data class CheckResult(
    val checkId: String,
    val category: ThreatCategory,
    val severity: Severity,
    val detected: Boolean,
    val detail: String? = null
)

interface ThreatCheck {
    val id: String
    val category: ThreatCategory
    val severity: Severity
    fun evaluate(context: CheckContext): CheckResult
}

interface ThreatReporter {
    fun onThreatDetected(result: CheckResult)
}
```

`ThreatCategory` is deliberately small and stable for V1 (5 values). Renaming
or restructuring this enum after checks depend on it is a breaking change for
every contributor — see `DECISIONS.md` before touching it. New categories are
additive (append, don't rename/remove).

## 4a. Telemetry — a separate contract from ThreatReporter

`ThreatReporter` answers "a threat was detected." Telemetry answers a
different question: "is the SDK running, how are checks performing, is
anything erroring" — operational/usage data, not security signals.
Conflating the two would force every integrator who wants basic usage
analytics into the threat-reporting business as well, so they stay
separate interfaces:

```kotlin
// droidshield-domain

data class TelemetryEvent(
    val name: String,
    val properties: Map<String, Any?> = emptyMap()
)

interface TelemetrySink {
    fun capture(event: TelemetryEvent)
}
```

Candidate events the engine/registry might emit (illustrative, not
exhaustive): `check_executed` (checkId, durationMs, category),
`check_error` (checkId, errorType — never raw stack traces or device
identifiers by default), `engine_initialized` (checkCount, buildVariantSeed).
Keep event payloads minimal and free of PII by default; anything more
detailed is the integrator's opt-in choice via their own `TelemetrySink`
implementation, not DroidShield's default behavior.

Like `ThreatReporter`, this is wired as a Dagger-provided singleton with a
no-op default:

```kotlin
@Module
class TelemetryModule(private val sink: TelemetrySink = NoOpTelemetrySink()) {
    @Provides @Singleton
    fun providesTelemetrySink(): TelemetrySink = sink
}
```

An integrator wanting PostHog (or Firebase, Mixpanel, Amplitude, or an
internal system) implements the one-method interface themselves:

```kotlin
class PostHogTelemetrySink(private val posthog: PostHog) : TelemetrySink {
    override fun capture(event: TelemetryEvent) {
        posthog.capture(event.name, event.properties)
    }
}

DroidShield.init(context, telemetrySink = PostHogTelemetrySink(posthogInstance))
```

DroidShield's core `.aar` never depends on PostHog's SDK (or anyone else's)
— zero-dependency by default, matching the "no backend assumed" principle
for `ThreatReporter`. The sample app can include a PostHog wiring example
in its integration guide without the core library gaining the dependency.

### Adding a new check (the contributor path)

1. Copy the template in `droidshield-data-android/checks/TEMPLATE.kt` into the
   right category package, e.g. `checks/root/MyNewCheck.kt`.
2. Implement `evaluate()`.
3. Register it (see §5 — registration mechanism).
4. Add a unit test next to it.
5. Open a PR. No Gradle plugin, ASM, or engine knowledge required.

## 5. Registration mechanism

**Decision needed before scaling past a handful of checks** (flag this in
`DECISIONS.md` when resolved). Three options, trade-offs below:

| Approach | Contributor effort | Build-time cost | Risk |
|---|---|---|---|
| Manual registry list | Add one line to a `RegistryModule.kt` list | None | Merge conflicts on the list; easy to forget |
| Annotation + KSP/kapt processor | Add `@ThreatCheckProvider` to the class | Moderate (codegen step) | More build-tool complexity for contributors to debug |
| `ServiceLoader` / manifest-based | Add class name to a resource manifest file | Low | Manifest file is a second thing to remember to edit |

**Recommendation for V1**: manual registry list via a Dagger `@IntoSet`
multibinding (see §6) — lowest tooling complexity, plain Kotlin, and Dagger
already gives you the "collect all instances of type X" mechanism for free
without introducing annotation processing on top of Dagger's own.

## 6. Dependency injection — Dagger 2

**Why Dagger 2 over Hilt**: this is a library consumed by third-party apps,
not an app module itself. Hilt assumes it owns the `Application` class and
the full DI graph; a library that forces Hilt onto every integrator is an
aggressive requirement. Plain Dagger 2 lets DroidShield own an internal
graph without imposing anything on the host app's DI choice (Hilt, Koin,
manual, or none).

### Component structure

```kotlin
@Singleton
@Component(modules = [ChecksModule::class, ReportingModule::class])
interface DroidShieldComponent {
    fun engine(): ThreatDetectionEngine

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): DroidShieldComponent
    }
}
```

### Multibinding for checks (`@IntoSet`)

This is the Dagger mechanism that backs the "registration" decision in §5.
Each check module contributes to one `Set<ThreatCheck>` without any check
knowing about any other check, and without a central list to edit for the
*consumer* side (the engine just asks for `Set<ThreatCheck>`).

```kotlin
@Module
object RootChecksModule {
    @Provides @IntoSet
    fun providesSuBinaryCheck(): ThreatCheck = SuBinaryPathCheck()

    @Provides @IntoSet
    fun providesSuPackageCheck(): ThreatCheck = SuPackageCheck()
    // ... one @Provides @IntoSet per check
}

@Module(includes = [RootChecksModule::class, DebuggerChecksModule::class,
                    HookChecksModule::class, EmulatorChecksModule::class,
                    TamperChecksModule::class])
object ChecksModule {
    // aggregates all category modules into one Set<ThreatCheck>
}
```

Contributors still add one `@Provides @IntoSet` line per new check (this is
the "manual registry" cost from §5) — but they add it to their own category
module, not a shared cross-cutting file, which keeps merge conflicts rare.

### Reporting

```kotlin
@Module
class ReportingModule(private val reporter: ThreatReporter) {
    @Provides @Singleton
    fun providesReporter(): ThreatReporter = reporter
}
```

Default is `LogcatThreatReporter` if the integrator supplies nothing —
zero-setup path for evaluation/testing, matching the "no backend assumed"
principle in `DECISIONS.md`.

### Why not Hilt, why not Koin
- **Hilt**: forces `@HiltAndroidApp`/`@AndroidEntryPoint` onto the host app.
  Unacceptable for a library aimed at arbitrary third-party integrators.
- **Koin**: service-locator style resolves at runtime via reflection-lite;
  Dagger's compile-time graph validation catches missing bindings for a new
  check at build time, which matters more for a security SDK than Koin's
  faster iteration speed.

## 7. Android SDK best-practice notes for V1

- Minimum API level: match AGP Instrumentation API's own floor (verify
  against current AGP docs at implementation time — this shifts across AGP
  versions).
- No `Activity`/`Fragment` dependencies anywhere in the SDK — this is a
  headless library; nothing about it should require lifecycle awareness in
  V1 (no UI, no dialogs — that's the host app's decision to make with the
  `ThreatReporter` callback).
- Native library packaging: ship `.so` per-ABI (`arm64-v8a`, `armeabi-v7a`
  minimum; `x86_64` for emulator/CI testing only, documented as such).
- ProGuard/R8: ship consumer ProGuard rules with the `.aar` so integrators
  don't have to guess which DroidShield classes must survive minification —
  and so obfuscation of the host app doesn't accidentally break check
  reflection if any check relies on it.
- Thread discipline: `evaluate()` calls must not do disk/network I/O on the
  calling thread if invoked from a lifecycle callback — document this
  constraint in the `ThreatCheck` KDoc, since contributors won't all default
  to it correctly.

## 8. What "V1 done" looks like

See `DECISIONS.md` for the full V1 exit criteria. Architecturally: all five
`ThreatCategory` values have real, working checks wired through the Dagger
multibinding graph, the engine can apply reproducible seeded ordering, the
native `.so` covers root+debugger+hook at
minimum, and a fresh contributor can add one new check by touching only
`droidshield-data-android` and reading nothing else.
