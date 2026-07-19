# DroidShield — Implemented check sources (40 checks)

Every implemented check below cites a specific, publicly checkable source — a MASTG
page ID with URL, a research paper, or a well-known reference
implementation on GitHub. Nothing here is invented; all of these are
long-documented, widely-used techniques (the same ones security testers
use when auditing an app for MASTG compliance).

Every check is intentionally weak alone — OWASP is explicit that
"more-is-better": no single check is reliable, and every technique listed
here is also, on the same cited pages, a documented *bypass target*. The
polymorphic engine's value is combining + shuffling many of these across
builds, not any one check being individually strong. Cite this file's
"Sourcing notes" section in your README/CONTRIBUTING so users and
reviewers understand this is deliberate.

---

## ROOT (8)

1. **su binary path check** — `File.exists()` against known su paths
   (`/system/bin/su`, `/system/xbin/su`, `/sbin/su`, `/su/bin/su`, etc).
   *Source:* OWASP MASTG, `MASTG-KNOW-0027` — Root Detection.
   https://mas.owasp.org/MASTG/knowledge/android/MASVS-RESILIENCE/MASTG-KNOW-0027/
   (bypass list with the exact path set also documented in
   `MASTG-TECH-0144` — Bypassing Root Detection:
   https://mas.owasp.org/MASTG/techniques/android/MASTG-TECH-0144/)
2. **Root management app package check** — `PackageManager` lookup for
   known root manager packages: `eu.chainfire.supersu`,
   `com.noshufou.android.su`, `com.koushikdutta.superuser`,
   `com.topjohnwu.magisk`. Android 11+ package-visibility restrictions can
   cause false negatives — needs a `<queries>` manifest declaration.
   *Source:* OWASP MASTG `MASTG-KNOW-0027` (same page as #1, lists these
   exact package names).
   https://mas.owasp.org/MASTG/knowledge/android/MASVS-RESILIENCE/MASTG-KNOW-0027/
3. **Running process scan for su/root managers** — enumerate running
   services via `ActivityManager.getRunningServices()` and match process
   names against `supersu`/`superuser`.
   *Source:* OWASP MASTG `MASTG-KNOW-0027`, `checkRunningProcesses()`
   example. https://mas.owasp.org/MASTG/knowledge/android/MASVS-RESILIENCE/MASTG-KNOW-0027/
4. **Build tags check** — `Build.TAGS` containing `test-keys` (signals a
   non-stock/custom-signed build).
   *Source:* Comparative study of emulator/root detection heuristics
   (university thesis, cites the same `Build.TAGS` field used across
   RootBeer-adjacent tooling):
   https://www.cin.ufpe.br/~tg/2022-1/tg_CC/tg_lccao.pdf
5. **Writable system partition check** — attempt to detect `/system`
   mounted read-write instead of stock read-only.
   *Source:* Standard technique referenced across RootBeer-style detection
   libraries; conceptually documented alongside OWASP's root detection
   discussion (`MASTG-KNOW-0027`) as one of the "broader" root-detection
   signals OWASP explicitly says to include (custom builds, not just su
   presence).
6. **Dangerous system properties check** — inspect `ro.debuggable`,
   `ro.secure` build properties for values inconsistent with a locked-down
   stock device.
   *Source:* OWASP MASTG anti-debugging background,
   `MASTG-BEST-0007` (debuggable flag guidance):
   https://github.com/OWASP/mastg/blob/master/best-practices/MASTG-BEST-0007.md
7. **Custom ROM / build fingerprint heuristic** — `Build.FINGERPRINT`,
   `Build.MANUFACTURER`, `Build.MODEL` against known generic/custom-build
   patterns (shared signal with emulator detection, category 8 below).
   *Source:* Same OWASP MASTG root detection material (`MASTG-KNOW-0027`),
   cross-referenced with the emulator detection sources in that category.
8. **Execution of shell commands succeeding unexpectedly** — run a command
   that should fail without root (e.g. `su -c id`) and check for
   unexpected success rather than a permission error.
   *Source:* OWASP MASTG `MASTG-KNOW-0027`, general root-detection
   discussion of su invocation as a signal.
   https://mas.owasp.org/MASTG/knowledge/android/MASVS-RESILIENCE/MASTG-KNOW-0027/
## DEBUGGER (8)

1. **`Debug.isDebuggerConnected()`** — standard Android API check.
   *Source:* OWASP MASTG `MASTG-KNOW-0028` — Anti-Debugging.
   https://mas.owasp.org/MASTG/knowledge/android/MASVS-RESILIENCE/MASTG-KNOW-0028/
2. **`ApplicationInfo.FLAG_DEBUGGABLE` manifest flag check** — verify the
   app wasn't repackaged with `android:debuggable="true"`.
   *Source:* OWASP MASTG `MASTG-BEST-0007` — debuggable flag best
   practice: https://github.com/OWASP/mastg/blob/master/best-practices/MASTG-BEST-0007.md
3. **JDWP presence via `/proc/self/status` `TracerPid`** — non-zero
   `TracerPid` indicates a tracer/debugger is attached.
   *Source:* OWASP MASTG `MASTG-TEST-0046` — Testing Anti-Debugging
   Detection: https://mas.owasp.org/MASTG/tests/android/MASVS-RESILIENCE/MASTG-TEST-0046/
4. **`Debug.waitingForDebugger()`** — detects the "Wait for Debugger"
   developer-options startup scenario.
   *Source:* OWASP MASTG `MASTG-TECH-0040` — Waiting for the Debugger:
   https://mas.owasp.org/MASTG/techniques/android/MASTG-TECH-0040/
5. **Timing-based detection** — measure execution time across a region;
   anomalous delays suggest single-stepping/breakpoints.
   *Source:* OWASP MASTG `MASTG-KNOW-0028`, general anti-debugging
   discussion of preventive vs reactive techniques (timing falls under
   reactive detection methods referenced there):
   https://mas.owasp.org/MASTG/knowledge/android/MASVS-RESILIENCE/MASTG-KNOW-0028/
6. **ptrace self-attach (native)** — native code calls
   `ptrace(PTRACE_TRACEME, ...)` on itself; failure implies another
   process (a debugger) is already attached. Native-layer check.
   *Source:* OWASP MASTG anti-debugging discussion of native/multi-layer
   defenses, `MASTG-KNOW-0028`:
   https://mas.owasp.org/MASTG/knowledge/android/MASVS-RESILIENCE/MASTG-KNOW-0028/
7. **JDWP string in `/proc/self/task/comm`** — community-contributed
   technique for surfacing JDWP debuggability from process metadata.
   *Source:* OWASP MASTG GitHub issue #1907 (community-proposed technique,
   tracked for inclusion): https://github.com/OWASP/mastg/issues/1907
