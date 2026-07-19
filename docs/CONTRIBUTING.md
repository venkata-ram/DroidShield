# Contributing to DroidShield

Thanks for helping improve DroidShield. Contributions can include new checks,
false-positive fixes, tests, documentation, build improvements, and carefully scoped
API changes.

## Before you start

- Search existing issues and pull requests before opening a duplicate.
- For a large API, architecture, dependency, or threat-model change, open an issue
  first so the approach can be agreed before substantial implementation work.
- Do not publish secrets, real user/device data, or working exploit details in an
  issue. Use GitHub's private security-advisory flow for a vulnerability that would
  put users at risk if disclosed publicly.
- Read [ARCHITECTURE.md](ARCHITECTURE.md) before changing module boundaries and
  [DECISIONS.md](DECISIONS.md) before reversing an established decision. The decision
  log is append-only: add a superseding entry instead of editing history.

## Development setup

You need:

- JDK 17
- Android SDK 35
- Android NDK with CMake 3.22.1 for native checks

Clone the repository and run the complete local verification:

```bash
./gradlew test assemble
```

The `sample-app` is a standalone consumer of the published JitPack artifacts. It is
useful for release verification, but it does not compile uncommitted SDK changes unless
you first publish those artifacts to Maven Local as described in
[INTEGRATION.md](INTEGRATION.md#consuming-a-local-build).

## Repository map

| Module | Responsibility |
|---|---|
| `droidshield-domain` | Stable Kotlin contracts and backend evidence/verdict models |
| `droidshield-data-android` | Android root, debugger, hook, emulator, and tamper checks |
| `droidshield-native` | JNI/C++ checks where native code meaningfully raises the bypass cost |
| `droidshield-engine` | Check execution, ordering, reporting, and telemetry |
| `droidshield-sdk` | Public Android facade and dependency-injection graph |
| `droidshield-gradle-plugin` | Per-build polymorphic seed generation |
| `sample-app` | Standalone published-artifact integration test |

## Adding a threat check

Most new checks belong in the appropriate category under
`droidshield-data-android/src/main/kotlin/dev/droidshield/data/checks/`.

1. Implement `ThreatCheck` in one focused class.
2. Give it a stable, descriptive `checkId`. Treat a released ID as part of the backend
   wire contract; do not rename it casually.
3. Return a clean, non-detecting result when optional integrator configuration is
   absent.
4. Avoid collecting personal data. Keep `detail` diagnostic, minimal, and safe for
   local logs even though backend evidence excludes it by default.
5. Register the check with one Dagger `@Provides @IntoSet` binding in its category
   module.
6. Add focused tests covering clean, detected, unavailable, and failure-prone paths.

Use native code only where it provides a concrete resistance benefit. Reading an
ordinary Android API through JNI adds complexity without meaningfully improving the
security boundary.

## Code and API expectations

- Keep the domain module free of Android imports.
- Do not add a backend, analytics vendor, HTTP client, or JSON converter to the core
  SDK. Integration contracts must remain transport-neutral.
- Do not make checks crash the host application. Expected environmental failures
  should become clean results or contained telemetry errors.
- Preserve the five existing `ThreatCategory` values. New values are additive API
  changes and need explicit design discussion.
- Keep checks synchronous; the SDK facade owns background dispatch through
  `runChecksSuspending()` and `collectEvidence()`.
- Update public KDoc and integration documentation when changing a public contract.

## Pull requests

Create a focused branch, make the smallest coherent change, and verify it locally.
Every pull request should include:

- what changed and why;
- security and false-positive considerations;
- tests or an explanation of why tests are not applicable;
- documentation updates for public behavior;
- the output of `./gradlew test assemble`.

Pull requests target `main`. The branch is protected: changes must go through a pull
request, receive an approval, resolve review conversations, and preserve linear
history. Force-pushes and branch deletion are disabled on `main`.

By contributing, you agree that your contribution is licensed under the repository's
[Apache License 2.0](LICENSE).
