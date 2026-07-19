# Consumer ProGuard/R8 rules for integrators of the droidshield .aar.
# See DECISIONS.md D014.

# EngineSelfPresenceCheck (TAMPER #10) looks this class up reflectively by
# its fully-qualified name and reports a tamper when the lookup fails.
# Without this rule R8 renames the class in any minified release build, the
# lookup misses, and the check fires a HIGH-severity false positive on every
# clean release — i.e. exactly the builds integrators actually ship.
-keep class dev.droidshield.engine.ThreatDetectionEngine { *; }

# NativeBridge's method names are the JNI symbols that jni_bridge.cpp
# exports (Java_dev_droidshield_nativelayer_NativeBridge_*). Renaming the
# class or its methods breaks the symbol lookup at load time, which takes
# out all four native-layer checks silently.
-keep class dev.droidshield.nativelayer.NativeBridge { *; }

# These are wire DTOs intended for Retrofit converters. Preserve their names
# and fields so reflection-based Gson/Moshi conversion emits the documented
# backend contract in minified release builds.
-keep class dev.droidshield.domain.backend.** { *; }