8. **Native breakpoint / signal handler check** — install a `SIGTRAP`
   handler and verify it fires as expected; anomalies suggest an attached
   native debugger.
   *Source:* OWASP MASTG `MASTG-KNOW-0028`, native-layer anti-debugging
   discussion: https://mas.owasp.org/MASTG/knowledge/android/MASVS-RESILIENCE/MASTG-KNOW-0028/
## HOOK — Frida / Xposed / hooking frameworks (8)

1. **`/proc/self/maps` scan for frida/xposed signatures** — scan loaded
   memory mappings for strings like `frida`, `xposed`.
   *Source:* Darvin's Blog, "Detect Frida for Android" (documents this
   technique with the checksum-comparison rationale in #5 below):
   https://darvincitech.wordpress.com/2019/12/23/detect-frida-for-android/
2. **Frida default port check** — attempt connection to ports 27042/27043.
   *Source:* Talsec/freeRASP AppSec article, "How to Detect Hooking
   (Frida) using Kotlin":
   https://docs.talsec.app/appsec-articles/articles/how-to-detect-hooking-frida-using-kotlin
3. **Frida named pipe check** — detect named pipes frida-server uses for
   external communication.
   *Source:* OWASP MASTG `MASTG-TOOL-0001` — Frida (Android), architecture
   description: https://mas.owasp.org/MASTG/tools/android/MASTG-TOOL-0001/
4. **Suspicious loaded native library enumeration** — flag loaded
   libraries like `frida-agent.so`, `frida-gadget.so`.
   *Source:* OWASP MASTG `MASTG-TEST-0048` — Testing Reverse Engineering
   Tools Detection: https://mas.owasp.org/MASTG/tests/android/MASVS-RESILIENCE/MASTG-TEST-0048/
5. **Native code checksum/integrity comparison** — compare on-disk vs
   in-memory checksum of a native library's executable section; Frida
   rewrites `.text` on attach.
   *Source:* Darvin's Blog, "Detect Frida for Android" — describes this
   exact disk-vs-memory checksum technique:
   https://darvincitech.wordpress.com/2019/12/23/detect-frida-for-android/
