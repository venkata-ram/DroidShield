# DroidShield — Decision log

Append-only. Never edit or delete a past entry — if a decision is reversed,
add a new entry that supersedes it and link back (`Supersedes: D00X`). This
is what lets a contributor trust that a past entry still reflects what was
actually decided at the time, even after the project moves past it.

Entry format: **Status** is one of `Decided` (settled for V1, low chance of
change), `Revisitable` (settled for V1 but explicitly flagged as likely to
change once a stated trigger condition is hit), or `Open` (not yet decided —
tracked here so it isn't silently assumed).

---

## D001 — No backend, no bundled telemetry vendor

**Date:** 2026-07-19
**Status:** Decided

**Decision:** DroidShield ships as a pure client-side library. No hosted
backend, no dashboard, no bundled analytics SDK (PostHog/Firebase/Mixpanel/
etc.) in the core `.aar`. `ThreatReporter` and `TelemetrySink` are the only
integration points, both pluggable, both defaulting to no-op-or-local
implementations.

**Reasoning:** This is an open-source project with one primary maintainer
and volunteer contributors — no paid team, no sales motion. A backend
implies ongoing hosting cost and a support burden that doesn't fit that
model. It also respects integrators' own compliance/vendor constraints:
forcing a specific analytics vendor onto every consumer of a security SDK
is an aggressive requirement none of them asked for.

**Alternatives considered:** Ship a default hosted reporting endpoint for
zero-config evaluation. Rejected — implies a backend to operate and secure,
which contradicts the "no paid team" constraint and turns a security tool
into a data-collection liability.

---

## D002 — Native (`.so`) layer only where it's meaningfully harder to bypass

**Date:** 2026-07-19
**Status:** Decided

**Decision:** Not every check moves to native code. Only checks where
native genuinely raises the bypass bar (ptrace-based anti-debug,
`/proc/self/maps` scanning, native signature/checksum verification) live in
`droidshield-native`. Everything else stays in `droidshield-data-android`.

**Reasoning:** Native code has a real cost: its own toolchain (CMake/NDK),
its own contributor skill bar (C++ + security background), and slower
iteration. Moving a check like "read `Build.TAGS`" to native buys no extra
resistance to bypass — it's a `PackageManager`/`Build` field read either
way — so paying the native cost for it would be pure overhead with no
security benefit.

**Alternatives considered:** All checks native, for a uniform "everything
is hard to reverse" story. Rejected — inflates the contributor bar for the
majority of checks that gain nothing from it, directly working against the
extensibility goal.

---

## D003 — C++ only for V1 native layer, no Rust

**Date:** 2026-07-19
**Status:** Revisitable

**Decision:** `droidshield-native` is C++ (NDK/CMake) only for V1. Rust is
explicitly out of scope.

**Reasoning:** Adding a second native toolchain (Rust + cargo-ndk + FFI
bridging) on top of C++ doubles the native contribution bar and CI/build
surface for a V1 whose native footprint is deliberately small (root +
debugger + hook checks, per §8 of `ARCHITECTURE.md`). One native toolchain
is enough complexity to introduce at once.

**Alternatives considered:** Rust for new native checks going forward
(memory-safety argument). Rejected for V1 specifically because it fragments
a small native surface across two toolchains for marginal benefit at this
scale.

**Revisit trigger:** If the native check surface grows large enough that
memory-safety bugs in C++ checks become a recurring source of crashes/CVEs
in the SDK itself, revisit — Rust's safety guarantees would matter more at
that scale.

---

## D004 — SSL pinning / network hardening (`NetworkGuard`) deferred

**Date:** 2026-07-19
**Status:** Revisitable

**Decision:** Out of scope for V1. Tracked as a future, separately-named
component (`NetworkGuard`), not a `ThreatCategory`.

**Reasoning:** SSL pinning is a *prevention* mechanism (it changes network
behavior), not a *detection* check — it doesn't fit the `ThreatCheck`
contract (`evaluate() -> CheckResult`) at all. Folding it into the existing
five detection categories would force a square peg into a round hole and
was explicitly flagged in the Stage 6 self-check (`COT_PROMPT.md`) as
needing a different contract shape.

**Alternatives considered:** Add it as a sixth `ThreatCategory` now.
Rejected — `ThreatCategory` is deliberately small and stable (see D008);
a prevention mechanism doesn't belong in an enum of detection categories
regardless of scheduling.

**Revisit trigger:** When `NetworkGuard` is designed, it gets its own
contract (something like a `NetworkPolicy` interface), not a bent version
of `ThreatCheck`.

---

## D005 — No BFSI/regulatory compliance mapping in V1

**Date:** 2026-07-19
**Status:** Decided

**Decision:** DroidShield does not map its checks to RBI/SEBI/NPCI or other
regulatory frameworks in V1.

**Reasoning:** Compliance mapping is a claims/liability surface (asserting
"this satisfies regulation X" is a legal statement, not a technical one)
that doesn't belong in a community-maintained OSS project with no legal
review process. It's also orthogonal to the core detection-engine work.

**Alternatives considered:** None seriously considered — this was ruled out
at the goals-restatement stage as out of scope for a volunteer-maintained
OSS SDK.

---

## D006 — Dagger 2 over Hilt

**Date:** 2026-07-19
**Status:** Decided

**Decision:** Dependency injection uses plain Dagger 2, not Hilt.

**Reasoning:** DroidShield is a library consumed by third-party host apps,
not an app module itself. Hilt assumes it owns the `Application` class
(`@HiltAndroidApp`) and the full DI graph. Forcing that onto every
integrator — who may already use Hilt, Koin, manual DI, or nothing — is an
aggressive, unacceptable requirement for a library.

**Alternatives considered:** Hilt (rejected, above). No DI at all / manual
wiring (rejected — loses compile-time graph validation for the
multibinding-based check registry, see D009).

---

## D007 — Dagger 2 over Koin

**Date:** 2026-07-19
**Status:** Decided

**Decision:** Dagger 2, not Koin, for the internal DI graph.

**Reasoning:** Koin resolves bindings at runtime via reflection-lite
service location; a missing or misconfigured binding surfaces as a runtime
crash. Dagger validates the graph at compile time. For a security SDK,
catching "a new check's binding is broken" at build time rather than at
runtime on a user's device matters more than Koin's faster iteration
speed.

**Alternatives considered:** Koin (rejected, above).

---

## D008 — `ThreatCategory` is small, stable, and append-only

**Date:** 2026-07-19
**Status:** Decided

**Decision:** `ThreatCategory` ships with 5 values for V1 (`ROOT`,
`DEBUGGER`, `HOOK`, `EMULATOR`, `TAMPER`). New categories may be **added**;
existing values may not be renamed or removed without treating it as a
breaking change logged here first.

**Reasoning:** Every `ThreatCheck` implementation across every contributor's
fork/PR depends on this enum. Renaming or removing a value breaks
compilation for every existing check, which is exactly the kind of
surprise cost the extensibility goal is designed to avoid. Additive changes
are safe; destructive ones are not.

**Alternatives considered:** Open string-based category (`String` instead
of `enum`) for maximum flexibility. Rejected — loses compile-time
exhaustiveness checking in `when` blocks across the engine and reporting
code, which is more valuable than the flexibility for a set of categories
that changes rarely.

---

## D009 — Check registration: manual registry via Dagger `@IntoSet`

**Date:** 2026-07-19
**Status:** Revisitable

**Decision:** New checks register via one `@Provides @IntoSet` line in
their own category's Dagger module (e.g. `RootChecksModule`). No annotation
processor, no `ServiceLoader`/manifest file.

**Reasoning:** Three options were weighed (see `ARCHITECTURE.md` §5):
manual list, annotation processor (KSP/kapt), `ServiceLoader`/manifest.
Manual `@IntoSet` wins for V1 because (a) Dagger is already a hard
dependency for the DI graph, so this adds zero new tooling — no extra
annotation processor to configure or debug; (b) the one line a contributor
adds goes into their own category module, not a shared cross-cutting file,
so merge conflicts across concurrent PRs stay rare; (c) it keeps the
compile-time graph validation from D007 for the new check's binding too.

**Alternatives considered:**
- Annotation processor (`@ThreatCheckProvider` + KSP/kapt codegen) —
  rejected for V1: adds build-tool complexity contributors have to debug
  when codegen fails, for a benefit (less boilerplate) that doesn't matter
  much at V1's check count.
- `ServiceLoader`/manifest file — rejected: trades one "remember to edit
  file X" cost (the Dagger module) for a different one (the manifest),
  without a clear win, and loses Dagger's compile-time validation.

**Revisit trigger:** Explicitly flagged in `ARCHITECTURE.md` §5 as
"decision needed before scaling past a handful of checks" — if the check
count grows large enough that manual `@IntoSet` lines become genuinely
tedious or error-prone, revisit toward the annotation-processor option.

---

## D010 — `TelemetrySink` is a separate contract from `ThreatReporter`

**Date:** 2026-07-19
**Status:** Decided

**Decision:** Two interfaces, two Dagger modules, two independent
integrator choices: `ThreatReporter.onThreatDetected(CheckResult)` for
security signals, `TelemetrySink.capture(TelemetryEvent)` for operational/
usage data (check latency, error counts, engine init). Never merged into
one interface or one module.

**Reasoning:** These answer different questions — "a threat was detected"
vs. "is the SDK healthy and running" — for different audiences and
different opt-in decisions. An integrator who wants basic usage analytics
(e.g. via PostHog) shouldn't be forced into also handling security-threat
callbacks through the same interface, and vice versa. Conflating them
would also mean a `TelemetrySink` implementation accidentally receiving
security-sensitive `CheckResult` data it was never meant to see.

**Alternatives considered:** One `Callback` interface with an event-type
discriminator. Rejected — collapses two distinct integrator decisions
(do I want threat reporting? do I want usage telemetry?) into one binding,
and risks security data leaking into a telemetry pipeline by accident.

---

## D011 — Default implementations: `LogcatThreatReporter`, `NoOpTelemetrySink`

**Date:** 2026-07-19
**Status:** Decided

**Decision:** If an integrator supplies no `ThreatReporter`, DroidShield
defaults to logging to Logcat. If no `TelemetrySink` is supplied, it
defaults to a no-op.

**Reasoning:** Zero-setup evaluation path — `DroidShield.init(context)`
with no arguments should still do something visible (Logcat) for the
security signal, matching the "no backend assumed" principle. Telemetry
defaults to silent no-op rather than Logcat because operational event spam
in Logcat by default would be noise nobody asked for; security threats are
worth surfacing by default, routine `check_executed` events are not.

**Alternatives considered:** No-op default for both. Rejected for
`ThreatReporter` specifically — a security SDK that silently does nothing
when misconfigured is worse than one that's noisy in Logcat until the
integrator wires a real reporter.

---

## D012 — Minimum API level not yet pinned

**Date:** 2026-07-19
**Status:** Open

**Decision:** Not yet decided. `ARCHITECTURE.md` §7 states it should match
the AGP Instrumentation API's own floor, verified against current AGP docs
at implementation time.

**Reasoning:** This wasn't settled because it depends on which AGP version
the Gradle plugin module targets, which itself hasn't been pinned yet (no
`build.gradle.kts` exists in the repo as of this entry). Pinning it now
would be guessing.

