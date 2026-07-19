package dev.droidshield.sdk

import android.content.Context
import android.os.Build
import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.ThreatReporter
import dev.droidshield.domain.TelemetrySink
import dev.droidshield.domain.backend.DeviceEvidence
import dev.droidshield.domain.backend.EvidenceContext
import dev.droidshield.domain.backend.toDeviceEvidence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The only class most integrators ever import (ARCHITECTURE.md §3). Not a
 * singleton object holding global mutable state on purpose — [init]
 * returns an instance so a host app can (in principle) run more than one
 * configured instance, e.g. for testing, though the common case is to keep
 * the single returned instance around for the app's lifetime.
 */
class DroidShield private constructor(
    private val component: DroidShieldComponent,
    private val androidContext: Context,
) {
    /**
     * Runs every registered check once and returns all results (not just
     * detected ones) on the calling thread. Individual checks do blocking
     * disk I/O (`/proc` reads, file existence checks, ZIP parsing) and one
     * does a socket connect attempt (DECISIONS.md D016) — calling this
     * directly from a lifecycle callback (e.g. `Application.onCreate`)
     * blocks the main thread. Use this only when the caller already
     * manages its own background thread (e.g. inside a `WorkManager`
     * `Worker.doWork()`). From a coroutine, prefer [runChecksSuspending].
     */
    fun runChecks(): List<CheckResult> {
        val engine = component.engine()
        return engine.runAll(AndroidCheckContext(androidContext))
    }

    /**
     * Coroutine-friendly entry point: runs [runChecks] on [Dispatchers.IO]
     * so it's safe to call from `Application.onCreate` (or any other
     * lifecycle callback) inside a `launch { }` without blocking the
     * calling thread. This is the recommended way to invoke DroidShield —
     * see DECISIONS.md D016/D030 for why the underlying checks stay
     * synchronous (not `suspend`) while the dispatch decision lives here,
     * at the facade, instead.
     */
    suspend fun runChecksSuspending(): List<CheckResult> = withContext(Dispatchers.IO) {
        runChecks()
    }

    /**
     * Runs all checks and returns a transport-neutral payload that can be used
     * directly as a Retrofit request body. DroidShield performs no networking;
     * the host app owns its endpoint, converter, authentication, and retry
     * policy.
     *
     * App and Android version fields are read by the SDK. Opaque installation,
     * session, and server-issued nonce values may be supplied through [context].
     */
    suspend fun collectEvidence(
        context: EvidenceContext = EvidenceContext(),
    ): DeviceEvidence = withContext(Dispatchers.IO) {
        val packageInfo = @Suppress("DEPRECATION")
        androidContext.packageManager.getPackageInfo(androidContext.packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        runChecks().toDeviceEvidence(
            sdkVersion = BuildConfig.DROIDSHIELD_VERSION,
            appPackageName = androidContext.packageName,
            appVersionName = packageInfo.versionName,
            appVersionCode = versionCode,
            androidSdk = Build.VERSION.SDK_INT,
            context = context,
        )
    }

    companion object {
        /**
         * @param reporter defaults to [LogcatThreatReporter] — see DECISIONS.md D011.
         * @param telemetrySink defaults to [NoOpTelemetrySink] — see DECISIONS.md D011.
         */
        fun init(
            context: Context,
            config: DroidShieldConfig = DroidShieldConfig(),
            reporter: ThreatReporter = LogcatThreatReporter(),
            telemetrySink: TelemetrySink = NoOpTelemetrySink(),
        ): DroidShield {
            val applicationContext = context.applicationContext
            val component = DaggerDroidShieldComponent.factory().create(
                applicationContext,
                config,
                ReportingModule(reporter),
                TelemetryModule(telemetrySink),
            )
            return DroidShield(component, applicationContext)
        }
    }
}