6. **Trampoline / inline-hook detection** — inspect function prologues for
   jump instructions consistent with Frida's `Interceptor` API.
   *Source:* OWASP MASTG `MASTG-TOOL-0031` — Frida, explains the
   trampoline/inline-hooking mechanism directly:
   https://mas.owasp.org/MASTG/tools/generic/MASTG-TOOL-0031/
7. **Xposed framework artifact check** — check for Xposed-specific classes
   (e.g. `de.robv.android.xposed.XposedBridge`) via reflection.
   *Source:* OWASP MASTG `MASTG-TECH-0043` — Method Hooking, discusses
   Xposed module mechanics: https://mas.owasp.org/MASTG/techniques/android/MASTG-TECH-0043/
8. **LSPosed / Zygisk module detection** — check for Magisk module mount
   points associated with LSPosed.
   *Source:* Talsec/freeRASP article, listing Frida/Xposed/Magisk/LSPosed
   as the detection surface for modern RASP hook-detection:
   https://docs.talsec.app/appsec-articles/articles/how-to-detect-hooking-frida-using-kotlin
## EMULATOR (9)

No single OWASP MASTG page enumerates emulator detection the way it does
for root/debugger/hook — this category instead draws from long-standing,
widely-forked open-source reference implementations. The most-cited
original is Tim Strazzere's `anti-emulator` (an early, well-known pentest
tool); the `Build.*` property list below is essentially identical across
every source found, which is itself a signal these are stable, community-
converged techniques rather than one person's invention.

1. **Build fingerprint/model heuristics** — `Build.FINGERPRINT` starting
   with `generic`, `Build.MODEL` containing `google_sdk`/`Emulator`,
   `Build.MANUFACTURER` containing `Genymotion`.
   *Source:* CalebFenton/AndroidEmulatorDetect (reference `Detector.java`):
   https://github.com/CalebFenton/AndroidEmulatorDetect/blob/master/app/src/main/java/org/cf/emulatordetect/Detector.java
   — same property set independently corroborated in
   gmh5225/Android-Emulator-Detection:
   https://github.com/gmh5225/Android-Emulator-Detection
2. **`Build.PRODUCT` / `Build.BOARD` checks** — values like `sdk`,
   `google_sdk`, `vbox86p` (VirtualBox-based emulators), Nox-specific
   board strings.
   *Source:* Same as #1, `Detector.java`:
   https://github.com/CalebFenton/AndroidEmulatorDetect/blob/master/app/src/main/java/org/cf/emulatordetect/Detector.java
3. **Telephony properties check** — fixed/known dummy `IMEI`,
   `SubscriberId`, or phone number values.
   *Source:* strazzere/anti-emulator, `FindEmulator.java` (uses
   `TelephonyManager` checks directly):
   https://github.com/strazzere/anti-emulator/blob/master/AntiEmulator/src/diff/strazzere/anti/emulator/FindEmulator.java