**Alternatives considered:** N/A — flagged as open rather than guessed, per
the "state uncertainty rather than silently assume" instruction in
`COT_PROMPT.md` Stage 1.

**Resolve when:** The Gradle scaffold is created and an AGP version is
chosen (see `droidshield-gradle-plugin` setup).

---

## D013 — Native ABI packaging: `arm64-v8a` + `armeabi-v7a` minimum

**Date:** 2026-07-19
**Status:** Decided

**Decision:** Ship `.so` for `arm64-v8a` and `armeabi-v7a` as the minimum
supported ABIs. `x86_64` is built and shipped too, but documented as
emulator/CI-testing-only, not a production target.

**Reasoning:** These two ARM ABIs cover the overwhelming majority of real
Android devices in the field. `x86_64` devices are effectively emulators
(and some Chromebooks); shipping it unlabeled as "supported" would imply a
production guarantee DroidShield can't meaningfully make for anti-emulator
checks running on emulator-class hardware — that would be actively
confusing for a check category that includes emulator *detection*.

**Alternatives considered:** Ship `x86_64` as a fully supported production
ABI. Rejected as misleading given the emulator-detection category's
existence.

---

## D014 — Ship consumer ProGuard/R8 rules with the `.aar`

**Date:** 2026-07-19
**Status:** Decided

