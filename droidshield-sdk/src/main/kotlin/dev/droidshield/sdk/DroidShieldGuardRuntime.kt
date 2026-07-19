package dev.droidshield.sdk

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/** Runtime bridge called by methods instrumented with [DroidShieldGuarded]. */
object DroidShieldGuardRuntime {
    private const val TAG = "DroidShieldGuard"

    private data class Registration(
        val droidShield: DroidShield,
        val minimumIntervalMillis: Long,
    )

    private val registration = AtomicReference<Registration?>()
    private val checkInFlight = AtomicBoolean(false)
    private val nextAllowedRunAtMillis = AtomicLong(0L)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "DroidShield-Guard").apply { isDaemon = true }
    }

    internal fun install(droidShield: DroidShield, minimumIntervalMillis: Long) {
        registration.set(Registration(droidShield, minimumIntervalMillis))
        nextAllowedRunAtMillis.set(0L)
    }

    /**
     * Called from injected bytecode. Calls made before [DroidShield.init] are
     * intentionally ignored; Android components can run before Application
     * initialization completes.
     */
    @JvmStatic
    fun onGuardedMethodEntered(operationId: String) {
        val current = registration.get() ?: return
        val now = SystemClock.elapsedRealtime()
        if (now < nextAllowedRunAtMillis.get()) return
        if (!checkInFlight.compareAndSet(false, true)) return

        nextAllowedRunAtMillis.set(now + current.minimumIntervalMillis)
        executor.execute {
            try {
                current.droidShield.runGuardedChecks(operationId)
            } catch (failure: Throwable) {
                Log.e(TAG, "Guarded check run failed for $operationId", failure)
            } finally {
                checkInFlight.set(false)
            }
        }
    }
}
