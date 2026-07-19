plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("dev.droidshield")
}

android {
    namespace = "dev.droidshield.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.droidshield.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
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
    implementation(project(":droidshield-sdk"))
}