**Decision:** DroidShield's `.aar` bundles consumer ProGuard rules so
integrators don't have to hand-write keep rules for DroidShield's own
classes.

**Reasoning:** A security SDK that breaks silently under the host app's
R8/ProGuard config (and only surfaces as a crash or a check that always
returns a false result) is worse than useless — it gives a false sense of
protection. Shipping the rules with the `.aar` is the standard Android
library practice for exactly this failure mode.

**Alternatives considered:** Document required keep rules in the README
and let integrators add them manually. Rejected — too easy to skip or get
wrong, and the failure mode (checks silently misbehaving) is severe enough
to not leave to chance.

---

## D015 — No `Activity`/`Fragment` dependencies; headless library

**Date:** 2026-07-19
**Status:** Decided

**Decision:** Nothing in DroidShield requires lifecycle awareness in V1. No
UI, no dialogs. The `ThreatReporter` callback is the only surface — what to
do with a detected threat (alert, block, wipe) is entirely the host app's
decision.

**Reasoning:** A library that assumes it can show a dialog or needs a
`Fragment`/`Activity` context forces integration assumptions onto host apps
that may run checks from a `Service`, a `WorkManager` job, or anywhere else
with no UI context available. Keeping it headless maximizes where it can be
called from.

**Alternatives considered:** Ship an optional default "threat detected"
UI (blocking dialog/screen) integrators can opt into. Rejected for V1 as
scope creep beyond the detection-engine goal — can be a separate optional
module later without touching the core contract.

---

## D016 — `evaluate()` must not block on I/O when called from a lifecycle callback

**Date:** 2026-07-19
**Status:** Decided

**Decision:** `ThreatCheck.evaluate()` implementations must not perform
blocking disk/network I/O on the calling thread if the engine is invoked
from a lifecycle callback (e.g. `Application.onCreate`). This is documented
in the `ThreatCheck` KDoc rather than enforced by the type system.

**Reasoning:** Many real checks legitimately need I/O (`/proc` reads, file
existence checks) — banning it outright at the contract level would break
half the seed check list in `CHECKS_SEED_LIST.md`. But contributors won't
all default to running these off the main thread correctly, and a slow
check on `Application.onCreate` is an ANR risk. Documenting the constraint
is the pragmatic middle ground given the contract can't enforce threading
at compile time without adding suspend-function complexity that the
Stage 3 constraint ("implementable by someone who's never seen the rest of
the codebase") argues against.

**Alternatives considered:** Make `evaluate()` a `suspend fun` to force
async-safe usage. Deferred, not rejected outright — flagged here as worth
revisiting once the engine's execution model (§ engine, not yet
implemented) is built, since it changes the contract shape from §4 and
would need its own decision entry when that module is scaffolded.

---

## D017 — Toolchain versions pinned for initial Gradle scaffold

**Date:** 2026-07-19
**Status:** Revisitable

**Decision:** Kotlin 2.0.21, AGP 8.7.2, Dagger 2.52, Gradle version catalog
(`gradle/libs.versions.toml`) as the single source of truth for all module
`build.gradle.kts` files.

**Reasoning:** Kotlin 2.0.x is the current stable K2-compiler line, needed
since `droidshield-gradle-plugin` will itself be a Gradle plugin (best
built against a modern, actively-supported Kotlin/Gradle combination).
AGP 8.7.2 is a recent stable release with a mature Instrumentation API
(the mechanism `droidshield-gradle-plugin` depends on per ARCHITECTURE.md
§3). Dagger 2.52 is current stable and matches the multibinding
(`@IntoSet`) usage from D009. A version catalog (rather than per-module
hardcoded versions) was chosen so a future version bump touches one file,
not seven `build.gradle.kts` files.

**Alternatives considered:** Pin to older LTS-style versions for maximum
compatibility. Rejected — this is a new project with no existing user base
to preserve compatibility for; starting on current stable tooling avoids
an immediate "upgrade the whole toolchain" chore.

