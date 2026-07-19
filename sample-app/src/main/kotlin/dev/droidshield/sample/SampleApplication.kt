package dev.droidshield.sample

import android.app.Application
import android.util.Log
import dev.droidshield.generated.DroidShieldBuildSeed
import dev.droidshield.sdk.DroidShield
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Demonstrates the full integration path: applying `dev.droidshield` in
 * `build.gradle.kts` generates [DroidShieldBuildSeed] at build time
 * (DECISIONS.md D026). The SDK picks that seed up automatically
 * (DECISIONS.md D038), so this app does not wire it into the config — it
 * only reads it here to log the value in use.
 *
 * Checks run via [DroidShield.runChecksSuspending] inside [applicationScope]
 * rather than the blocking [DroidShield.runChecks] — several checks do
 * blocking disk I/O and one does a socket connect attempt (DECISIONS.md
 * D016), so calling the blocking variant directly from `onCreate()` would
 * stall app startup on the main thread. `SupervisorJob` means a failure in
 * this launch can't cascade into cancelling anything else sharing the
 * scope (there's nothing else here yet, but it's the correct default for
 * an application-lifetime scope over `Job()`).
 */
class SampleApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        val droidShield = DroidShield.init(context = this)

        applicationScope.launch {
            val results = droidShield.runChecksSuspending()
            Log.i("DroidShieldSample", "Ran ${results.size} checks, seed=${DroidShieldBuildSeed.SEED}")
            results.forEach { result ->
                Log.i("DroidShieldSample", "  ${result.checkId}: detected=${result.detected}")
            }
        }
    }
}
