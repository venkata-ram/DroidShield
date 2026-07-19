// Pure Kotlin JVM module on purpose — see DECISIONS.md D022. The engine
// operates on Set<ThreatCheck> only and must never depend on Android APIs
// or on any specific check implementation.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":droidshield-domain"))

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