4. **Sensor absence check** — emulator configs report zero or minimal
   sensor sets vs real accelerometer/gyroscope/proximity sensors.
   *Source:* samohyes/Anti-vm-in-android, README technique list ("Emulators
   may can't register sensors or its sensors have constant values"):
   https://github.com/samohyes/Anti-vm-in-android
5. **QEMU-specific file/pipe check** — `/dev/qemu_pipe`,
   `/dev/socket/qemud`, `/dev/socket/genyd` (Genymotion).
   *Source:* Corroborated across three independent sources: killcod3
   detector (https://github.com/killcod3/Android-Emulator-Detection-Method),
   samohyes/Anti-vm-in-android (https://github.com/samohyes/Anti-vm-in-android),
   and the UFPE comparative-study thesis Table 4:
   https://www.cin.ufpe.br/~tg/2022-1/tg_CC/tg_lccao.pdf
6. **CPU info check** — `/proc/cpuinfo` and `/proc/tty/drivers` inspected
   for `goldfish` (QEMU's virtual CPU) rather than real ARM identifiers.
   *Source:* strazzere/anti-emulator, `FindEmulator.java`:
   https://github.com/strazzere/anti-emulator/blob/master/AntiEmulator/src/diff/strazzere/anti/emulator/FindEmulator.java
   ; also samohyes/Anti-vm-in-android README:
   https://github.com/samohyes/Anti-vm-in-android
7. **Network interface heuristic** — emulator network stacks (e.g. `eth0`
   presence) differ from typical real-device Wi-Fi/mobile interfaces.
   *Source:* samohyes/Anti-vm-in-android README ("Some emulators has eth0
   network interface"): https://github.com/samohyes/Anti-vm-in-android
8. **Battery status heuristic** — emulators often report static/unrealistic
   battery status (always plugged-in, fixed level).
   *Source:* Flagged as a lower-confidence, less-independently-corroborated
   heuristic than #1–7 — include as a weak supplementary signal only, not
   a primary check. No single strong citation found; treat as
   community-common-knowledge pending a better source.
9. **Installed package heuristic** — Genymotion-specific packages present,
    or absence of packages a real consumer device would almost always have.
    *Source:* Ray Chong, "Android Emulator Detection" (Medium) — "Go
    Advanced" section on file/package-based detection beyond `Build.*`
    properties: https://ray-chong.medium.com/android-emulator-detection-4d0f994aab5e

## TAMPER / INTEGRITY (7)

1. **APK signature verification at runtime** — compare running app's
   signing certificate against an expected pinned hash.
   *Source:* OWASP MASTG resiliency-testing document, 0x05j:
   https://mas.owasp.org/MASTG/0x05j-Testing-Resiliency-Against-Reverse-Engineering/
2. **Package name / installer source check** — verify
   `getInstallerPackageName()` matches an expected source; weak alone
   (sideloading is common and legitimate in many markets).
   *Source:* General Android security guidance discussed in the
   anti-repackaging research paper below, in the context of what
   naive-but-common anti-repackaging apps check.
3. **CRC/checksum of critical DEX files** — walk every DEX file and detect
   modification post-build.
   *Source:* "You Shall not Repackage! Demystifying Anti-Repackaging on
   Android," arXiv:2009.04718: https://arxiv.org/pdf/2009.04718
4. **Native library integrity check** — checksum the `.so` file's on-disk
   contents vs an expected build-time value (static-patching detection,
   distinct from the runtime hook-checksum check in the HOOK category).
   *Source:* Same paper, arXiv:2009.04718, discusses native-layer
   integrity verification as part of anti-tampering schemes:
   https://arxiv.org/pdf/2009.04718
5. **Manifest tamper check** — verify `debuggable`/`allowBackup`/exported
   flags in the running `ApplicationInfo` match the build's declared
   manifest.
   *Source:* OWASP MASTG `MASTG-BEST-0007`, debuggable-flag manipulation as
   a repackaging vector: https://github.com/OWASP/mastg/blob/master/best-practices/MASTG-BEST-0007.md
6. **Resource/asset tamper check** — hash critical raw assets and compare
   at runtime.
   *Source:* arXiv:2009.04718, general anti-tampering integrity-check
   discussion: https://arxiv.org/pdf/2009.04718
7. **Self-checksum of the DroidShield engine itself** — the polymorphic
    engine/registry checksum their own presence/integrity so the SDK can't
    be cleanly stripped out without tripping a tamper signal.
    *Source:* Design inference from arXiv:2009.04718's broader point that
    anti-tampering schemes are strongest when checks protect each other,
    not just the host app: https://arxiv.org/pdf/2009.04718

---

## Sourcing notes

- **Root, debugger, hook**: primarily OWASP MASTG (Mobile Application
  Security Testing Guide) — the community-vetted reference for these
  categories, cited per-check by page ID above. A few hook-category checks
  also cite independent practitioner writeups (Talsec/freeRASP docs,
  Darvin's Blog, a real UnCrackable-app bypass writeup) where MASTG
  describes the mechanism generally but a named source shows the concrete
  implementation.
- **Emulator**: no single OWASP MASTG page covers this the way it does
  root/debugger/hook. Sourced instead from several long-standing,
  independently-converged open-source reference implementations
  (Strazzere's `anti-emulator`, CalebFenton's `AndroidEmulatorDetect`,
  `samohyes/Anti-vm-in-android`) plus one academic comparative study. The
  fact that the same `Build.*` property list appears near-identically
  across all of them (see the citations above) is itself evidence these
  are stable, converged community techniques — but note this is a
  different, weaker citation tier than the OWASP-sourced categories, and
  say so plainly in your README rather than implying uniform sourcing
  quality. Check #8 (battery heuristic) is flagged as the one weak link
  with no strong independent source — consider dropping it or marking it
  experimental if a reviewer pushes back.
- **Tamper/integrity**: primarily OWASP MASTG plus the anti-repackaging
  academic paper (arXiv:2009.04718), which is the most rigorous single
  source in this list and worth reading in full when maintaining the
  integrity checks.
- All checks are, by their own cited sources, publicly documented bypass
  targets. This is expected for RASP and is why DroidShield's differentiator
  is combining and shuffling many signals per build (the polymorphic
  engine), not any individual check's strength — make this explicit to
  users so nobody mistakes any single check for a strong guarantee.