**Revisit trigger:** Standard dependency-bump cadence; also revisit
immediately if a chosen version turns out to have a known issue with the
AGP Instrumentation API specifically (this combination hasn't been build-
verified yet in this environment — see the "Not yet build-verified" note
in D012's resolution below).

---

## D018 — `CheckContext` is an empty marker interface in `domain`, not `android.content.Context`

**Date:** 2026-07-19
**Status:** Decided

**Decision:** `dev.droidshield.domain.CheckContext` is an empty marker
interface. The concrete Android-backed implementation
(`AndroidCheckContext`, wrapping `android.content.Context`) lives in
`droidshield-data-android`, which is where actual checks are written and
where an Android `Context` import is legal.

**Reasoning:** `ARCHITECTURE.md` §4 defines `ThreatCheck.evaluate(context:
CheckContext)` in the domain layer, but domain has zero Android imports by
rule (§2) — so `CheckContext` cannot literally be `android.content.Context`
there. Since every real `ThreatCheck` implementation lives in
`droidshield-data-android` (which already depends on the Android SDK), the
concrete wrapper belongs there, not in domain. This preserves the
"domain has zero Android imports" invariant that makes the contract
JVM-testable (see `droidshield-domain/OVERVIEW.md`) while still giving
Android-layer checks what they actually need.

**Alternatives considered:** Make `evaluate()` generic
(`evaluate(context: T)`) so each layer supplies its own context type.
Rejected — genericizing the core contract method makes the multibinding
`Set<ThreatCheck>` in D009 harder to express cleanly (a `Set` of a generic
interface with varying type parameters doesn't collect the way `@IntoSet`
needs it to), for a problem the marker-interface approach already solves
simply.

---

## D019 — Placeholder group ID: `dev.droidshield`

**Date:** 2026-07-19
**Status:** Open

**Decision:** Package namespace and eventual Maven `groupId` set to
`dev.droidshield` as a placeholder throughout the initial scaffold.

**Reasoning:** Some namespace was needed to write real, compiling Kotlin
package declarations rather than leaving files package-less. `dev.droidshield`
was chosen only because it's short and matches the project name — no
domain/GitHub org/Maven Central account has actually been secured under
this name.

**Alternatives considered:** None meaningfully — this is a placeholder, not
a considered choice.

**Resolve when:** Before any real publishing (Maven Central / JitPack /
GitHub Packages) happens, the maintainer needs to confirm the actual
intended package namespace and update it repo-wide in one pass (it's a
find-replace, not a re-architecture, as long as it's done before an
external contributor starts depending on the current namespace).

---

## D020 — Resolution of D012: min/compile/target SDK

**Date:** 2026-07-19
**Status:** Decided
**Resolves:** D012

**Decision:** `compileSdk = 35`, `targetSdk = 35`, `minSdk = 24` (Android 7.0)
for all Android-facing modules (`droidshield-data-android`,
`droidshield-native`, `droidshield-sdk`, `sample-app`).

**Reasoning:** AGP 8.7.2's Instrumentation API has no meaningful floor
above API 21, so the constraint isn't the tooling — it's a product
judgment call. API 24 was chosen as a pragmatic floor for a *security* SDK:
it's old enough to cover the large majority of active devices, but recent
enough that several of the seed checks (`CHECKS_SEED_LIST.md`) — package
visibility behavior, some `/proc` access patterns — behave more
predictably than on very old API levels, reducing false-positive/false-
negative variance across the check set. `compileSdk`/`targetSdk` track the
latest stable API level at time of writing since there's no legacy
`targetSdk` constraint on a brand-new project.

**Alternatives considered:** minSdk 21 (broadest reach). Rejected — pushes
support burden onto pre-Android-7 devices that make up a shrinking, now
small fraction of the install base, for a security library where check
correctness matters more than maximizing floor reach.

**Note:** This combination has not yet been build-verified in this
environment (no network access to download the Gradle/AGP distribution
during this session) — flagged honestly rather than claimed as tested.

---

## D021 — Build requires JDK 17 as Gradle's own runtime, not just as a target

**Date:** 2026-07-19
**Status:** Decided

**Decision:** Contributors must run Gradle itself under JDK 17 (e.g. via
`JAVA_HOME` or a `gradle.properties` `org.gradle.java.home` pointing at a
17 install), not merely set `jvmToolchain(17)` in build scripts. A
`gradlew` wrapper should be added once network access is available to
generate one, so this is enforced automatically instead of relying on each
contributor's ambient `JAVA_HOME`.

**Reasoning:** Verified directly in this environment: with the ambient
`JAVA_HOME` pointed at JDK 26, `gradle :droidshield-domain:test` failed —
the Kotlin 2.0.21 compiler daemon couldn't parse the JDK's own version
string (`26.0.1`), an internal compiler crash, independent of the
`jvmToolchain(17)` setting in `droidshield-domain/build.gradle.kts`. That
toolchain setting controls what JDK *compiled code targets*; it does not
control what JDK *Gradle and the Kotlin daemon run on*. Re-running with
`JAVA_HOME` pointed at a JDK 17 install fixed it — `compileKotlin` and
`test` both succeeded (`droidshield-domain` module, 2026-07-19).

**Alternatives considered:** Wait for a newer Kotlin release with JDK 26
support and drop this constraint. Rejected for now — no such release is
confirmed available, and blocking the whole scaffold on it isn't
worthwhile when pointing `JAVA_HOME` at JDK 17 is a one-line fix.

**Action item:** Add a `gradlew`/`gradlew.bat` wrapper (needs network
access to fetch the distribution) so this isn't a tribal-knowledge
requirement — track as a follow-up, not yet done in this pass.

**Resolution (2026-07-19):** Wrapper added (`gradlew`, `gradlew.bat`,
`gradle/wrapper/`), pinned to **Gradle 8.9** — not the ambient system
Gradle 9.2.1. Reason: running `droidshield-gradle-plugin` (which applies
`java-gradle-plugin` and therefore compiles against Gradle's own API jar)
under system Gradle 9.2.1 failed with a Kotlin metadata version mismatch —
Gradle 9.2.1's `gradle-api` jar and bundled `kotlin-reflect`/`kotlin-stdlib`
are compiled with Kotlin 2.2.x metadata, incompatible with this project's
pinned Kotlin 2.0.21 compiler (D017). Gradle 8.9 was already cached
locally and is a well-supported match for AGP 8.7.2 (D017) and Kotlin
2.0.21, and resolved the failure with no other change. With the wrapper
in place and `JAVA_HOME` pointed at JDK 17, `./gradlew build` succeeds for
all 7 modules — verified directly in this environment (2026-07-19, "BUILD
SUCCESSFUL", 330 tasks, includes `sample-app:assemble` producing both
debug and release APKs).

---

## D022 — `droidshield-engine` is a pure Kotlin JVM module, not an Android library

**Date:** 2026-07-19
**Status:** Decided

**Decision:** `droidshield-engine` uses the same `kotlin.jvm` plugin as
`droidshield-domain`, not `com.android.library`.

**Reasoning:** `ARCHITECTURE.md` §3 is explicit that the engine "does not
know about specific checks — it operates on the `ThreatCheck` contract
only," and §2's dependency rule has `data`/`build-plugin` depend on
`domain`, not the reverse. Since the engine only needs `Set<ThreatCheck>`,
ordering/shuffling logic, and the domain contract types — none of which
touch Android APIs — there's no reason for it to carry an Android
dependency. Keeping it a plain JVM module keeps it JVM-testable (same
benefit as domain, see `droidshield-domain/OVERVIEW.md`) and physically
prevents a future contributor from accidentally reaching for a
`PackageManager` call inside engine code, which would violate the "engine
never knows about specific checks" invariant.

**Alternatives considered:** Android library module for consistency with
the other runtime modules (`data-android`, `sdk`). Rejected — "consistent
module type" isn't a real benefit; preventing an accidental Android
dependency creeping into the one module that's supposed to stay
check-agnostic is a real one.

---

## D023 — Stub modules for `data-android`, `native`, `gradle-plugin`, `sdk`, `sample-app` contain no detection/engine logic yet

**Date:** 2026-07-19
**Status:** Decided (scope note, not an architectural decision)

**Decision:** This scaffolding pass creates buildable, minimal module
shells (`build.gradle.kts`, manifests, an `OVERVIEW.md` per module) for
the five remaining modules, but does not implement actual `ThreatCheck`s,
the Dagger graph, the ASM instrumentation, or CMake native checks in them.

**Reasoning:** Per the project's own working rules, half-finished feature
implementations are worse than none — a fake `RootChecksModule` with no
real checks, or a Dagger component that doesn't actually compile against
real bindings, would be misleading scaffolding that looks more done than
it is. Each real check (per `CHECKS_SEED_LIST.md`), the actual Dagger
wiring (§6), and the ASM visitor logic (`droidshield-gradle-plugin`) are
substantial enough to warrant their own focused pass rather than being
rushed into this scaffolding step.

**Follow-up:** Implement `droidshield-data-android` checks one category at
a time (ROOT first, per `CHECKS_SEED_LIST.md` ordering), then wire the
Dagger graph in `droidshield-sdk` once at least one category has real
checks to bind.

**Resolution (2026-07-19):** Superseded — all five categories, the Dagger
graph, and the plugin are now implemented. See D025–D029.

---

## D024 — `droidshield-native`'s package/namespace is `dev.droidshield.nativelayer`, not `dev.droidshield.native`

**Date:** 2026-07-19
**Status:** Decided

**Decision:** The module directory stays `droidshield-native` (matches
ARCHITECTURE.md §3), but its Android `namespace` and Kotlin package are
`dev.droidshield.nativelayer`.

**Reasoning:** Verified directly in this environment — AGP rejected
`namespace = "dev.droidshield.native"` at configuration time with
"`native` is a Java keyword," since `native` is a reserved word in Java
package/identifier grammar (used for JNI method modifiers) even though
Kotlin itself doesn't reserve it. The module's Gradle name is unaffected
since Gradle project names aren't Java identifiers.

**Alternatives considered:** `dev.droidshield.nativecheck` — considered,
but `nativelayer` more directly names what §2 of `ARCHITECTURE.md` calls
this tier ("data layer... native bridge"), so it was picked as the closer
match to existing architecture vocabulary.

---

## D025 — Checks skipped from CHECKS_SEED_LIST.md, and why

**Date:** 2026-07-19
**Status:** Decided

**Decision:** Of the 50 candidate techniques in `CHECKS_SEED_LIST.md`, 9
were not implemented as `ThreatCheck`s. Each is a genuine per-technique
call, not a scope cut:

- **ROOT #7 (missing Google OTA certificate)** — no reliable public API to
  check for OTA certificate presence; implementing it would mean guessing
  at an unstable heuristic. Skipped rather than shipped as a check that
  looks authoritative but isn't.
- **ROOT #10 (RootBeer-style aggregate)** — the seed list itself frames
  this as "a fallback/reference baseline... not to depend on directly,"
  not a technique to implement.
- **DEBUGGER #9 (inotify-based memory dump detection)** — needs
  persistent background file-system watching; `ThreatCheck.evaluate()` is
  a synchronous, single-shot call (ARCHITECTURE.md §4). Forcing a
  stateful/async technique into a synchronous contract would be the "bad
  fit" the COT_PROMPT's Stage 6 self-check explicitly warns against.
- **DEBUGGER #10 (anti-debug at multiple layers)** — a design instruction
  ("duplicate a subset of the above at both layers"), not a discrete
  technique. Satisfied structurally by having both `TracerPidCheck`
  (Kotlin) and `PtraceSelfAttachCheck` (native) rather than as its own
  check class.
- **HOOK #9 (watchdog thread pattern)** — same contract mismatch as
  DEBUGGER #9: a background thread re-verifying checks haven't been
  patched out doesn't fit a single synchronous `evaluate()` call.
- **HOOK #10 (reaction-on-detection design note)** — explicitly "not a
  detection method itself" per the seed list; already satisfied by
  `CheckResult.severity` existing for integrators to key a response on.
- **EMULATOR #8 (OpenGL renderer string check)** — querying the GL
  renderer needs a live EGL/GL rendering context, which conflicts with
  DroidShield being a headless library with no `Activity`/UI dependency
  (D015). Implementing it would mean either silently requiring a
  `GLSurfaceView` from the integrator or a hidden/unstable API path — both
  worse than not implementing it.
- **TAMPER #8 (anti-repackaging logic-bomb pattern)** — the seed list
  itself flags this as "an advanced/V2 pattern... implementation-heavy,"
  not a V1 `ThreatCheck`.
- **TAMPER #9 (Play Integrity API integration point)** — the seed list
  itself frames this as "not a DroidShield-native check," and D001 (no
  bundled vendor dependencies) rules out adding a Play Integrity
  dependency to the core `.aar` regardless.

**Alternatives considered:** Implement weaker/partial versions of all 9
anyway to hit "10 per category." Rejected — `CHECKS_SEED_LIST.md` itself
explicitly instructs against padding the list with filler, and a
weak-on-purpose check that looks real is worse than an honestly-absent
one for a security tool.

---

## D026 — Polymorphic build variance via Gradle-plugin source codegen, not ASM bytecode instrumentation

**Date:** 2026-07-19
**Status:** Revisitable

**Decision:** `droidshield-gradle-plugin` generates one Kotlin source file
per build (`DroidShieldBuildSeed.SEED`, a `Long`) and wires it into the
host app's Kotlin compilation. The engine's `CheckOrder.Seeded` (see
`droidshield-engine/CheckOrder.kt`) uses that seed to shuffle check order
and optionally select a subset at *runtime*. No ASM `AsmClassVisitorFactory`
bytecode rewriting of host-app classes is implemented.

**Reasoning:** `ARCHITECTURE.md` §3 originally assumed AGP Instrumentation
API + ASM class visitors as the mechanism. Implementing real ASM bytecode
rewriting correctly — visiting arbitrary host-app classes, injecting calls
at the right point, handling every AGP variant/build-type combination —
is genuinely high-risk to get right without iterative testing against a
running host app on a device, which wasn't available in this environment.
A subtly wrong ASM visitor doesn't fail loudly; it silently corrupts or
skips host-app bytecode, which is a far worse failure mode for a
security SDK than a less exotic mechanism. Source-codegen achieves the
actual stated goal from ARCHITECTURE.md §1 — "each build assembles a
different subset/ordering... of checks" — without touching host bytecode
at all: the seed varies every build (or is pinned via
`-PdroidshieldSeed=<n>` for reproducible testing), and `CheckOrder.Seeded`
does the actual subset/ordering variance at runtime. This was verified
working end-to-end in this environment: the generated
`DroidShieldBuildSeed` class was confirmed present in the sample app's
compiled, packaged APK (`classes13.dex`, via `dexdump`).

**What this does NOT achieve, compared to true ASM instrumentation:**
varying *where in the host app's bytecode* checks get invoked from, or
injecting checks into arbitrary host classes without any code on the
integrator's part. The current mechanism still requires the integrator to
explicitly call `DroidShield.init()` — it doesn't auto-inject that call.

**Alternatives considered:** Implement ASM instrumentation anyway, accept
the risk. Rejected for this pass — the risk/verification tradeoff was
judged unacceptable without device-level testing available.

**Revisit trigger:** If auto-injecting the `DroidShield.init()` call
itself (not just varying check order/subset) becomes a real requirement,
revisit ASM instrumentation specifically for that narrower goal, with
device-level test coverage as a precondition for shipping it.

---

## D027 — `droidshield-gradle-plugin` is a standalone included build, not a regular subproject

**Date:** 2026-07-19
**Status:** Decided

**Decision:** `droidshield-gradle-plugin` has its own `settings.gradle.kts`
and is consumed via `pluginManagement { includeBuild("droidshield-gradle-plugin") }`
in the root `settings.gradle.kts`, rather than being listed in the root's
`include(...)` block.

**Reasoning:** Verified directly in this environment: a Gradle plugin
can't be applied via the `plugins { id("...") }` DSL from a sibling
subproject in the same build — that DSL resolves against Gradle's plugin
resolution mechanism (plugin portal / included builds), not the regular
project dependency graph. Since `sample-app` needed to actually apply
`id("dev.droidshield")` to prove the seed-codegen mechanism works
end-to-end (not just compile in isolation), the plugin had to become
resolvable as an actual Gradle plugin, which requires either publishing it
or using `includeBuild`. `includeBuild` was chosen since publishing isn't
applicable pre-release.

**Consequence:** This is also why D003/D022's module-boundary reasoning
(engine and native modules never depending on Gradle/AGP internals) paid
off unexpectedly — `droidshield-gradle-plugin` had an unused
`api(project(":droidshield-engine"))` dependency at the time this was
discovered, which was removed as part of this change (D023's scaffold
declared it speculatively; the actual seed-codegen implementation never
needed it). Had the plugin genuinely needed an engine dependency, the
included-build conversion would have created a circular composite-build
reference (included build depending back on the including build), which
Gradle doesn't support — a real constraint worth remembering if a future
change tries to give the plugin a project dependency back into the main
build.

**Alternatives considered:** Keep it a regular subproject and only unit-test
`DroidShieldPlugin` in isolation (e.g. via `ProjectBuilder`), without
proving it applies to a real Android module. Rejected — an untested
"plugin applies to a real host app" path is exactly the kind of gap that
looks done but isn't; the whole point of building `sample-app` was to
verify integration, not just compilation.

---

## D028 — Generated Kotlin sources wired via KGP's `sourceSets`, not AGP's `variant.sources.kotlin`

**Date:** 2026-07-19
**Status:** Decided

**Decision:** `DroidShieldPlugin` adds the generated-seed output directory
to the Kotlin Gradle Plugin's own source set
(`KotlinAndroidProjectExtension.sourceSets.getByName("main").kotlin.srcDir(...)`),
not via AGP's `AndroidComponentsExtension` → `variant.sources.kotlin?.addGeneratedSourceDirectory(...)`.

**Reasoning:** Verified directly in this environment, the hard way. The
first implementation used `variant.sources.kotlin?.addGeneratedSourceDirectory(...)`
(AGP 8.7.2's public Variant API). It compiled cleanly and even appeared to
work — the generated-source task ran and AGP redirected its output
directory to AGP's own managed convention path
(`build/generated/kotlin/generateDroidShieldSeed/...`), which is the
expected symptom of the wiring call actually executing. But
`:sample-app:compileDebugKotlin` still failed with "Unresolved reference"
for the generated class — `variant.sources.kotlin` accepted the
registration but that bucket is never actually consumed by
`compileDebugKotlin` for a plain (non-Kotlin-Multiplatform)
Android+Kotlin module on this AGP 8.7.2 / Kotlin 2.0.21 combination. This
looks like a real, currently-existing gap in that AGP/KGP version
combination's integration, not a mistake in how the API was called. The
Kotlin Gradle Plugin's own `sourceSets` API is the mechanism actually
consumed by `compileDebugKotlin` — switching to it fixed the failure, and
was confirmed by finding the generated `DroidShieldBuildSeed` class
directly inside a `dexdump` of the built APK's DEX files.

**Alternatives considered:** None — this was found empirically after the
"obviously correct" AGP-native approach silently failed to wire through,
so there wasn't a menu of options being compared; there was one approach
that looked right and didn't work, and one that was verified to work.

**Revisit trigger:** If a future AGP/Kotlin version combination properly
threads `variant.sources.kotlin` through to `compileDebugKotlin` for
plain Android+Kotlin modules (not just KMP), the KGP-specific wiring could
be simplified back to the AGP-native API — but only after re-verifying
with the same dexdump-level proof used here, not just "it compiles."

---

## D029 — TAMPER checks needing integrator-supplied config default to "not configured" (a clean, non-detecting result) rather than failing

**Date:** 2026-07-19
**Status:** Decided

**Decision:** `ApkSignatureCheck`, `DexIntegrityCheck`,
`NativeLibraryIntegrityCheck`, and `AssetTamperCheck` all take expected
hash/CRC values the integrator must supply (via `DroidShieldConfig`).
When left at their empty/blank defaults, each returns
`CheckResult(detected = false, detail = "not configured")` rather than
throwing or returning a false positive.

**Reasoning:** DroidShield cannot know an integrator's actual release
signing hash or DEX checksums — there's no default that would be
meaningful (ARCHITECTURE.md's "no backend, no assumptions" principle
extends to build secrets too). Returning `detected = false` for an
unconfigured check means: enabling `TamperChecksModule` doesn't
immediately spam an integrator with false-positive threat reports before
they've filled in their real values, but the `detail = "not configured"`
field makes the gap visible to anyone actually inspecting results (as
opposed to silently and invisibly no-op'ing).

**Alternatives considered:** Throw an exception if unconfigured, forcing
the integrator to notice immediately. Rejected — `ThreatDetectionEngine`
already treats a thrown exception as a `check_error` and skips the result
(see `ThreatDetectionEngine.kt`), which would make an unconfigured check
indistinguishable from a genuinely broken one in telemetry, losing the
more specific "not configured" signal.

---

---

## D030 — Coroutine entry point added; `Application.onCreate()` must not call the blocking `runChecks()` directly

**Date:** 2026-07-19
**Status:** Decided

**Decision:** `DroidShield` gains `suspend fun runChecksSuspending(): List<CheckResult>`,
which runs the existing blocking `runChecks()` inside `withContext(Dispatchers.IO)`.
`droidshield-sdk` now depends on `kotlinx-coroutines-core` as an `api`
dependency. `sample-app`'s `SampleApplication` was fixed to call
`runChecksSuspending()` from a `CoroutineScope(SupervisorJob()).launch { }`
instead of calling the blocking `runChecks()` straight from `onCreate()`.

**Reasoning:** This was a real bug, not a style preference — caught by
the user, and it directly contradicted DECISIONS.md D016, which already
documented that individual checks do blocking I/O (`/proc` reads, ZIP
parsing, a socket connect attempt in `FridaPortCheck`) and must not run
on a thread invoked from a lifecycle callback. The original
`SampleApplication` called `droidShield.runChecks()` synchronously
inside `onCreate()` — exactly the case D016 warned about — which would
stall app startup on the main thread. D016 also considered and
deferred making `ThreatCheck.evaluate()` itself a `suspend fun`,
reasoning that forcing every check author to deal with coroutines
raises the contribution bar for what should be simple, synchronous
Kotlin. That reasoning still holds: the fix here keeps every
`ThreatCheck` implementation and `ThreatDetectionEngine.runAll()`
completely synchronous, and pushes the threading decision to the one
place that actually needs to make it — the public facade. `runChecks()`
(blocking) is kept alongside `runChecksSuspending()` rather than removed,
since a caller that already manages its own background thread (e.g. a
`WorkManager` `Worker.doWork()`, which runs off the main thread by
contract) shouldn't be forced to spin up a coroutine just to call a
method it can already call safely.

**Alternatives considered:**
- Make `ThreatCheck.evaluate()` itself `suspend`. Rejected again, for
  the same reason as D016: it would ripple through every one of the ~37
  check implementations for a problem that's actually about *where the
  whole batch runs*, not about any individual check's own contract.
- Remove the blocking `runChecks()` entirely, forcing all callers
  through the suspend variant. Rejected — would break the legitimate
  "I'm already on a background thread" caller for no benefit.

**Verified:** `./gradlew build` succeeds for all 7 modules with the new
dependency and API surface; `sample-app:compileDebugKotlin` and
`droidshield-sdk:compileDebugKotlin` both compile cleanly against the
coroutine-based call site.

---

## D031 — sample-app needs a launcher Activity; DroidShield itself stays headless

**Date:** 2026-07-19
**Status:** Decided

**Decision:** Added `MainActivity` (a trivial `Activity` showing a static
`TextView`) with a `MAIN`/`LAUNCHER` intent filter to `sample-app`. No
change to `droidshield-sdk` or any other module.

**Reasoning:** `sample-app` had an `Application` class but zero
`Activity`s — verified directly by installing and launching it on a
connected emulator, which failed with "default activity not found"
since there was nothing for the launcher to resolve. This is specific
to the *demo app*, not the SDK: `droidshield-sdk` correctly has no
`Activity`/`Fragment` dependency (D015, headless library), and that
constraint is unaffected — `MainActivity` lives in `sample-app` only,
exists purely so the demo app is launchable, and does nothing but
display a static pointer to the logcat tag where `SampleApplication`
already logs check results.

**Verified on-device (2026-07-19, Pixel 7a AVD):** Installed and
launched via `adb shell am start`. All 40 registered checks
(36 Kotlin + 4 native) ran with zero crashes via the
`runChecksSuspending()` coroutine path added in D030. Native checks
executed correctly on-device (`debugger.ptrace_self_attach` and others
returned real results, not just compiled), and the emulator-detection
checks correctly fired true (`emulator.build_fingerprint`,
`emulator.build_product_board`, `emulator.network_interface`) since
this genuinely is an AVD — the first true runtime confirmation of the
check logic, not just a build-time compile check.

## D032 — `sample-app` is a standalone build consuming published JitPack artifacts, not a subproject on `project(":droidshield-sdk")`

**Date:** 2026-07-19
**Status:** Decided

**Decision:** Removed `:sample-app` from the root `settings.gradle.kts`
`include(...)` list and gave it its own `settings.gradle.kts`,
`build.gradle.kts` and `gradle.properties`. It now resolves
`com.github.venkata-ram.DroidShield:droidshield-sdk:0.1.0` and the
`dev.droidshield` plugin from JitPack. Built with
`cd sample-app && ../gradlew assembleDebug`. No source changes to
`SampleApplication` or `MainActivity` were needed.

**Reasoning:** As a subproject on `project(":droidshield-sdk")`,
`sample-app` proved the *code* worked but never the thing an integrator
actually does. Project dependencies bypass POM generation entirely, and
the `includeBuild` in `pluginManagement` (D027) substituted the plugin
locally, so the whole publishing path — transitive POM correctness, the
packaged `.so`s inside the AAR, and the plugin-marker coordinate — was
untested by any build in the repo. That path is also the one most likely
to break silently, because JitPack rewrites the group ID to
`com.github.<user>.<repo>`, which violates the convention that a plugin
marker lives at a group ID equal to the plugin ID. The
`resolutionStrategy.eachPlugin` workaround for this existed only as a
README snippet nobody executed; it is now the sample's real
`settings.gradle.kts`, so a documentation drift or a broken release
fails a build instead of reaching an integrator.

Versions in the sample are hardcoded rather than read from
`gradle/libs.versions.toml`, since a standalone build has no access to
the root catalog — and shouldn't, because an integrator copying the file
wouldn't have it either.

**Trade-offs accepted:** (1) The sample is pinned to a published release,
so it lags the working tree by design and cannot exercise uncommitted SDK
changes; `publishAllToMavenLocal` plus `mavenLocal()` is the documented
route for that (recorded in `sample-app/OVERVIEW.md`). (2) A root-level
`./gradlew build` no longer compiles the sample, so it needs its own step
once CI exists. Both were preferred over the status quo, where the sample
compiled reliably while telling us nothing about whether a release was
consumable.

**Verified (2026-07-19):** `cd sample-app && ../gradlew assembleDebug` →
BUILD SUCCESSFUL, 36 tasks, resolving 0.1.0 from JitPack with no local
substitution. `compileDebugKotlin` succeeding is itself proof the
published Gradle plugin resolved through the `resolutionStrategy` mapping
and generated `DroidShieldBuildSeed`, since `SampleApplication` imports it.
The output APK contains `lib/{arm64-v8a,armeabi-v7a,x86_64}/libdroidshield.so`,
confirming the native artifacts survive the publish → JitPack → consume
round trip. Root `./gradlew assemble` still passes without the module.
