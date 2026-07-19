# DroidShield — Seed check list (V1 research)

Sourced primarily from OWASP MASTG (Mobile Application Security Testing
Guide) — the closest thing to a canonical, community-vetted reference for
these techniques. Each check below is a *candidate* for
`droidshield-data-android` or `droidshield-native`, not a finished
implementation. All of these are publicly documented, well-known techniques
(the same ones OWASP documents for *testing* apps against) — nothing here
is novel bypass research.

Every check is intentionally weak alone (OWASP is explicit that
"more-is-better" — no single check is reliable) — the polymorphic engine's
value is combining + shuffling many of these, not any one check being
strong.

---

## ROOT (10)

1. **su binary path check** — `File.exists()` against known su paths
   (`/system/bin/su`, `/system/xbin/su`, `/sbin/su`, `/su/bin/su`, etc).
   Weakest, most well-known check; still worth including as one signal
   among many.
2. **Root management app package check** — `PackageManager` lookup for
   known root manager packages: `com.topjohnwu.magisk`, `eu.chainfire.supersu`,
   `com.noshufou.android.su`, `com.koushikdutta.superuser`. Note: Android 11+
   package visibility restrictions can cause false negatives here — needs a
   `<queries>` manifest declaration to work reliably.
3. **Running process scan for su/root managers** — enumerate running
   services/processes and match against known root-related process names.
4. **Build tags check** — `Build.TAGS` containing `test-keys` (signals a
   non-stock/custom-signed build).
5. **Writable system partition check** — attempt to detect `/system`
   mounted read-write instead of the stock read-only.
6. **Dangerous system properties check** — inspect `ro.debuggable`,
   `ro.secure`, and related build properties for values inconsistent with a
   locked-down stock device.
7. **Missing Google OTA certificate check** — stock Android builds carry
   Google's OTA update certificates; absence suggests a custom ROM.
8. **Custom ROM / build fingerprint heuristic** — `Build.FINGERPRINT`,
   `Build.MANUFACTURER`, `Build.MODEL` compared against known emulator/
   generic-build patterns (overlaps with emulator detection — shared
   signal).
9. **Execution of shell commands succeeding unexpectedly** — attempt a
   command that should fail on a non-rooted device (e.g. `su -c id`) and
   check for unexpected success rather than a permission error.
10. **RootBeer-style aggregate library check** — as a fallback/reference
    baseline, note that libraries like RootBeer combine many of the above;
    useful as a cross-check during testing, not to depend on directly.

## DEBUGGER (10)

1. **`Debug.isDebuggerConnected()`** — the standard Android API check.
   Trivially bypassable alone (OWASP notes hooking this single function
   defeats it) — must be combined with others.
2. **`ApplicationInfo.FLAG_DEBUGGABLE` manifest flag check** — verify the
   app wasn't repackaged with `android:debuggable="true"`.
3. **JDWP presence check via `/proc/self/status`** — inspect `TracerPid`
   field; non-zero indicates a tracer/debugger is attached.
4. **`Debug.waitingForDebugger()`** — detects the "Wait for Debugger"
   developer-options scenario during app startup.
5. **Timing-based detection** — measure execution time across a code
   region; anomalous delays suggest single-stepping or breakpoints.
6. **ptrace self-attach (native)** — native code attempts `ptrace(PTRACE_TRACEME, ...)`
   on itself; if it fails, another process (a debugger) is already attached.
   This is a native-layer check (see `droidshield-native`).
