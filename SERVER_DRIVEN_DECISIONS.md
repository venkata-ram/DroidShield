# Server-Driven Decisions with DroidShield

← Back to [README.md](README.md)

This guide explains how to take DroidShield's on-device results and let **your server** decide what happens next — instead of hardcoding "if rooted, block" into your app.

DroidShield itself ships no backend. Everything below is a design you build on your side; DroidShield just gives you the raw signal.

---

## 1. Why not just decide on the device?

The obvious approach looks like this:

```kotlin
if (results.any { it.detected && it.severity == Severity.CRITICAL }) {
    finishAffinity()   // kick the user out
}
```

It works, and it's fine as a starting point. But it has three real problems.

**Problem 1: The attacker owns the `if`.** That comparison runs on a device they fully control. They can patch the bytecode so it always evaluates to `false`, or hook `runChecks()` to return an empty list. Any decision made on the device can be undone on the device.

**Problem 2: You can't change your mind.** You decide today that root = block. Next month you learn 8% of your legitimate users are rooted developers, and you're losing them. Changing that rule means shipping a new APK and waiting weeks for adoption.

**Problem 3: You only see one device at a time.** The client can't tell that this same device fingerprint just failed checks on 400 other accounts. That pattern is only visible from the server.

Server-driven decisions fix all three: the decision moves somewhere the attacker can't patch, you can tune the rules in minutes, and you can correlate across your whole user base.

---

## 2. The core idea in one sentence

> The device **reports what it observed**. The server **decides what it means**.

The client's only job is to be an honest witness. It gathers evidence and sends it. It does not judge. The server holds the policy and returns a verdict the app obeys.

```
┌──────────────────────┐                    ┌──────────────────────┐
│   Android app        │                    │   Your server        │
│                      │                    │                      │
│  DroidShield         │   1. evidence      │   Policy engine      │
│  runs 41 checks  ────┼───────────────────>│   (rules you own)    │
│                      │                    │          │           │
│                      │   2. verdict       │          ▼           │
│  Obeys the verdict <─┼────────────────────┼──  ALLOW / STEP_UP   │
│                      │                    │    / LIMIT / BLOCK   │
└──────────────────────┘                    └──────────────────────┘
```

Important: this is **not** a security boundary on its own. A tampered client can lie about the evidence or ignore the verdict. What it does give you is *centralized, changeable policy* — and, combined with the fact that the verdict also gates what your server is willing to do, real leverage. More on that in §7.

---

## 3. Step one — collect the evidence

DroidShield already returns everything you need. Don't send the raw `CheckResult` list, though — shape it into a small, stable payload.

```kotlin
val results = droidShield.runChecksSuspending()

val evidence = DeviceEvidence(
    // Only the checks that fired. A clean device sends an empty list.
    triggered = results.filter { it.detected }.map {
        TriggeredCheck(
            checkId = it.checkId,          // "su_binary_path"
            category = it.category.name,   // "ROOT"
            severity = it.severity.name    // "HIGH"
        )
    },
    // Context the server needs to interpret the above.
    checksRun = results.size,              // did all 41 run, or only a subset?
    appVersion = BuildConfig.VERSION_NAME,
    sdkVersion = Build.VERSION.SDK_INT,
    collectedAtMillis = System.currentTimeMillis()
)
```

**Send `checksRun` too — it matters more than it looks.** Because polymorphic builds can run a *subset* of checks, a report saying "0 threats out of 12 checks" is weaker evidence than "0 threats out of 41." And a report claiming 41 checks ran with 0 detections in 3 milliseconds is suspicious in itself.

**What not to send:** `detail` strings can contain file paths and process names. Keep them out of the default payload unless you've reviewed each one for PII and decided you need them for investigation.

---

## 4. Step two — get it to the server

Two patterns, depending on how urgent the signal is.

### Pattern A: Attach evidence to requests that matter

Simplest and usually best. Cache the last result and attach it as a header on sensitive calls — login, payment, withdrawal.

```kotlin
// Refreshed on app start and on resume; read by an OkHttp Interceptor.
object ThreatState {
    @Volatile var latest: DeviceEvidence? = null
}

class EvidenceInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val evidence = ThreatState.latest ?: return chain.proceed(chain.request())
        return chain.proceed(
            chain.request().newBuilder()
                .header("X-Device-Evidence", evidence.toCompactJson())
                .build()
        )
    }
}
```

The server now has the device's posture attached to the exact request it's authorizing. No extra round trip.

### Pattern B: Push threats as they happen

Use `ThreatReporter` when you want to know immediately, not at the next API call.

```kotlin
class ServerThreatReporter(private val api: ThreatApi) : ThreatReporter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onThreatDetected(result: CheckResult) {
        scope.launch {
            runCatching { api.report(result.checkId, result.category.name, result.severity.name) }
            // Swallow failures. Never let threat reporting break the app —
            // that's a denial-of-service switch you're handing the attacker.
        }
    }
}

DroidShield.init(context, reporter = ServerThreatReporter(api))
```

Note that `onThreatDetected` fires **once per detected check**, so a heavily compromised device can fire a dozen times in one run. Batch or debounce before you put this on your ingest endpoint.

Most teams want both: Pattern B for visibility, Pattern A for enforcement.

