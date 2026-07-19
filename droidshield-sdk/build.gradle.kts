// The public .aar. Only module most integrators ever import.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // No version here: kapt ships bundled with the Kotlin Gradle plugin
    // already on the classpath via kotlin-android above — declaring a
    // version via the catalog caused a "plugin already on classpath with
    // unknown version" conflict (verified in this environment).
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "dev.droidshield.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "DROIDSHIELD_VERSION", "\"${project.version}\"")
    }

    buildFeatures {
        buildConfig = true
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
    api(project(":droidshield-domain"))
    implementation(project(":droidshield-data-android"))
    implementation(project(":droidshield-native"))
    implementation(project(":droidshield-engine"))
    implementation(libs.dagger)
    kapt(libs.dagger.compiler)
    // api, not implementation: DroidShield.runChecksSuspending() is part
    // of the public surface (DECISIONS.md D030), so consumers calling it
    // from their own coroutine (launch { }, lifecycleScope, etc.) need
    // kotlinx-coroutines on their compile classpath too — api propagates
    // it transitively instead of every integrator having to redeclare it.
    api(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
