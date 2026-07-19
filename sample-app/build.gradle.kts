// Versions are hardcoded rather than pulled from the root version catalog:
// this build stands alone (see settings.gradle.kts), so it has no access to
// ../gradle/libs.versions.toml — and shouldn't, since an integrator copying
// this file wouldn't have it either.
plugins {
    id("com.android.application") version "8.7.2"
    id("org.jetbrains.kotlin.android") version "2.0.21"
    // The plugin ID is the JitPack coordinate, which is what lets this build
    // skip the resolutionStrategy workaround entirely (DECISIONS.md D033).
    // Literal version: the plugins block cannot reference a val.
    id("com.github.venkata-ram.DroidShield") version "0.3.2"
}

val droidShieldVersion = "0.3.2"

// DroidShield derives its release-specific ordering seed from Gradle's
// Project.version. Android's versionName is a separate value, so keep both
// backed by the same release version.
version = droidShieldVersion

android {
    namespace = "dev.droidshield.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.droidshield.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = droidShieldVersion
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // The JitPack coordinate, not project(":droidshield-sdk"). -sdk pulls in
    // -domain, -data-android, -native and -engine transitively.
    implementation("com.github.venkata-ram.DroidShield:droidshield-sdk:$droidShieldVersion")
}
