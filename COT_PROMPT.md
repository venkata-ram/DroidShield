# Chain-of-thought prompt: DroidShield architecture + check research

Use this prompt (with an LLM that has web search / research capability) to
regenerate or extend the DroidShield ARCHITECTURE.md and check seed list.
Designed to be re-run per category when adding new threat categories later.

---

## PROMPT

You are acting as a senior Android platform/security architect helping design
an open-source Android runtime threat-detection SDK called DroidShield. It is a
foundation for RASP architectures and backend-driven enforcement, not a complete
RASP product. Work through this task in the following explicit stages. Do not
skip a stage or merge stages together — think step by step and show your
reasoning at each stage before producing the final artifact.

### Stage 1 — Constraints restatement
Before designing anything, restate in your own words:
- This is an **open-source** project, not a commercial product. There is no
  backend, no sales motion, and no paid team — one primary maintainer plus
  volunteer contributors.
- The primary design goal is **extensibility**: a contributor who has never
  read the Gradle plugin or seeded ordering engine must be able to add ONE new threat check by
  touching the smallest possible surface area.
- The architecture must follow **Clean Architecture** (dependency rule:
  outer layers depend on inner layers, not vice versa) and **Android SDK
  library best practices** (no forced lifecycle dependencies, ProGuard
  rules shipped, per-ABI native packaging, etc).
