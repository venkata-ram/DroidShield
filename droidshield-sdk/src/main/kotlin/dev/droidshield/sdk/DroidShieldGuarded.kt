package dev.droidshield.sdk

/**
 * Marks an application method as a security-sensitive entry point.
 *
 * When the DroidShield Gradle plugin is applied, it injects a call at the
 * beginning of the method. The call schedules a full DroidShield check run on
 * a background executor; it never blocks the guarded method's caller.
 *
 * @param value stable operation identifier used for diagnostics. When empty,
 * the plugin derives one from the declaring class and JVM method signature.
 * Inline functions are not supported because their call sites are expanded
 * before Android bytecode instrumentation runs.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class DroidShieldGuarded(val value: String = "")
