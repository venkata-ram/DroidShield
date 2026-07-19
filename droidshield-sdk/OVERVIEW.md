# droidshield-sdk

The public `.aar`. Contains the `DroidShield` facade, the Dagger 2
component (`DroidShieldComponent`), and the wiring connecting
`data-android` + `native` implementations to `engine` at runtime
(ARCHITECTURE.md §6).

`runChecksSuspending()` returns raw results. `collectEvidence()` runs the same
checks and packages them with app/device metadata into the transport-neutral
backend contract from `droidshield-domain`; the host app owns networking.

`@DroidShieldGuarded` marks application methods for Gradle-plugin
instrumentation. `DroidShieldGuardRuntime` receives the injected calls and runs
checks on a dedicated executor with configurable cooldown and in-flight
deduplication; `DroidShield.init()` installs the active runtime instance.

Consumer ProGuard rules ship from here (`consumer-rules.pro`,
DECISIONS.md D014), including JNI/reflection and backend wire-DTO rules.