- Dependency injection must use **Dagger 2** specifically — not Hilt (a
  library can't force `@HiltAndroidApp` onto every host app) and not Koin
  (compile-time graph validation matters more than runtime flexibility for
  a security SDK).
- Reporting/backend is **fully pluggable** — the SDK ships a
  `ThreatReporter` interface and a default logcat implementation; it never
  assumes or ships a hosted backend.
- Telemetry (operational/usage data — check execution counts, latency,
  errors) is **also fully pluggable and separate from `ThreatReporter`**.
  The SDK ships a `TelemetrySink` interface and a no-op default, so an
  integrator can wire in PostHog, Firebase, Mixpanel, or any other
  analytics service without DroidShield's core `.aar` depending on any of
  them. Do not conflate this with `ThreatReporter` — one is a security
  signal, the other is operational health/usage data, and forcing them
  into one interface couples two unrelated integrator decisions.
- SSL pinning / network hardening is explicitly **out of scope** for this
  pass (tracked separately as `NetworkGuard`, deferred).

State any constraint you're uncertain about rather than silently assuming
an answer.

### Stage 2 — Layer and module design
Design the module structure using Clean Architecture:
1. Identify the innermost layer (pure Kotlin/Java, zero Android imports) —
   this holds the core contract types (`ThreatCheck`, `CheckResult`,
   `ThreatCategory`, `Severity`, `ThreatReporter`).
2. Identify what needs Android APIs vs what needs native/C++ — split these
   into separate modules, since they have different toolchains and
   different contributor skill bars.
3. Identify what is purely build-time (Gradle plugin and Kotlin seed source
   generation) vs purely runtime (the `.aar`, the Dagger graph).
4. Identify the engine/registry as its own module that depends ONLY on the
   core contract types — never on any specific check implementation. This
   is what keeps the engine stable while checks grow.
5. For each module, write one sentence: "A contributor touches this module
   when they want to ___."

Justify why each module boundary exists — a boundary that doesn't reduce
contributor cognitive load or doesn't isolate a genuinely different
toolchain/skillset is unnecessary complexity; cut it.

### Stage 3 — The extensibility contract
Design the minimal interface a new check must implement. Constraints:
- Must be implementable by someone who knows Kotlin but has never seen the
  rest of the codebase.
- Must be self-registering or near-self-registering (minimize "you also
  need to edit file X" steps).
- Must carry enough metadata (category, severity, id) for the ordering
  engine to reason about it generically, without the engine knowing
  anything about what the check actually does.

Show 2-3 alternative designs for the registration mechanism (e.g. manual
list, annotation processor, Dagger multibinding, ServiceLoader/manifest),
list the contributor-effort/build-cost/risk trade-off for each, and commit
to one for V1 with a one-sentence justification. Flag it explicitly as
revisitable in a decisions log.

### Stage 4 — Dagger 2 wiring
Given the contract from Stage 3, design the actual Dagger 2 component and
module structure:
- One `@Component` that assembles the full graph, taking `Context` as a
  `@BindsInstance`.
- One `@Module` per threat category (root, debugger, hook, emulator,
  tamper — or whatever categories Stage 1 settled on), each contributing
  to a shared `Set<ThreatCheck>` via `@Provides @IntoSet`.
- One `@Module` for reporting, defaulting to a no-op/logcat
  `ThreatReporter` if the integrator supplies none.
- One `@Module` for telemetry, defaulting to a no-op `TelemetrySink` if the
  integrator supplies none. This must be wired as its own binding, separate
  from the `ThreatReporter` binding — do not merge them into one module or
  one interface, even though both are "callback the integrator supplies."
Write the actual Kotlin code for this, not just prose description.

### Stage 5 — Research: seed checks per category
For each `ThreatCategory` identified in Stage 1/2, research (using real
web search against authoritative sources — OWASP MASTG is the primary
source; do not invent techniques from memory) at least 10 distinct,
genuine, non-overlapping detection techniques. For each technique:
- One-line description of the mechanism.
- Note if it's Java/Kotlin-layer or native-layer (affects which module it
  belongs in).
- Note its known weakness/bypass if the source documents one (this is
  expected and fine — runtime detection checks are individually weak; combining +
  shuffling many is the actual defense).

Do not pad the list with near-duplicate checks to hit the count — if a
category genuinely only has 6-7 well-documented distinct techniques, say
so explicitly rather than inventing filler. Prefer fewer, real, cited
techniques over ten checks where three are trivial rewordings of each
other.

Cite the actual source (OWASP MASTG page ID, research paper, or
established library documentation) for every technique — a check with no
traceable source is a liability for an open-source security tool, since
contributors and users will ask where it came from.

### Stage 6 — Self-check before finalizing
Before producing the final ARCHITECTURE.md and check list, verify:
- Does every module boundary from Stage 2 still make sense given the
  concrete Dagger wiring from Stage 4? (Sometimes wiring reveals a module
  split was unnecessary or a needed one was missed.)
- Can the Stage 3 contract actually express every technique found in
  Stage 5? If a Stage 5 technique doesn't fit the `ThreatCheck` interface
  cleanly (e.g. something requiring persistent background state, or
  something that's a prevention mechanism rather than a detection check —
  like SSL pinning), flag it as needing a different contract
  (`NetworkGuard`-style) rather than forcing a bad fit.
- Is any part of this design implicitly assuming a paid team, a backend,
  or a sales motion? If so, remove it — re-check against Stage 1.

### Final output
Produce:
1. `ARCHITECTURE.md` — the full design, written for a contributor who has
   never seen the project before.
2. A seed check list — organized by category, minimum 10 per category
   where genuinely available, each with source citation.
3. A short "open questions" section listing anything flagged as
   uncertain/revisitable in Stages 1-6, so it can be logged in
   `DECISIONS.md` rather than silently decided.

---

## Notes on reusing this prompt

- Re-run Stage 5 alone (with the category list from an existing
  `ARCHITECTURE.md`) when adding a new `ThreatCategory` later — no need to
  redo Stages 1-4 unless the category requires a new module (e.g. a
  network-layer category would revisit Stage 2/3, since it's a prevention
  contract, not a detection one).
- If handing this to a different LLM/agent session, paste the current
  `ARCHITECTURE.md` and `DECISIONS.md` alongside the prompt so Stage 1's
  constraint restatement is grounded in what's actually been decided,
  not re-derived from scratch.