7. **JDWP string in `/proc/self/task/comm`** — community-contributed
   technique (flagged in OWASP's own issue tracker) for surfacing JDWP
   debuggability from process metadata.
8. **Native breakpoint / signal handler check** — install a `SIGTRAP`
   handler and verify it fires as expected; anomalies suggest an attached
   native debugger.
9. **inotify-based memory dump detection** — watch for file-system events
   consistent with a memory-dump tool writing output during app execution.
10. **Anti-debug at multiple layers** — deliberately duplicate a subset of
    the above at both Java and native layers per OWASP's explicit guidance
    that single-layer defenses are the easiest to bypass.

## HOOK (Frida / Xposed / hooking frameworks) (10)

1. **`/proc/self/maps` scan for frida/xposed signatures** — scan loaded
   memory mappings for strings like `frida`, `xposed`, `substrate`; a
   documented, widely-used technique (also widely known to be bypassable
   via `strstr` hooking — combine with others).
2. **Frida default port check** — attempt connection to Frida's default
   ports (27042/27043); presence suggests `frida-server` is running.
3. **Frida named pipe check** — detect named pipes frida-server uses for
   external communication.
4. **Suspicious loaded native library enumeration** — list loaded shared
   libraries and flag unexpected ones (`frida-agent.so`, `frida-gadget.so`).
5. **Native code checksum/integrity comparison** — compare the on-disk
   checksum of a native library's executable section against the
   in-memory version; mismatch indicates runtime tampering by a hooking
   framework (Frida rewrites the `.text` section on attach).
6. **Trampoline / inline-hook detection** — inspect function prologues for
   unexpected jump instructions consistent with Frida's `Interceptor` API
   trampolining.
7. **Xposed framework artifact check** — check for Xposed-specific
   classes/files (e.g. presence of `de.robv.android.xposed.XposedBridge`
   on the classpath via reflection).
8. **LSPosed / Zygisk module detection** — check for known Magisk
   module mount points associated with LSPosed.
9. **Watchdog thread pattern** — a background thread periodically
   re-verifying the above checks haven't been silently patched out at
   runtime (defends against a single-point-in-time bypass).
10. **Reaction on tool detection per OWASP test guidance** — not a
    detection method itself, but a required design note: OWASP's own test
    criteria (MASTG-TEST-0048) expect apps to respond to hook detection
    (alert, block, wipe, or report) — the check registry should carry a
    `severity`/`recommendedAction` hint so integrators can build this.

## EMULATOR (10)

1. **Build fingerprint/model heuristics** — `Build.FINGERPRINT`,
   `Build.MODEL`, `Build.MANUFACTURER`, `Build.HARDWARE` matched against
   well-known emulator signatures (`generic`, `sdk`, `Genymotion`, `Android SDK
   built for x86`, `google_sdk`).
2. **`Build.PRODUCT` / `Build.BOARD` checks** — similar heuristic against
   emulator-specific values (`sdk_gphone`, `vbox86p`, etc).
3. **Telephony properties check** — emulators commonly report fixed/known
   dummy values for `IMEI`, `SubscriberId`, or phone number.
4. **Sensor absence check** — real devices have accelerometer/gyroscope/
   proximity sensors; many emulator configs report zero or a minimal
   sensor set.
5. **QEMU-specific file/pipe check** — presence of QEMU-related files
   (`/dev/qemu_pipe`, `/dev/socket/qemud`) is a strong Android-emulator
   signal.
6. **CPU info check** — `/proc/cpuinfo` inspected for virtualized/known
   emulator CPU signatures rather than real ARM hardware identifiers.
7. **Network interface heuristic** — emulator network stacks often expose
   distinctive interface configurations (e.g. specific default gateway
   patterns in Android Studio's AVD).
8. **OpenGL renderer string check** — querying the GL renderer often
   reveals software/virtualized renderers (`Android Emulator OpenGL ES`)
   instead of a real GPU vendor string.
9. **Battery status heuristic** — emulators frequently report an
   unrealistic/static battery status (e.g. always plugged-in, fixed level).
10. **Installed package heuristic** — presence of Genymotion-specific
    packages or absence of packages a real consumer device would almost
    always have (e.g. no Google Play Store on some bare emulator images).

## TAMPER / INTEGRITY (10)

1. **APK signature verification at runtime** — compare the running app's
   signing certificate against an expected pinned hash; catches re-signed/
   repackaged APKs. This is the core anti-repackaging check.
2. **Package name / installer source check** — verify
   `getInstallerPackageName()` matches an expected source (Play Store)
   where that's a meaningful signal (documented as weak alone since
   sideloading is common and legitimate in many markets — treat as a
   low-severity signal only).
3. **CRC/checksum of critical DEX or resource files** — detect
   modification of classes.dex or key resources post-build.
4. **Native library integrity check** — checksum the `.so` file's on-disk
   contents vs an expected value baked in at build time (complements the
   hook-detection checksum check, different purpose: catches static
   patching, not runtime hooking).
5. **Manifest tamper check** — verify `debuggable`/`allowBackup`/exported-
   component flags in the running `ApplicationInfo` match what the build
   actually declared, catching manifest-level repackaging.
6. **Multi-DEX integrity walk** — for apps with multiple DEX files,
   checksum each rather than only the primary one (repackaging tools often
   target secondary DEX files to evade naive single-file checks).
7. **Resource/asset tamper check** — hash critical raw assets (e.g. crypto
   key material, config files bundled as assets) and compare at runtime.
8. **Anti-repackaging logic-bomb pattern (research-documented)** — per
   published anti-repackaging research, scatter integrity checks whose
   effects surface asynchronously/later in execution rather than
   immediately, so static analysis and naive dynamic tracing both have a
   harder time correlating cause and effect. Flag as an advanced/V2
   pattern, not a simple V1 `ThreatCheck` — implementation-heavy.
9. **Play Integrity API integration point** — not a DroidShield-native
   check, but the architecture should leave a clean extension point for
   integrators who also want Google's Play Integrity API result folded
   into the same `CheckResult`/severity model.
10. **Self-checksum of the DroidShield engine itself** — the polymorphic
    engine and registry code checksum their own presence/integrity, so an
    attacker can't cleanly strip DroidShield out of the APK without
    tripping a tamper signal.

---

## Sourcing notes

- Root, debugger, and hook categories are drawn directly from OWASP MASTG
  (`MASTG-KNOW-0027` root detection, `MASTG-KNOW-0028` anti-debugging,
  `MASTG-TEST-0048` reverse-engineering-tool detection) plus the anti-
  repackaging research paper "You Shall not Repackage! Demystifying
  Anti-Repackaging on Android" (arXiv:2009.04718) for the tamper category's
  logic-bomb pattern.
- Emulator detection list reflects widely-documented, standard heuristics
  used across open-source detection libraries (RootBeer-adjacent
  literature) — no single canonical OWASP page enumerates all ten, so
  treat this category as "well-established community knowledge" rather
  than one-citation-sourced like root/debugger/hook.
- Every technique listed here is also, by the same OWASP pages, a
  documented *bypass target* — i.e. publicly known to be defeatable in
  isolation. This is exactly why the polymorphic engine (combining +
  shuffling many weak signals per build) is the actual differentiator, not
  any individual check's strength.
