package dev.droidshield.sample

import android.app.Application
import android.util.Log
import dev.droidshield.generated.DroidShieldBuildSeed
import dev.droidshield.sdk.DroidShield
import dev.droidshield.sdk.DroidShieldConfig

/**
 * Demonstrates the full integration path: applying `dev.droidshield` in
 * `build.gradle.kts` generates [DroidShieldBuildSeed] at build time
 * (DECISIONS.md D026), which this app feeds into [DroidShieldConfig] so
 * the engine's check ordering varies build-to-build.
 */
class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val droidShield = DroidShield.init(
            context = this,
            config = DroidShieldConfig(polymorphicSeed = DroidShieldBuildSeed.SEED),
        )

        val results = droidShield.runChecks()
        Log.i("DroidShieldSample", "Ran ${results.size} checks, seed=${DroidShieldBuildSeed.SEED}")
        results.forEach { result ->
            Log.i("DroidShieldSample", "  ${result.checkId}: detected=${result.detected}")
        }
    }
}
