// Build-time only: AGP Variant API + source codegen (see DECISIONS.md
// D026). No dependency on droidshield-engine, droidshield-data-android, or
// droidshield-native — it never needs to know about specific checks, and
// (as of D027) it's now a standalone included build, so a project
// dependency back into the main build isn't even resolvable.
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly("com.android.tools.build:gradle-api:8.7.2")
    // See DECISIONS.md D028: AGP's variant.sources.kotlin API doesn't
    // actually get consumed by compileDebugKotlin for a plain (non-KMP)
    // Android+Kotlin module in this AGP/Kotlin combo — verified directly.
    // The Kotlin Gradle Plugin's own source-set API is what's needed.
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

gradlePlugin {
    plugins {
        create("droidshield") {
            id = "dev.droidshield"
            implementationClass = "dev.droidshield.gradleplugin.DroidShieldPlugin"
        }
    }
}
