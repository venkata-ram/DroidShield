package dev.droidshield.gradleplugin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/** Consumer-facing configuration for build-time DroidShield hardening. */
abstract class DroidShieldExtension @Inject constructor(objects: ObjectFactory) {
    /** Instruments methods annotated with `dev.droidshield.sdk.DroidShieldGuarded`. */
    val instrumentGuardedMethods: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    /** Fails release builds that are debuggable or do not enable R8 and resource shrinking. */
    val enforceReleaseHardening: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