---

## 5. Step three — the server decides

This is where the actual design lives. Keep it boring and readable.

### Score, then threshold

Turn evidence into a number, compare the number to thresholds you can tune.

```
weight(severity):   LOW = 1    MEDIUM = 5    HIGH = 15    CRITICAL = 40

score = sum of weights of all triggered checks
        + 20 if two or more distinct categories fired
```

That last bonus matters. One root signal might be a false positive. Root **and** hook **and** tamper firing together is not a coincidence — corroboration across categories is much stronger evidence than any single check, so make your scoring reflect that.

### Map score to a verdict

```
score  0        → ALLOW      normal operation
score  1–14     → MONITOR    allow, but log and watch
score 15–39     → STEP_UP    allow after extra auth (OTP, biometric, re-login)
score 40–79     → LIMIT      read-only; block payments, withdrawals, key rotation
score 80+       → BLOCK      refuse the session
```

**Return the verdict, not the reasoning.** If your response says `"blocked because su_binary_path fired"`, you've just handed the attacker a free debugger for their bypass. Send a verdict and an opaque reference id; keep the reasoning in your logs.

```json
{ "verdict": "LIMIT", "ref": "a7f3c9e1" }
```

### Make the rules data, not code

Put the weights and thresholds in a config table or feature flag, not in a compiled constant. The whole point of moving the decision server-side is being able to change it without a deploy — let alone an app release.

### Graded responses beat binary ones

`BLOCK` is the least useful verdict. It tells the attacker exactly which change tripped you, so they iterate until it stops. `LIMIT` and `STEP_UP` are better: the account stays usable for a genuine user on a rooted phone, while the specific dangerous action is off the table, and an attacker gets much weaker feedback about what you noticed.

---

## 6. Step four — the client obeys

```kotlin
when (response.verdict) {
    "ALLOW", "MONITOR" -> proceed()
    "STEP_UP"          -> requireBiometric { proceed() }
    "LIMIT"            -> enterReadOnlyMode()
    "BLOCK"            -> showGenericError(response.ref)
}
```

Two rules:

**Fail closed on sensitive actions, fail open elsewhere.** If the verdict call times out, let the user read their transaction history; don't let them wire money. Never let a flaky network turn into a total outage — and never let it turn into a free bypass either.

**Show the user something honest and generic.** "We can't complete this on this device right now. Reference: a7f3c9e1." Not "root detected." Support can look up the reference; the attacker learns nothing.

---

## 7. The part that makes this actually worth doing

Everything above still runs through a client the attacker controls. So why does it help?

**Because the server enforces the verdict on its own side too.** The real protection isn't the app entering read-only mode — that's just UX. It's your server refusing to process the withdrawal when the session is flagged. The client-side response is a courtesy to honest users; the server-side refusal is the control. If a verdict only changes what the app draws on screen, you've built nothing.

**Because lying leaves a trace.** Reports are a stream, not a snapshot. A device that reported 41 checks with 6 detections yesterday and 41 checks with 0 detections today, from the same install id, just told you something. So did the client that always reports perfectly clean but has impossible timing, or the thousand accounts reporting byte-identical evidence.

**Because polymorphic builds keep the bypass expensive.** An attacker who hooks around the checks in v2.1 faces a different check ordering and subset in v2.2 (see [README.md](README.md) §Polymorphic builds). Their patch needs redoing every release, while your policy change takes a config edit.

**Because you can act at the account level, not the device level.** The device can refuse to send evidence. It cannot stop you from freezing the account, requiring re-verification, or reviewing the last 30 days of activity.

---

## 8. Rollout — don't turn on enforcement first

The single most common way this goes wrong is shipping enforcement on day one and blocking thousands of real users over a check you hadn't validated in the wild.

1. **Observe (2–4 weeks).** Report evidence, enforce nothing. Everything returns `ALLOW`.
2. **Measure.** What percentage of your users trip each check? If `InstallerSourceCheck` fires for 30% of users, your `expectedInstallerPackage` is wrong for the markets you're in — not 30% of your users are attackers.
3. **Tune.** Set weights from what you actually observed. Silence checks that are noisy for your user base.
4. **Enforce softly.** `STEP_UP` only, on your most sensitive action only. Watch the support queue.
5. **Tighten gradually.** Add `LIMIT`, then `BLOCK`, one action at a time.

Ship it as a killable feature flag. You want to be able to turn enforcement off in one minute at 3am without an app release.

---

## 9. Quick reference

| Question | Answer |
|---|---|
| Where does the decision live? | Server. Always. |
| What does the client send? | Which checks fired, their category and severity, and how many ran. |
| What does the server return? | A verdict and an opaque ref. Never the reasoning. |
| Where are the rules stored? | Config/flags — changeable without a deploy. |
| What if the verdict call fails? | Fail closed on sensitive actions, open on everything else. |
| Is this tamper-proof? | No. It centralizes policy and raises cost. Keep validating server-side. |
| What actually stops the attack? | Your server refusing the operation — not the app's UI state. |

---

See also: [README.md](README.md) for setup and the check catalogue, [ARCHITECTURE.md](ARCHITECTURE.md) for the SDK's internal design, [DECISIONS.md](DECISIONS.md) for the rationale log.
