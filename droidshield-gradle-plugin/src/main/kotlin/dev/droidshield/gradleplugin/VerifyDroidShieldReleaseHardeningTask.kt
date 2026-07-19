package dev.droidshield.gradleplugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/** Enforces the minimum release settings expected from a security-sensitive Android app. */
abstract class VerifyDroidShieldReleaseHardeningTask : DefaultTask() {
    @get:Input
    abstract val minifyEnabled: Property<Boolean>

    @get:Input
    abstract val shrinkResources: Property<Boolean>

    @get:Input
    abstract val debuggable: Property<Boolean>

    @get:Input
    abstract val enforcementEnabled: Property<Boolean>

    @TaskAction
    fun verify() {
        val failures = buildList {
            if (debuggable.get()) add("release is debuggable")
            if (!minifyEnabled.get()) add("R8 minification is disabled")
            if (!shrinkResources.get()) add("resource shrinking is disabled")
        }
        if (failures.isEmpty()) {
            logger.lifecycle("DroidShield release hardening verified: non-debuggable, R8 enabled, resources shrinking.")
            return
        }

        val message = "DroidShield release hardening failed: ${failures.joinToString()}. " +
            "Fix the release build type or set droidShield.enforceReleaseHardening=false to downgrade this to a warning."
        if (enforcementEnabled.get()) throw GradleException(message)
        logger.warn(message)
    }
}
